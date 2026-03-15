import * as pulumi from "@pulumi/pulumi";
import * as aws from "@pulumi/aws";
import * as command from "@pulumi/command";
import * as fs from "fs";
import { execFileSync } from "child_process";

const playLogback = fs.readFileSync("../docker/play/config/logback-play.xml", "utf8");
const tlsSecurity = fs.readFileSync("../docker/play/config/enableLegacyTLS.security", "utf8");
const composeTemplate = fs.readFileSync("../docker/docker-compose.yml", "utf8");

const config = new pulumi.Config("zenobase");
const awsConfig = new pulumi.Config("aws");
const region = awsConfig.require("region");
const accountId = aws.getCallerIdentity().then(id => id.accountId);
const certificateArn = config.require("certificateArn");
const adminCidr = process.env.ENABLE_SSH
    ? `${execFileSync("curl", ["-s", "ifconfig.me"], { encoding: "utf8" }).trim()}/32`
    : undefined;
const keyPairName = config.require("keyPairName");
const instanceType = config.get("instanceType") || "t4g.large";
const playImageTag = config.get("playImageTag") || "latest";
const activeTargetGroup = config.get("activeTargetGroup") || "blue";
const deployTarget = config.get("deployTarget") || activeTargetGroup;
const opensearchSnapshotBucket = config.get("opensearchSnapshotBucket") || "";
const opensearchDomain = config.get("opensearchDomain") || "zenobase";
const opensearchReplayHost = config.get("opensearchReplayHost") || "";
const opensearchRebuildHost = config.get("opensearchRebuildHost") || "";
const hostname = config.get("hostname") || "http://localhost:9000";
const apiHostname = config.get("apiHostname") || "http://localhost:9000";
const oauthHostname = config.get("oauthHostname") || "https://zenobase.com";
const sesIdentity = config.get("sesIdentity") || "";

// ---------- VPC ----------

const vpc = new aws.ec2.Vpc("zenobase-vpc", {
    cidrBlock: "10.0.0.0/16",
    enableDnsSupport: true,
    enableDnsHostnames: true,
    tags: { Name: "zenobase" },
});

const igw = new aws.ec2.InternetGateway("zenobase-igw", {
    vpcId: vpc.id,
    tags: { Name: "zenobase" },
});

const publicRouteTable = new aws.ec2.RouteTable("zenobase-rt", {
    vpcId: vpc.id,
    routes: [{ cidrBlock: "0.0.0.0/0", gatewayId: igw.id }],
    tags: { Name: "zenobase-public" },
});

const subnetA = new aws.ec2.Subnet("zenobase-subnet-a", {
    vpcId: vpc.id,
    cidrBlock: "10.0.1.0/24",
    availabilityZone: `${region}a`,
    mapPublicIpOnLaunch: true,
    tags: { Name: "zenobase-a" },
});

const subnetB = new aws.ec2.Subnet("zenobase-subnet-b", {
    vpcId: vpc.id,
    cidrBlock: "10.0.2.0/24",
    availabilityZone: `${region}b`,
    mapPublicIpOnLaunch: true,
    tags: { Name: "zenobase-b" },
});

new aws.ec2.RouteTableAssociation("zenobase-rta-a", {
    subnetId: subnetA.id,
    routeTableId: publicRouteTable.id,
});

new aws.ec2.RouteTableAssociation("zenobase-rta-b", {
    subnetId: subnetB.id,
    routeTableId: publicRouteTable.id,
});

// ---------- Security Groups ----------

const albSg = new aws.ec2.SecurityGroup("zenobase-alb-sg", {
    vpcId: vpc.id,
    description: "ALB - HTTPS from internet",
    ingress: [
        { protocol: "tcp", fromPort: 443, toPort: 443, cidrBlocks: ["0.0.0.0/0"] },
        { protocol: "tcp", fromPort: 80, toPort: 80, cidrBlocks: ["0.0.0.0/0"] },
    ],
    egress: [
        { protocol: "-1", fromPort: 0, toPort: 0, cidrBlocks: ["0.0.0.0/0"] },
    ],
    tags: { Name: "zenobase-alb" },
});

