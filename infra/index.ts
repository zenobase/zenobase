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
const playHeap = config.get("playHeap") || "2g";
const esHeap = config.get("esHeap") || "4g";
const playImageTag = config.get("playImageTag") || "latest";
const esImageTag = config.get("esImageTag") || "latest";
const activeTargetGroup = config.get("activeTargetGroup") || "blue";
const deployTarget = config.get("deployTarget") || activeTargetGroup;
const esSnapshotBucket = config.get("esSnapshotBucket") || "";
const esCluster = config.get("esCluster") || "elasticsearch";
const esReplayHost = config.get("esReplayHost") || "";
const esRebuildHost = config.get("esRebuildHost") || "";
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
    description: "EC2 - app traffic from ALB, SSH from admin, ES transport between instances",
    ingress: [
        { protocol: "tcp", fromPort: 9000, toPort: 9000, securityGroups: [albSg.id] },
        // ES REST API port for migration between old and new instances
        { protocol: "tcp", fromPort: 9200, toPort: 9200, cidrBlocks: ["10.0.0.0/16"] },
        // ES transport port for cluster replication between old and new instances
        { protocol: "tcp", fromPort: 9300, toPort: 9302, cidrBlocks: ["10.0.0.0/16"] },
        ...(adminCidr ? [{ protocol: "tcp", fromPort: 22, toPort: 22, cidrBlocks: [adminCidr] }] : []),
    ],
    egress: [
        { protocol: "-1", fromPort: 0, toPort: 0, cidrBlocks: ["0.0.0.0/0"] },
    ],
    tags: { Name: "zenobase-ec2" },
});

// ---------- ECR Repositories ----------

const playRepo = new aws.ecr.Repository("zenobase-play", {
    name: "zenobase-play",
    imageTagMutability: "MUTABLE",
});

