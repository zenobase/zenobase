import * as pulumi from "@pulumi/pulumi";
import * as aws from "@pulumi/aws";

const config = new pulumi.Config("zenobase");
const awsConfig = new pulumi.Config("aws");
const region = awsConfig.require("region");
const accountId = aws.getCallerIdentity().then(id => id.accountId);
const certificateArn = config.require("certificateArn");
const imageTag = config.require("imageTag");
const opensearchSnapshotBucket = config.get("opensearchSnapshotBucket") || "";
const opensearchDomain = config.get("opensearchDomain") || "zenobase";
const opensearchReplayDomain = config.get("opensearchReplayDomain") || "";
const opensearchRebuildDomain = config.get("opensearchRebuildDomain") || "";
const opensearchVersion = config.get("opensearchVersion") || "OpenSearch_3.3";
const hostname = config.get("hostname") || "http://localhost:9000";
const apiHostname = config.get("apiHostname") || "http://localhost:9000";
const oauthHostname = config.get("oauthHostname") || "https://zenobase.com";
const sesIdentity = config.get("sesIdentity") || "";
const sentryDsn = config.get("sentryDsn") || "";
const bastionEnabled = config.get("bastionEnabled") === "true";
const fargateCpu = config.get("fargateCpu") || "1024";
const fargateMemory = config.get("fargateMemory") || "2048";
const opensearchInstanceType = config.get("opensearchInstanceType") || "t3.medium.search";

// ---------- GitHub Actions IAM Role ----------

const ghOidcArn = pulumi.all([accountId]).apply(([account]) =>
    `arn:aws:iam::${account}:oidc-provider/token.actions.githubusercontent.com`);

const ghActionsRole = new aws.iam.Role("zenobase-github-actions", {
    name: "GitHubActionsZenobase",
    assumeRolePolicy: ghOidcArn.apply(oidcArn => JSON.stringify({
        Version: "2012-10-17",
        Statement: [{
            Effect: "Allow",
            Principal: { Federated: oidcArn },
            Action: "sts:AssumeRoleWithWebIdentity",
            Condition: {
                StringEquals: {
                    "token.actions.githubusercontent.com:aud": "sts.amazonaws.com",
                },
                StringLike: {
                    "token.actions.githubusercontent.com:sub": "repo:zenobase/zenobase:*",
                },
            },
        }],
    })),
    tags: { Name: "zenobase-github-actions" },
});

// ---------- Snyk IAM Role ----------

const snykRole = new aws.iam.Role("zenobase-snyk", {
    name: "ZenobaseSnyk",
    assumeRolePolicy: JSON.stringify({
        Version: "2012-10-17",
        Statement: [{
            Effect: "Allow",
            Principal: { AWS: "arn:aws:iam::198361731867:user/ecr-integration-user" },
            Action: "sts:AssumeRole",
            Condition: {
                StringEquals: {
                    "sts:ExternalId": "b937945a-4634-4cd6-9105-b254d5824443",
                },
            },
        }],
    }),
    tags: { Name: "zenobase-snyk" },
});

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

const ecsSg = new aws.ec2.SecurityGroup("zenobase-ecs-sg", {
    vpcId: vpc.id,
    description: "ECS - app traffic from ALB",
    ingress: [
        { protocol: "tcp", fromPort: 9000, toPort: 9000, securityGroups: [albSg.id] },
    ],
    egress: [
        { protocol: "-1", fromPort: 0, toPort: 0, cidrBlocks: ["0.0.0.0/0"] },
    ],
    tags: { Name: "zenobase-ecs" },
});

const osSg = new aws.ec2.SecurityGroup("zenobase-os-sg", {
    vpcId: vpc.id,
    description: "OpenSearch - HTTPS from ECS",
    ingress: [
        { protocol: "tcp", fromPort: 443, toPort: 443, securityGroups: [ecsSg.id] },
    ],
    egress: [
        { protocol: "-1", fromPort: 0, toPort: 0, cidrBlocks: ["0.0.0.0/0"] },
    ],
    tags: { Name: "zenobase-opensearch" },
});