const ec2Sg = new aws.ec2.SecurityGroup("zenobase-ec2-sg", {
    vpcId: vpc.id,
    description: "EC2 - app traffic from ALB, SSH from admin",
    ingress: [
        { protocol: "tcp", fromPort: 9000, toPort: 9000, securityGroups: [albSg.id] },
        ...(adminCidr ? [{ protocol: "tcp", fromPort: 22, toPort: 22, cidrBlocks: [adminCidr] }] : []),
    ],
    egress: [
        { protocol: "-1", fromPort: 0, toPort: 0, cidrBlocks: ["0.0.0.0/0"] },
    ],
    tags: { Name: "zenobase-ec2" },
});

const osSg = new aws.ec2.SecurityGroup("zenobase-os-sg", {
    vpcId: vpc.id,
    description: "OpenSearch - HTTPS from EC2",
    ingress: [
        { protocol: "tcp", fromPort: 443, toPort: 443, securityGroups: [ec2Sg.id] },
    ],
    egress: [
        { protocol: "-1", fromPort: 0, toPort: 0, cidrBlocks: ["0.0.0.0/0"] },
    ],
    tags: { Name: "zenobase-opensearch" },
});

// ---------- ECR Repositories ----------

const playRepo = new aws.ecr.Repository("zenobase-play", {
    name: "zenobase-play",
    imageTagMutability: "MUTABLE",
});

// ---------- OpenSearch Service ----------

const osDomain = new aws.opensearch.Domain("zenobase-os", {
    domainName: opensearchDomain,
    engineVersion: "OpenSearch_3.1",
    clusterConfig: {
        instanceType: "t3.medium.search",
        instanceCount: 1,
    },
    ebsOptions: {
        ebsEnabled: true,
        volumeSize: 20,
        volumeType: "gp3",
    },
    vpcOptions: {
        subnetIds: [subnetA.id],
        securityGroupIds: [osSg.id],
    },
    domainEndpointOptions: {
        enforceHttps: true,
    },
    nodeToNodeEncryption: {
        enabled: true,
    },
    encryptAtRest: {
        enabled: true,
    },
    accessPolicies: pulumi.all([accountId]).apply(([account]) => JSON.stringify({
        Version: "2012-10-17",
        Statement: [{
            Effect: "Allow",
            Principal: { AWS: "*" },
            Action: "es:*",
            Resource: `arn:aws:es:${region}:${account}:domain/${opensearchDomain}/*`,
        }],
    })),
    tags: { Name: opensearchDomain },
});

// Snapshot IAM role for OpenSearch to access S3
const osSnapshotRole = new aws.iam.Role("zenobase-os-snapshot-role", {
    assumeRolePolicy: JSON.stringify({
        Version: "2012-10-17",
        Statement: [{
            Action: "sts:AssumeRole",
            Effect: "Allow",
            Principal: { Service: "es.amazonaws.com" },
        }],
    }),
    tags: { Name: "zenobase-os-snapshot" },
});

new aws.iam.RolePolicy("zenobase-os-snapshot-policy", {
    role: osSnapshotRole.id,
    policy: JSON.stringify({
        Version: "2012-10-17",
        Statement: [{
            Effect: "Allow",
            Action: ["s3:GetObject", "s3:PutObject", "s3:ListBucket", "s3:DeleteObject"],
            Resource: [
                "arn:aws:s3:::zeno-snapshots",
                "arn:aws:s3:::zeno-snapshots/*",
            ],
        }],
    }),
});

// ---------- IAM ----------

const instanceRole = new aws.iam.Role("zenobase-instance-role", {
    assumeRolePolicy: JSON.stringify({
        Version: "2012-10-17",
        Statement: [{
            Action: "sts:AssumeRole",
            Effect: "Allow",
            Principal: { Service: "ec2.amazonaws.com" },
        }],
    }),
    tags: { Name: "zenobase-instance" },
});

new aws.iam.RolePolicyAttachment("zenobase-ssm-policy", {
    role: instanceRole.name,
    policyArn: "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore",
});

