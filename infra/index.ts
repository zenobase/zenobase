import * as pulumi from "@pulumi/pulumi";
import * as aws from "@pulumi/aws";

const config = new pulumi.Config("zenobase");
const awsConfig = new pulumi.Config("aws");
const region = awsConfig.require("region");
const certificateArn = config.require("certificateArn");
const adminCidr = config.get("adminCidr");
const keyPairName = config.require("keyPairName");
const instanceType = config.get("instanceType") || "t3.large";
const playHeap = config.get("playHeap") || "2g";
const esHeap = config.get("esHeap") || "4g";
const playImageTag = config.get("playImageTag") || "latest";
const esImageTag = config.get("esImageTag") || "latest";
const activeTargetGroup = config.get("activeTargetGroup") || "blue";
const deployTarget = config.get("deployTarget") || activeTargetGroup;
const esCluster = config.get("esCluster") || "elasticsearch";
const esReplayCluster = config.get("esReplayCluster") || "";

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
        // ES transport port for cluster replication between old and new instances
        { protocol: "tcp", fromPort: 9300, toPort: 9300, cidrBlocks: ["10.0.0.0/16"] },
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
    forceDelete: true,
});

const esRepo = new aws.ecr.Repository("zenobase-elasticsearch", {
    name: "zenobase-elasticsearch",
    imageTagMutability: "MUTABLE",
    forceDelete: true,
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

new aws.iam.RolePolicyAttachment("zenobase-ecr-policy", {
    role: instanceRole.name,
    policyArn: "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly",
});

new aws.iam.RolePolicyAttachment("zenobase-ssm-policy", {
    role: instanceRole.name,
    policyArn: "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore",
});

const instancePolicy = new aws.iam.RolePolicy("zenobase-instance-policy", {
    role: instanceRole.id,
    policy: pulumi.all([playRepo.arn, esRepo.arn]).apply(([playArn, esArn]) => JSON.stringify({
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
                Resource: [`arn:aws:secretsmanager:${region}:*:secret:zenobase/*`],
            },
            {
                Effect: "Allow",
                Action: ["ses:SendEmail", "ses:SendRawEmail"],
                Resource: "*",
            },
            {
                Effect: "Allow",
                Action: [
                    "logs:CreateLogGroup",
                    "logs:CreateLogStream",
                    "logs:PutLogEvents",
                    "logs:DescribeLogStreams",
                ],
                Resource: "arn:aws:logs:*:*:*",
            },
            {
                Effect: "Allow",
                Action: [
                    "ecr:GetAuthorizationToken",
                    "ecr:BatchCheckLayerAvailability",
                    "ecr:GetDownloadUrlForLayer",
                    "ecr:BatchGetImage",
                ],
                Resource: "*",
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

const alb = new aws.lb.LoadBalancer("zenobase-alb", {
    internal: false,
    loadBalancerType: "application",
    securityGroups: [albSg.id],
    subnets: [subnetA.id, subnetB.id],
    tags: { Name: "zenobase" },
});

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
        targetGroupArn: activeTg.arn,
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

const deployTg = deployTarget === "blue" ? tgBlue : tgGreen;

const userData = pulumi.all([
    playRepo.repositoryUrl,
    esRepo.repositoryUrl,
    prodConfSecret.arn,
]).apply(([playRepoUrl, esRepoUrl, secretArn]) => {
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
aws ecr get-login-password --region ${region} | docker login --username AWS --password-stdin ${playRepoUrl.split("/")[0]}

# Retrieve prod.conf from Secrets Manager
mkdir -p /etc/play
aws secretsmanager get-secret-value --secret-id zenobase/prod-conf --region ${region} --query SecretString --output text > /etc/play/prod.conf

# Set up docker-compose directory
mkdir -p /opt/zenobase/elasticsearch/config /opt/zenobase/play/config
cd /opt/zenobase

# Write config files
cat > elasticsearch/config/elasticsearch.yml << 'ESEOF'
cluster.name: \${ES_CLUSTER_NAME}

indices.store.throttle.type: none
indices.breaker.fielddata.limit: 80%
indices.breaker.request.limit: 80%
indices.breaker.total.limit: 80%

plugin.mandatory: cloud-aws,geocluster-facet,decimal-histogram-facet

cloud:
    aws:
        access_key: \${AWS_ACCESS_KEY}
        secret_key: \${AWS_SECRET_KEY}
        region: \${AWS_REGION}

discovery:
    type: \${ES_DISCOVERY_TYPE:zen}
    ec2:
        tag:
            Service: zenobase
    zen:
        ping:
            unicast:
                hosts: ""
            multicast:
                enabled: false
ESEOF

cat > play/config/logback-play.xml << 'LPEOF'
<configuration>
  <contextName>play</contextName>
  <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <encoder><pattern>%date [%level] %logger - %message%n%xException</pattern></encoder>
  </appender>
  <logger name="play" level="INFO" />
  <logger name="application" level="INFO" />
  <logger name="com.zenobase" level="INFO"/>
  <root level="WARN"><appender-ref ref="STDOUT"/></root>
</configuration>
LPEOF

cat > elasticsearch/config/logback-elasticsearch.xml << 'LEEOF'
<configuration>
  <contextName>elasticsearch</contextName>
  <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <encoder><pattern>%date [%level] %logger - %message%n%xException</pattern></encoder>
  </appender>
  <logger name="com.amazonaws" level="WARN" />
  <root level="INFO"><appender-ref ref="STDOUT"/></root>
</configuration>
LEEOF

cat > play/config/enableLegacyTLS.security << 'TLSEOF'
jdk.tls.disabledAlgorithms=SSLv3, RC4, DES, MD5withRSA, \\
    DH keySize < 1024, EC keySize < 224, 3DES_EDE_CBC, anon, NULL, \\
    include jdk.disabled.namedCurves
TLSEOF

# Write docker-compose.yml
cat > docker-compose.yml << DCEOF
services:
  elasticsearch:
    image: ${esRepoUrl}:${esImageTag}
    ports:
      - "9200:9200"
      - "9300:9300"
    volumes:
      - es-data:/var/lib/elasticsearch
      - ./elasticsearch/config/elasticsearch.yml:/etc/elasticsearch/elasticsearch.yml:ro
      - ./elasticsearch/config/logback-elasticsearch.xml:/etc/elasticsearch/logback.xml:ro
    environment:
      - ES_HEAP_SIZE=${esHeap}
      - ES_CLUSTER_NAME=${esCluster}
      - ES_DISCOVERY_TYPE=ec2
      - AWS_ACCESS_KEY=\\\${AWS_ACCESS_KEY}
      - AWS_SECRET_KEY=\\\${AWS_SECRET_KEY}
      - AWS_REGION=${region}
    ulimits:
      nofile:
        soft: 64000
        hard: 64000
      memlock:
        soft: -1
        hard: -1
    restart: unless-stopped
    logging:
      driver: awslogs
      options:
        awslogs-region: ${region}
        awslogs-group: /zenobase/elasticsearch
        awslogs-stream: es

  play:
    image: ${playRepoUrl}:${playImageTag}
    ports:
      - "9000:9000"
    volumes:
      - /etc/play/prod.conf:/etc/play/prod.conf:ro
      - ./play/config/logback-play.xml:/etc/play/logback.xml:ro
      - ./play/config/enableLegacyTLS.security:/etc/play/enableLegacyTLS.security:ro
    environment:
      - JAVA_HEAP=${playHeap}
      - ES_CLUSTER=${esCluster}
      - ES_REPLAY=${esReplayCluster}
    depends_on:
      elasticsearch:
        condition: service_healthy
    restart: unless-stopped
    logging:
      driver: awslogs
      options:
        awslogs-region: ${region}
        awslogs-group: /zenobase/play
        awslogs-stream: play

volumes:
  es-data:
    driver: local
DCEOF

# Extract AWS credentials from prod.conf for ES
AWS_AK=\$(grep 'aws.access_key=' /etc/play/prod.conf | cut -d'"' -f2)
AWS_SK=\$(grep 'aws.secret_key=' /etc/play/prod.conf | cut -d'"' -f2)

# Write .env for docker-compose variable substitution
cat > .env << ENVEOF
ES_CLUSTER_NAME=${esCluster}
ES_DISCOVERY_TYPE=ec2
AWS_ACCESS_KEY=\${AWS_AK}
AWS_SECRET_KEY=\${AWS_SK}
AWS_REGION=${region}
ENVEOF

# Pull images and start
docker-compose pull
docker-compose up -d
`;
});

const instance = new aws.ec2.Instance("zenobase-instance", {
    ami: ami.then(a => a.id),
    instanceType: instanceType,
    subnetId: subnetA.id,
    vpcSecurityGroupIds: [ec2Sg.id],
    iamInstanceProfile: instanceProfile.name,
    keyName: keyPairName,
    userData: userData,
    userDataReplaceOnChange: true,
    rootBlockDevice: {
        volumeSize: 20,
        volumeType: "gp3",
    },
    tags: {
        Name: `zenobase-${deployTarget}`,
        Service: "zenobase",
    },
});

new aws.lb.TargetGroupAttachment("zenobase-tg-attachment", {
    targetGroupArn: deployTg.arn,
    targetId: instance.id,
    port: 9000,
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
export const instanceId = instance.id;
export const instancePublicIp = instance.publicIp;
export const instancePrivateIp = instance.privateIp;
export const blueTargetGroupArn = tgBlue.arn;
export const greenTargetGroupArn = tgGreen.arn;
export const activeTarget = activeTargetGroup;
export const deployTargetGroup = deployTarget;
export const deployTargetGroupArn = deployTg.arn;