// ---------- Bastion (for OpenSearch diagnostics) ----------

const bastionRole = new aws.iam.Role("zenobase-bastion-role", {
    assumeRolePolicy: JSON.stringify({
        Version: "2012-10-17",
        Statement: [{
            Action: "sts:AssumeRole",
            Effect: "Allow",
            Principal: { Service: "ec2.amazonaws.com" },
        }],
    }),
    tags: { Name: "zenobase-bastion" },
});

new aws.iam.RolePolicyAttachment("zenobase-bastion-ssm", {
    role: bastionRole.name,
    policyArn: "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore",
});

const bastionInstanceProfile = new aws.iam.InstanceProfile("zenobase-bastion-profile", {
    role: bastionRole.name,
});

let bastionInstanceId: pulumi.Output<string> = pulumi.output("");

if (bastionEnabled) {
    const bastionSg = new aws.ec2.SecurityGroup("zenobase-bastion-sg", {
        vpcId: vpc.id,
        description: "Bastion - SSM only, no SSH",
        egress: [
            { protocol: "-1", fromPort: 0, toPort: 0, cidrBlocks: ["0.0.0.0/0"] },
        ],
        tags: { Name: "zenobase-bastion" },
    });

    new aws.ec2.SecurityGroupRule("zenobase-os-bastion-ingress", {
        type: "ingress",
        securityGroupId: osSg.id,
        protocol: "tcp",
        fromPort: 443,
        toPort: 443,
        sourceSecurityGroupId: bastionSg.id,
    });

    const al2023Ami = aws.ec2.getAmi({
        mostRecent: true,
        owners: ["amazon"],
        filters: [
            { name: "name", values: ["al2023-ami-*-arm64"] },
            { name: "architecture", values: ["arm64"] },
            { name: "virtualization-type", values: ["hvm"] },
        ],
    });

    const bastion = new aws.ec2.Instance("zenobase-bastion", {
        ami: al2023Ami.then(ami => ami.id),
        instanceType: "t4g.nano",
        subnetId: subnetA.id,
        vpcSecurityGroupIds: [bastionSg.id],
        iamInstanceProfile: bastionInstanceProfile.name,
        tags: { Name: "zenobase-bastion" },
    });

    bastionInstanceId = bastion.id;
}

// ---------- ECR Repositories ----------

const ecrRepo = new aws.ecr.Repository("zenobase-api", {
    name: "zenobase-api",
    imageTagMutability: "MUTABLE",
});

new aws.ecr.LifecyclePolicy("zenobase-api-lifecycle", {
    repository: ecrRepo.name,
    policy: JSON.stringify({
        rules: [{
            rulePriority: 1,
            description: "Keep only the last 10 images",
            selection: {
                tagStatus: "any",
                countType: "imageCountMoreThan",
                countNumber: 10,
            },
            action: { type: "expire" },
        }],
    }),
});

new aws.iam.RolePolicy("zenobase-snyk-policy", {
    role: snykRole.name,
    policy: JSON.stringify({
        Version: "2012-10-17",
        Statement: [
            {
                Sid: "SnykAllowPull",
                Effect: "Allow",
                Action: [
                    "ecr:GetLifecyclePolicyPreview",
                    "ecr:GetDownloadUrlForLayer",
                    "ecr:BatchGetImage",
                    "ecr:DescribeImages",
                    "ecr:GetAuthorizationToken",
                    "ecr:DescribeRepositories",
                    "ecr:ListTagsForResource",
                    "ecr:ListImages",
                    "ecr:BatchCheckLayerAvailability",
                    "ecr:GetRepositoryPolicy",
                    "ecr:GetLifecyclePolicy",
                ],
                Resource: "*",
            },
        ],
    }),
});

export const snykRoleArn = snykRole.arn;

// ---------- OpenSearch Service ----------

const osLogGroup = new aws.cloudwatch.LogGroup("zenobase-opensearch-logs", {
    name: "/zenobase/opensearch",
    retentionInDays: 30,
});