const instancePolicy = new aws.iam.RolePolicy("zenobase-instance-policy", {
    role: instanceRole.id,
    policy: pulumi.all([playRepo.arn, accountId, osSnapshotRole.arn, osDomain.arn]).apply(([playArn, account, snapshotRoleArn, domainArn]) => JSON.stringify({
        Version: "2012-10-17",
        Statement: [
            {
                Effect: "Allow",
                Action: ["secretsmanager:GetSecretValue"],
                Resource: [`arn:aws:secretsmanager:${region}:${account}:secret:zenobase/*`],
            },
            {
                Effect: "Allow",
                Action: ["ses:SendEmail", "ses:SendRawEmail"],
                Resource: [`arn:aws:ses:${region}:${account}:identity/${sesIdentity}`],
            },
            {
                Effect: "Allow",
                Action: [
                    "logs:CreateLogGroup",
                    "logs:CreateLogStream",
                    "logs:PutLogEvents",
                    "logs:DescribeLogStreams",
                ],
                Resource: [`arn:aws:logs:${region}:${account}:log-group:/zenobase/*`],
            },
            {
                Effect: "Allow",
                Action: ["ecr:GetAuthorizationToken"],
                Resource: "*",
            },
            {
                Effect: "Allow",
                Action: [
                    "ecr:BatchCheckLayerAvailability",
                    "ecr:GetDownloadUrlForLayer",
                    "ecr:BatchGetImage",
                ],
                Resource: [playArn],
            },
            {
                Effect: "Allow",
                Action: ["iam:PassRole"],
                Resource: [snapshotRoleArn],
            },
            {
                Effect: "Allow",
                Action: ["es:ESHttpGet", "es:ESHttpPut", "es:ESHttpPost", "es:ESHttpDelete", "es:ESHttpHead"],
                Resource: [`${domainArn}/*`],
            },
        ],
    })),
});

const instanceProfile = new aws.iam.InstanceProfile("zenobase-instance-profile", {
    role: instanceRole.name,
});

// ---------- ALB ----------

const albLogsBucket = new aws.s3.Bucket("zenobase-lb-logs", {
    bucket: "zenobase-lb-logs",
    tags: { Name: "zenobase-lb-logs" },
});

new aws.s3.BucketLifecycleConfiguration("zenobase-lb-logs-lifecycle", {
    bucket: albLogsBucket.id,
    rules: [{
        id: "expire-logs",
        status: "Enabled",
        expiration: { days: 90 },
    }],
});

const elbAccount = aws.elb.getServiceAccount();

const albLogsBucketPolicy = new aws.s3.BucketPolicy("zenobase-lb-logs-policy", {
    bucket: albLogsBucket.id,
    policy: pulumi.all([albLogsBucket.arn, elbAccount.then(a => a.arn)]).apply(([bucketArn, elbArn]) =>
        JSON.stringify({
            Version: "2012-10-17",
            Statement: [{
                Effect: "Allow",
                Principal: { AWS: elbArn },
                Action: "s3:PutObject",
                Resource: `${bucketArn}/*`,
            }],
        }),
    ),
});

const alb = new aws.lb.LoadBalancer("zenobase-alb", {
    internal: false,
    loadBalancerType: "application",
    securityGroups: [albSg.id],
    subnets: [subnetA.id, subnetB.id],
    accessLogs: {
        bucket: albLogsBucket.bucket,
        enabled: true,
    },
    tags: { Name: "zenobase" },
}, { dependsOn: [albLogsBucketPolicy] });

const tgBlue = new aws.lb.TargetGroup("zenobase-tg-blue", {
    port: 9000,
    protocol: "HTTP",
    vpcId: vpc.id,
    targetType: "instance",
    healthCheck: {
        path: "/status",
        port: "9000",
        protocol: "HTTP",
        healthyThreshold: 2,
        unhealthyThreshold: 5,
        timeout: 10,
        interval: 30,
    },
    deregistrationDelay: 60,
    tags: { Name: "zenobase-blue" },
});

const tgGreen = new aws.lb.TargetGroup("zenobase-tg-green", {
    port: 9000,
    protocol: "HTTP",
    vpcId: vpc.id,
    targetType: "instance",
    healthCheck: {
        path: "/status",
        port: "9000",
        protocol: "HTTP",
        healthyThreshold: 2,
        unhealthyThreshold: 5,
        timeout: 10,
        interval: 30,
    },
    deregistrationDelay: 60,
    tags: { Name: "zenobase-green" },
});

const activeTg = activeTargetGroup === "blue" ? tgBlue : tgGreen;

const httpsListener = new aws.lb.Listener("zenobase-https", {
    loadBalancerArn: alb.arn,
    port: 443,
    protocol: "HTTPS",
    certificateArn: certificateArn,
    sslPolicy: "ELBSecurityPolicy-TLS13-1-2-2021-06",
    defaultActions: [{
        type: "forward",
        forward: {
            targetGroups: [
                { arn: tgBlue.arn, weight: activeTargetGroup === "blue" ? 100 : 0 },
                { arn: tgGreen.arn, weight: activeTargetGroup === "green" ? 100 : 0 },
            ],
        },
    }],
});