const esRepo = new aws.ecr.Repository("zenobase-opensearch", {
    name: "zenobase-elasticsearch",
    imageTagMutability: "MUTABLE",
}, {
    aliases: [{ name: "zenobase-elasticsearch" }],
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
    policy: pulumi.all([playRepo.arn, esRepo.arn, accountId]).apply(([playArn, esArn, account]) => JSON.stringify({
        Version: "2012-10-17",
        Statement: [
            {
                Effect: "Allow",
                Action: ["s3:GetObject", "s3:PutObject", "s3:ListBucket", "s3:DeleteObject"],
                Resource: [
                    "arn:aws:s3:::zeno-snapshots",
                    "arn:aws:s3:::zeno-snapshots/*",
                ],
            },
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
                Resource: [playArn, esArn],
            },
            {
                Effect: "Allow",
                Action: ["ec2:DescribeInstances"],
                Resource: "*",
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

new aws.lb.ListenerRule("zenobase-blue-host-rule", {
    listenerArn: httpsListener.arn,
    priority: 10,
    conditions: [{ hostHeader: { values: ["blue.zenobase.com"] } }],
    actions: [{ type: "forward", targetGroupArn: tgBlue.arn }],
});

new aws.lb.ListenerRule("zenobase-green-host-rule", {
    listenerArn: httpsListener.arn,
    priority: 11,
    conditions: [{ hostHeader: { values: ["green.zenobase.com"] } }],
    actions: [{ type: "forward", targetGroupArn: tgGreen.arn }],
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

new aws.cloudwatch.LogGroup("zenobase-es-logs", {
    name: "/zenobase/elasticsearch",
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
    esRepo.repositoryUrl,
    prodConfSecret.arn,
]).apply(([playRepoUrl, esRepoUrl, secretArn]) => {
    const ecrRegistry = playRepoUrl.split("/")[0];
    return `#!/bin/bash
set -euo pipefail
exec > /var/log/user-data.log 2>&1

# OpenSearch requires vm.max_map_count >= 262144
sysctl -w vm.max_map_count=262144
echo "vm.max_map_count=262144" >> /etc/sysctl.conf

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

# Fetch EC2 metadata via IMDSv2
TOKEN=\$(curl -s -X PUT "http://169.254.169.254/latest/api/token" -H "X-aws-ec2-metadata-token-ttl-seconds: 21600")
HOST_IP=\$(curl -s -H "X-aws-ec2-metadata-token: \$TOKEN" http://169.254.169.254/latest/meta-data/local-ipv4)
INSTANCE_ID=\$(curl -s -H "X-aws-ec2-metadata-token: \$TOKEN" http://169.254.169.254/latest/meta-data/instance-id)

# Check if any peer OpenSearch node with the same cluster name is already running
PEER_COUNT=\$(aws ec2 describe-instances \
  --region ${region} \
  --filters "Name=tag:Service,Values=zenobase" "Name=tag:ClusterName,Values=${esCluster}" "Name=instance-state-name,Values=running" \
  --query "Reservations[].Instances[?InstanceId!=\\\`\$INSTANCE_ID\\\`].InstanceId" \
  --output text | wc -w)

# If no peer exists, write override to bootstrap this node as initial cluster manager
if [ "\$PEER_COUNT" -eq 0 ]; then
  cat > docker-compose.override.yml << 'OVEOF'
services:
  opensearch:
    environment:
      - cluster.initial_cluster_manager_nodes=\${NODE_NAME}
OVEOF
fi

# Write .env for docker-compose variable substitution
cat > .env << ENVEOF
ECR_REGISTRY=${ecrRegistry}
PLAY_IMAGE_TAG=${playImageTag}
OS_IMAGE_TAG=${esImageTag}
OS_HEAP_SIZE=${esHeap}
CLUSTER_NAME=${esCluster}
AWS_REGION=${region}
JAVA_HEAP=${playHeap}
ES_REPLAY=${esReplayHost}
ES_REBUILD=${esRebuildHost}
HOSTNAME=${hostname}
API_HOSTNAME=${apiHostname}
OAUTH_HOSTNAME=${oauthHostname}
ES_SNAPSHOT_BUCKET=${esSnapshotBucket}
PUBLISH_HOST=\${HOST_IP}
NODE_NAME=\${INSTANCE_ID}
ENVEOF

# Write opensearch deregister script (runs on shutdown)
cat > /opt/zenobase/opensearch-deregister.sh << 'DEREGEOF'
#!/bin/bash
set -e

# Get this node's instance ID (used as node name)
TOKEN=$(curl -s -X PUT "http://169.254.169.254/latest/api/token" -H "X-aws-ec2-metadata-token-ttl-seconds: 21600")
NODE_NAME=$(curl -s -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/instance-id)

# Check if OpenSearch is reachable
if ! curl -sf -o /dev/null http://localhost:9200; then
  echo "OpenSearch not reachable, skipping deregistration"
  exit 0
fi

# Check if cluster has more than one node
NODE_COUNT=$(curl -sf http://localhost:9200/_cat/nodes?h=name | wc -l)
if [ "$NODE_COUNT" -le 1 ]; then
  echo "Single-node cluster, skipping voting exclusion"
  exit 0
fi

echo "Excluding node $NODE_NAME from voting configuration..."
curl -sf -X POST "http://localhost:9200/_cluster/voting_config_exclusions?node_names=\${NODE_NAME}&timeout=30s" || true
echo "Node $NODE_NAME excluded from voting configuration"
DEREGEOF
chmod +x /opt/zenobase/opensearch-deregister.sh

# Write opensearch clear-exclusions script (runs on boot)
cat > /opt/zenobase/opensearch-clear-exclusions.sh << 'CLEAREOF'
#!/bin/bash
set -e

echo "Waiting for OpenSearch to become healthy..."
for i in $(seq 1 60); do
  if curl -sf -o /dev/null http://localhost:9200/_cluster/health; then
    echo "OpenSearch is healthy, clearing voting config exclusions..."
    curl -sf -X DELETE "http://localhost:9200/_cluster/voting_config_exclusions" || true
    echo "Voting config exclusions cleared"
    exit 0
  fi
  sleep 5
done
echo "OpenSearch did not become healthy within 5 minutes, skipping exclusion cleanup"
CLEAREOF
chmod +x /opt/zenobase/opensearch-clear-exclusions.sh

# Write systemd service for opensearch deregistration
cat > /etc/systemd/system/opensearch-deregister.service << 'SVCEOF'
[Unit]
Description=OpenSearch voting config deregistration
After=docker.service
Before=shutdown.target

[Service]
Type=oneshot
RemainAfterExit=yes
ExecStart=/opt/zenobase/opensearch-clear-exclusions.sh
ExecStop=/opt/zenobase/opensearch-deregister.sh
TimeoutStartSec=360
TimeoutStopSec=45

[Install]
WantedBy=multi-user.target
SVCEOF

systemctl daemon-reload
systemctl enable opensearch-deregister.service
systemctl start opensearch-deregister.service

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
        httpTokens: "optional",    // allow IMDSv1 (needed by old AWS SDK in ES)
        httpEndpoint: "enabled",
    },
    tags: { Name: "zenobase-blue", Service: "zenobase", ClusterName: esCluster },
}, {
    retainOnDelete: true,
    ...(deployTarget !== "blue" && { ignoreChanges: ["ami", "instanceType", "userData", "rootBlockDevice", "metadataOptions", "tags"] }),
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
        httpTokens: "optional",    // allow IMDSv1 (needed by old AWS SDK in ES)
        httpEndpoint: "enabled",
    },
    tags: { Name: "zenobase-green", Service: "zenobase", ClusterName: esCluster },
}, {
    retainOnDelete: true,
    aliases: [{ name: "zenobase-instance" }],
    ...(deployTarget !== "green" && { ignoreChanges: ["ami", "instanceType", "userData", "rootBlockDevice", "metadataOptions", "tags"] }),
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
export const esEcrUrl = esRepo.repositoryUrl;
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