const ecsTaskRole = new aws.iam.Role("zenobase-ecs-task-role", {
    assumeRolePolicy: JSON.stringify({
        Version: "2012-10-17",
        Statement: [{
            Action: "sts:AssumeRole",
            Effect: "Allow",
            Principal: { Service: "ecs-tasks.amazonaws.com" },
        }],
    }),
    tags: { Name: "zenobase-ecs-task" },
});

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

new aws.cloudwatch.LogResourcePolicy("zenobase-opensearch-log-policy", {
    policyName: "zenobase-opensearch-log-policy",
    policyDocument: pulumi.all([accountId]).apply(([account]) => JSON.stringify({
        Version: "2012-10-17",
        Statement: [{
            Effect: "Allow",
            Principal: { Service: "es.amazonaws.com" },
            Action: ["logs:PutLogEvents", "logs:CreateLogStream"],
            Resource: `arn:aws:logs:${region}:${account}:log-group:/zenobase/opensearch:*`,
        }],
    })),
});

const osDomain = new aws.opensearch.Domain(`zenobase-os-${opensearchDomain}`, {
    domainName: opensearchDomain,
    engineVersion: opensearchVersion,
    clusterConfig: {
        instanceType: opensearchInstanceType,
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
    accessPolicies: pulumi.all([accountId, ecsTaskRole.arn, osSnapshotRole.arn, bastionRole.arn]).apply(([account, taskArn, snapshotArn, bastionArn]) => JSON.stringify({
        Version: "2012-10-17",
        Statement: [{
            Effect: "Allow",
            Principal: { AWS: [taskArn, snapshotArn, bastionArn] },
            Action: "es:*",
            Resource: `arn:aws:es:${region}:${account}:domain/${opensearchDomain}/*`,
        }],
    })),
    logPublishingOptions: [
        {
            logType: "ES_APPLICATION_LOGS",
            cloudwatchLogGroupArn: osLogGroup.arn,
        },
        {
            logType: "INDEX_SLOW_LOGS",
            cloudwatchLogGroupArn: osLogGroup.arn,
        },
        {
            logType: "SEARCH_SLOW_LOGS",
            cloudwatchLogGroupArn: osLogGroup.arn,
        },
    ],
    tags: { Name: opensearchDomain },
}, { retainOnDelete: true });

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

new aws.iam.RolePolicy("zenobase-bastion-opensearch", {
    role: bastionRole.name,
    policy: pulumi.all([osDomain.arn, osSnapshotRole.arn]).apply(([domainArn, snapshotRoleArn]) => JSON.stringify({
        Version: "2012-10-17",
        Statement: [
            {
                Effect: "Allow",
                Action: "es:ESHttp*",
                Resource: `${domainArn}/*`,
            },
            {
                Effect: "Allow",
                Action: "iam:PassRole",
                Resource: snapshotRoleArn,
            },
        ],
    })),
});

// ---------- IAM ----------

const ecsExecutionRole = new aws.iam.Role("zenobase-ecs-execution-role", {
    assumeRolePolicy: JSON.stringify({
        Version: "2012-10-17",
        Statement: [{
            Action: "sts:AssumeRole",
            Effect: "Allow",
            Principal: { Service: "ecs-tasks.amazonaws.com" },
        }],
    }),
    tags: { Name: "zenobase-ecs-execution" },
});

new aws.iam.RolePolicyAttachment("zenobase-ecs-execution-policy", {
    role: ecsExecutionRole.name,
    policyArn: "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy",
});

new aws.iam.RolePolicy("zenobase-ecs-execution-secrets", {
    role: ecsExecutionRole.id,
    policy: pulumi.all([accountId]).apply(([account]) => JSON.stringify({
        Version: "2012-10-17",
        Statement: [{
            Effect: "Allow",
            Action: ["secretsmanager:GetSecretValue"],
            Resource: [`arn:aws:secretsmanager:${region}:${account}:secret:zenobase/*`],
        }],
    })),
});

new aws.iam.RolePolicy("zenobase-ecs-task-policy", {
    role: ecsTaskRole.id,
    policy: pulumi.all([accountId, osSnapshotRole.arn, osDomain.arn]).apply(([account, snapshotRoleArn, domainArn]) => JSON.stringify({
        Version: "2012-10-17",
        Statement: [
            {
                Effect: "Allow",
                Action: ["ses:SendEmail", "ses:SendRawEmail"],
                Resource: [`arn:aws:ses:${region}:${account}:identity/${sesIdentity}`],
            },
            {
                Effect: "Allow",
                Action: ["ses:GetEmailAddressInsights"],
                Resource: ["*"],
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

const tg = new aws.lb.TargetGroup("zenobase-tg", {
    port: 9000,
    protocol: "HTTP",
    vpcId: vpc.id,
    targetType: "ip",
    healthCheck: {
        path: "/observe/health/ready",
        port: "9000",
        protocol: "HTTP",
        healthyThreshold: 2,
        unhealthyThreshold: 3,
        timeout: 5,
        interval: 10,
    },
    deregistrationDelay: 60,
    tags: { Name: "zenobase" },
});

const httpsListener = new aws.lb.Listener("zenobase-https", {
    loadBalancerArn: alb.arn,
    port: 443,
    protocol: "HTTPS",
    certificateArn: certificateArn,
    sslPolicy: "ELBSecurityPolicy-TLS13-1-3-2021-06",
    defaultActions: [{
        type: "forward",
        targetGroupArn: tg.arn,
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

// ---------- WAF ----------

// Uncomment to block specific IP addresses (must be declared before the WebAcl):
// const wafBlockedIps = new aws.wafv2.IpSet("zenobase-blocked-ips", {
//     scope: "REGIONAL",
//     ipAddressVersion: "IPV4",
//     addresses: ["1.2.3.4/32", "5.6.0.0/16"],
//     tags: { Name: "zenobase" },
// });

const waf = new aws.wafv2.WebAcl("zenobase-waf", {
    scope: "REGIONAL",
    defaultAction: { allow: {} },
    visibilityConfig: {
        cloudwatchMetricsEnabled: true,
        metricName: "zenobase-waf",
        sampledRequestsEnabled: true,
    },
    rules: [
        // Uncomment to block specific IP addresses (also uncomment the IpSet above):
        // {
        //     name: "blocked-ips",
        //     priority: 0,
        //     action: { block: {} },
        //     statement: { ipSetReferenceStatement: { arn: wafBlockedIps.arn } },
        //     visibilityConfig: { cloudwatchMetricsEnabled: true, metricName: "blocked-ips", sampledRequestsEnabled: true },
        // },
        {
            name: "aws-common",
            priority: 1,
            overrideAction: { none: {} },
            statement: {
                managedRuleGroupStatement: { vendorName: "AWS", name: "AWSManagedRulesCommonRuleSet" },
            },
            visibilityConfig: { cloudwatchMetricsEnabled: true, metricName: "aws-common", sampledRequestsEnabled: true },
        },
        {
            name: "aws-known-bad-inputs",
            priority: 2,
            overrideAction: { none: {} },
            statement: {
                managedRuleGroupStatement: { vendorName: "AWS", name: "AWSManagedRulesKnownBadInputsRuleSet" },
            },
            visibilityConfig: { cloudwatchMetricsEnabled: true, metricName: "aws-known-bad-inputs", sampledRequestsEnabled: true },
        },
        // Uncomment to block a specific user agent (inline, no separate resource needed):
        // {
        //     name: "blocked-useragent",
        //     priority: 3,
        //     action: { block: {} },
        //     statement: {
        //         byteMatchStatement: {
        //             searchString: "badbot",
        //             fieldToMatch: { singleHeader: { name: "user-agent" } },
        //             textTransformations: [{ priority: 0, type: "LOWERCASE" }],
        //             positionalConstraint: "CONTAINS",
        //         },
        //     },
        //     visibilityConfig: { cloudwatchMetricsEnabled: true, metricName: "blocked-useragent", sampledRequestsEnabled: true },
        // },
        // Uncomment to block by country (ISO 3166-1 alpha-2 codes):
        // {
        //     name: "blocked-countries",
        //     priority: 4,
        //     action: { block: {} },
        //     statement: {
        //         geoMatchStatement: { countryCodes: ["CN", "RU"] },
        //     },
        //     visibilityConfig: { cloudwatchMetricsEnabled: true, metricName: "blocked-countries", sampledRequestsEnabled: true },
        // },
    ],
    tags: { Name: "zenobase" },
});

new aws.wafv2.WebAclAssociation("zenobase-waf-alb", {
    resourceArn: alb.arn,
    webAclArn: waf.arn,
});

// ---------- Secrets Manager ----------

const apiConfig = new aws.secretsmanager.Secret("zenobase-prod-api-config", {
    name: "zenobase/prod/zenobase-api-config",
    description: "Zenobase API application configuration",
});

// ---------- CloudWatch Log Groups ----------

new aws.cloudwatch.LogGroup("zenobase-api-logs", {
    name: "/zenobase/api",
    retentionInDays: 30,
});

// ---------- ECS Fargate ----------

const cluster = new aws.ecs.Cluster("zenobase-cluster", {
    name: "zenobase",
    tags: { Name: "zenobase" },
});

new aws.iam.RolePolicy("zenobase-github-actions-policy", {
    role: ghActionsRole.name,
    policy: pulumi.all([ecrRepo.arn, cluster.arn, ecsExecutionRole.arn, ecsTaskRole.arn]).apply(
        ([repoArn, clusterArn, execRoleArn, taskRoleArn]) => JSON.stringify({
            Version: "2012-10-17",
            Statement: [
                {
                    Effect: "Allow",
                    Action: "ecr:GetAuthorizationToken",
                    Resource: "*",
                },
                {
                    Effect: "Allow",
                    Action: [
                        "ecr:BatchCheckLayerAvailability",
                        "ecr:PutImage",
                        "ecr:InitiateLayerUpload",
                        "ecr:UploadLayerPart",
                        "ecr:CompleteLayerUpload",
                        "ecr:BatchGetImage",
                        "ecr:GetDownloadUrlForLayer",
                    ],
                    Resource: repoArn,
                },
                {
                    Effect: "Allow",
                    Action: [
                        "ecs:RegisterTaskDefinition",
                        "ecs:DeregisterTaskDefinition",
                        "ecs:DescribeTaskDefinition",
                        "ecs:ListTaskDefinitions",
                    ],
                    Resource: "*",
                },
                {
                    Effect: "Allow",
                    Action: [
                        "ecs:DescribeServices",
                        "ecs:UpdateService",
                    ],
                    Resource: `arn:aws:ecs:*:*:service/zenobase/*`,
                },
                {
                    Effect: "Allow",
                    Action: "ecs:DescribeClusters",
                    Resource: clusterArn,
                },
                {
                    Effect: "Allow",
                    Action: "iam:PassRole",
                    Resource: [execRoleArn, taskRoleArn],
                },
            ],
        }),
    ),
});

const opensearchReplayUrl = opensearchReplayDomain
    ? pulumi.output(aws.opensearch.getDomain({ domainName: opensearchReplayDomain })).apply(d => `https://${d.endpoint}`)
    : pulumi.output("");
const opensearchRebuildUrl = opensearchRebuildDomain
    ? pulumi.output(aws.opensearch.getDomain({ domainName: opensearchRebuildDomain })).apply(d => `https://${d.endpoint}`)
    : pulumi.output("");

const taskDefinition = new aws.ecs.TaskDefinition("zenobase-task", {
    family: "zenobase-api",
    requiresCompatibilities: ["FARGATE"],
    networkMode: "awsvpc",
    cpu: fargateCpu,
    memory: fargateMemory,
    runtimePlatform: {
        cpuArchitecture: "ARM64",
        operatingSystemFamily: "LINUX",
    },
    executionRoleArn: ecsExecutionRole.arn,
    taskRoleArn: ecsTaskRole.arn,
    containerDefinitions: pulumi.all([
        ecrRepo.repositoryUrl,
        osDomain.endpoint,
        osSnapshotRole.arn,
        apiConfig.arn,
        opensearchReplayUrl,
        opensearchRebuildUrl,
    ]).apply(([repoUrl, osEndpoint, snapshotRoleArn, secretArn, replayUrl, rebuildUrl]) =>
        JSON.stringify([{
            name: "api",
            image: `${repoUrl}:${imageTag}`,
            essential: true,
            portMappings: [{ containerPort: 9000, protocol: "tcp" }],
            environment: [
                { name: "AWS_REGION", value: region },
                { name: "OPENSEARCH_HOST", value: `https://${osEndpoint}` },
                { name: "OPENSEARCH_SNAPSHOT_BUCKET", value: opensearchSnapshotBucket },
                { name: "OPENSEARCH_SNAPSHOT_ROLE_ARN", value: snapshotRoleArn },
                { name: "OPENSEARCH_REPLAY", value: replayUrl },
                { name: "OPENSEARCH_REBUILD", value: rebuildUrl },
                { name: "HOSTNAME", value: hostname },
                { name: "API_HOSTNAME", value: apiHostname },
                { name: "OAUTH_HOSTNAME", value: oauthHostname },
                { name: "SENTRY_DSN", value: sentryDsn },
                { name: "SENTRY_RELEASE", value: imageTag },
            ],
            secrets: [
                { name: "APPLICATION_CONF", valueFrom: secretArn },
            ],
            healthCheck: {
                command: ["CMD", "wget", "--quiet", "--tries=1", "--spider", "http://localhost:9000/observe/health/live"],
                interval: 30,
                timeout: 5,
                retries: 3,
                startPeriod: 60,
            },
            logConfiguration: {
                logDriver: "awslogs",
                options: {
                    "awslogs-region": region,
                    "awslogs-group": "/zenobase/api",
                    "awslogs-stream-prefix": "ecs",
                },
            },
        }]),
    ),
}, imageTag ? {} : { ignoreChanges: ["containerDefinitions"] });

new aws.ecs.Service("zenobase-service", {
    name: "zenobase-api",
    cluster: cluster.arn,
    taskDefinition: taskDefinition.arn,
    desiredCount: 1,
    launchType: "FARGATE",
    platformVersion: "LATEST",
    networkConfiguration: {
        subnets: [subnetA.id, subnetB.id],
        securityGroups: [ecsSg.id],
        assignPublicIp: true,
    },
    loadBalancers: [{
        targetGroupArn: tg.arn,
        containerName: "api",
        containerPort: 9000,
    }],
    healthCheckGracePeriodSeconds: 21600, // 6 hours: allow time for replay/rebuild before ALB health checks matter
    deploymentCircuitBreaker: {
        enable: true,
        rollback: true,
    },
}, { dependsOn: [httpsListener] });

// ---------- CloudWatch Alarms ----------

new aws.cloudwatch.MetricAlarm("zenobase-unhealthy-hosts", {
    alarmDescription: "ALB has unhealthy targets",
    namespace: "AWS/ApplicationELB",
    metricName: "UnHealthyHostCount",
    dimensions: {
        LoadBalancer: alb.arnSuffix,
        TargetGroup: tg.arnSuffix,
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

export const ghActionsRoleArn = ghActionsRole.arn;
export const vpcId = vpc.id;
export const albDnsName = alb.dnsName;
export const albArn = alb.arn;
export const ecrUrl = ecrRepo.repositoryUrl;
export const opensearchEndpoint = osDomain.endpoint;
export const opensearchSnapshotRoleArn = osSnapshotRole.arn;
export const ecsClusterName = cluster.name;
export const ecsServiceName = "zenobase-api";
export { bastionInstanceId };