new aws.lb.Listener("zenobase-http-redirect", {
    loadBalancerArn: alb.arn,
    port: 80,
    protocol: "HTTP",
    defaultActions: [{
        type: "redirect",
        redirect: {
            port: "443",
            protocol: "HTTPS",
            statusCode: "HTTP_301",
        },
    }],
});

// ---------- Secrets Manager ----------

const prodConfSecret = new aws.secretsmanager.Secret("zenobase-prod-conf", {
    name: "zenobase/prod-conf",
    description: "Zenobase production configuration (prod.conf)",
});

// ---------- CloudWatch Log Groups ----------

new aws.cloudwatch.LogGroup("zenobase-play-logs", {
    name: "/zenobase/play",
    retentionInDays: 30,
});


// ---------- EC2 Instance ----------

// Find latest Amazon Linux 2023 AMI
const ami = aws.ec2.getAmi({
    mostRecent: true,
    owners: ["amazon"],
    filters: [
        { name: "name", values: ["al2023-ami-2023*-arm64"] },
        { name: "state", values: ["available"] },
    ],
});

const blueNeeded = deployTarget === "blue" || activeTargetGroup === "blue";
const greenNeeded = deployTarget === "green" || activeTargetGroup === "green";

const userData = pulumi.all([
    playRepo.repositoryUrl,
    prodConfSecret.arn,
    osDomain.endpoint,
    osSnapshotRole.arn,
]).apply(([playRepoUrl, secretArn, osEndpoint, snapshotRoleArn]) => {
    const ecrRegistry = playRepoUrl.split("/")[0];
    return `#!/bin/bash
set -euo pipefail
exec > /var/log/user-data.log 2>&1

# Install Docker
dnf install -y docker aws-cli jq
systemctl enable docker
systemctl start docker

# Install docker-compose
COMPOSE_VERSION=v2.24.5
curl -L "https://github.com/docker/compose/releases/download/\${COMPOSE_VERSION}/docker-compose-linux-aarch64" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# ECR login
aws ecr get-login-password --region ${region} | docker login --username AWS --password-stdin ${ecrRegistry}

# Retrieve prod.conf from Secrets Manager
mkdir -p /etc/play
aws secretsmanager get-secret-value --secret-id zenobase/prod-conf --region ${region} --query SecretString --output text > /etc/play/prod.conf

# Set up docker-compose directory
mkdir -p /opt/zenobase/play/config
cd /opt/zenobase

# Write config files
cat > play/config/logback-play.xml << 'LPEOF'
${playLogback}LPEOF

cat > play/config/enableLegacyTLS.security << 'TLSEOF'
${tlsSecurity}TLSEOF

# Write docker-compose.yml
cat > docker-compose.yml << 'DCEOF'
${composeTemplate}DCEOF

# Write .env for docker-compose variable substitution
cat > .env << ENVEOF
ECR_REGISTRY=${ecrRegistry}
PLAY_IMAGE_TAG=${playImageTag}
AWS_REGION=${region}
OPENSEARCH_HOST=https://${osEndpoint}
OPENSEARCH_SNAPSHOT_BUCKET=${opensearchSnapshotBucket}
OPENSEARCH_SNAPSHOT_ROLE_ARN=${snapshotRoleArn}
OPENSEARCH_REPLAY=${opensearchReplayHost}
OPENSEARCH_REBUILD=${opensearchRebuildHost}
HOSTNAME=${hostname}
API_HOSTNAME=${apiHostname}
OAUTH_HOSTNAME=${oauthHostname}
ENVEOF

# Pull images and start
docker-compose pull
docker-compose up -d
`;
});

const blueInstance = new aws.ec2.Instance("zenobase-blue", {
    ami: ami.then(a => a.id),
    instanceType,
    subnetId: subnetA.id,
    vpcSecurityGroupIds: [ec2Sg.id],
    iamInstanceProfile: instanceProfile.name,
    keyName: keyPairName,
    userData,
    userDataReplaceOnChange: true,
    rootBlockDevice: { volumeSize: 20, volumeType: "gp3", encrypted: true },
    metadataOptions: {
        httpTokens: "required",
        httpEndpoint: "enabled",
    },
    tags: { Name: "zenobase-blue", Service: "zenobase" },
}, {
    retainOnDelete: true,
    ...(deployTarget !== "blue" && { ignoreChanges: ["ami", "instanceType", "userData", "rootBlockDevice", "metadataOptions", "tags", "vpcSecurityGroupIds"] }),
});
new aws.lb.TargetGroupAttachment("zenobase-tg-blue-attach", {
    targetGroupArn: tgBlue.arn,
    targetId: blueInstance.id,
    port: 9000,
});

const greenInstance = new aws.ec2.Instance("zenobase-green", {
    ami: ami.then(a => a.id),
    instanceType,
    subnetId: subnetA.id,
    vpcSecurityGroupIds: [ec2Sg.id],
    iamInstanceProfile: instanceProfile.name,
    keyName: keyPairName,
    userData,
    userDataReplaceOnChange: true,
    rootBlockDevice: { volumeSize: 20, volumeType: "gp3", encrypted: true },
    metadataOptions: {
        httpTokens: "required",
        httpEndpoint: "enabled",
    },
    tags: { Name: "zenobase-green", Service: "zenobase" },
}, {
    retainOnDelete: true,
    aliases: [{ name: "zenobase-instance" }],
    ...(deployTarget !== "green" && { ignoreChanges: ["ami", "instanceType", "userData", "rootBlockDevice", "metadataOptions", "tags", "vpcSecurityGroupIds"] }),
});
new aws.lb.TargetGroupAttachment("zenobase-tg-green-attach", {
    targetGroupArn: tgGreen.arn,
    targetId: greenInstance.id,
    port: 9000,
}, {
    aliases: [{ name: "zenobase-tg-attachment" }],
});

// Stop/start instances based on whether they're needed
new command.local.Command("zenobase-blue-state", {
    create: blueNeeded
        ? pulumi.interpolate`aws ec2 start-instances --instance-ids ${blueInstance.id} --region ${region} && aws ec2 wait instance-running --instance-ids ${blueInstance.id} --region ${region}`
        : pulumi.interpolate`aws ec2 stop-instances --instance-ids ${blueInstance.id} --region ${region}`,
    triggers: [blueNeeded],
});

new command.local.Command("zenobase-green-state", {
    create: greenNeeded
        ? pulumi.interpolate`aws ec2 start-instances --instance-ids ${greenInstance.id} --region ${region} && aws ec2 wait instance-running --instance-ids ${greenInstance.id} --region ${region}`
        : pulumi.interpolate`aws ec2 stop-instances --instance-ids ${greenInstance.id} --region ${region}`,
    triggers: [greenNeeded],
});

// ---------- CloudWatch Alarms ----------

new aws.cloudwatch.MetricAlarm("zenobase-unhealthy-hosts", {
    alarmDescription: "ALB has unhealthy targets",
    namespace: "AWS/ApplicationELB",
    metricName: "UnHealthyHostCount",
    dimensions: {
        LoadBalancer: alb.arnSuffix,
        TargetGroup: activeTg.arnSuffix,
    },
    statistic: "Maximum",
    period: 60,
    evaluationPeriods: 3,
    threshold: 1,
    comparisonOperator: "GreaterThanOrEqualToThreshold",
    treatMissingData: "breaching",
});

new aws.cloudwatch.MetricAlarm("zenobase-5xx-errors", {
    alarmDescription: "ALB 5xx error rate is elevated",
    namespace: "AWS/ApplicationELB",
    metricName: "HTTPCode_Target_5XX_Count",
    dimensions: {
        LoadBalancer: alb.arnSuffix,
    },
    statistic: "Sum",
    period: 300,
    evaluationPeriods: 2,
    threshold: 10,
    comparisonOperator: "GreaterThanOrEqualToThreshold",
    treatMissingData: "notBreaching",
});

// ---------- Exports ----------

export const vpcId = vpc.id;
export const albDnsName = alb.dnsName;
export const albArn = alb.arn;
export const playEcrUrl = playRepo.repositoryUrl;
export const opensearchEndpoint = osDomain.endpoint;
const activeInstance = activeTargetGroup === "blue" ? blueInstance : greenInstance;
const deployInstance = deployTarget === "blue" ? blueInstance : greenInstance;

export const instanceId = activeInstance.id;
export const instancePublicIp = activeInstance.publicIp;
export const instancePrivateIp = activeInstance.privateIp;
export const deployInstanceId = deployInstance.id;
export const deployInstancePublicIp = deployInstance.publicIp;
export const deployInstancePrivateIp = deployInstance.privateIp;
export const blueTargetGroupArn = tgBlue.arn;
export const greenTargetGroupArn = tgGreen.arn;
export const activeTarget = activeTargetGroup;
export const deployTargetOutput = deployTarget;
