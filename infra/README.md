# Infrastructure

## Prerequisites

- AWS account with CLI configured
- GitHub repo (zenobase/zenobase)
- [Pulumi Cloud](https://app.pulumi.com/) account
- Node.js 18+

## AWS: OIDC Identity Provider

Create a GitHub OIDC provider so GitHub Actions can assume an AWS role without long-lived credentials.

```sh
aws iam create-open-id-connect-provider \
  --url https://token.actions.githubusercontent.com \
  --client-id-list sts.amazonaws.com \
  --thumbprint-list 6938fd4d98bab03faadb97b34396831e3780aea1
```

Save the following as `trust-policy.json`:

```json
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": {
      "Federated": "arn:aws:iam::<ACCOUNT_ID>:oidc-provider/token.actions.githubusercontent.com"
    },
    "Action": "sts:AssumeRoleWithWebIdentity",
    "Condition": {
      "StringEquals": {
        "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
      },
      "StringLike": {
        "token.actions.githubusercontent.com:sub": "repo:zenobase/zenobase:*"
      }
    }
  }]
}
```

```sh
# Create the IAM role
aws iam create-role \
  --role-name GitHubActionsZenobase \
  --assume-role-policy-document file://trust-policy.json

# Attach permissions (EC2, VPC, ELB, ECR, IAM, Secrets Manager, CloudWatch, S3, SES)
for policy in AmazonEC2FullAccess AmazonVPCFullAccess \
  ElasticLoadBalancingFullAccess AmazonEC2ContainerRegistryFullAccess \
  IAMFullAccess SecretsManagerReadWrite CloudWatchFullAccess \
  AmazonS3FullAccess AmazonSESFullAccess; do
  aws iam attach-role-policy \
    --role-name GitHubActionsZenobase \
    --policy-arn "arn:aws:iam::aws:policy/$policy"
done
```

## AWS: ACM Certificate

Check for an existing certificate:

```sh
aws acm list-certificates --region us-east-1
```

If none exists, request one:

```sh
aws acm request-certificate \
  --domain-name zenobase.com \
  --subject-alternative-names "*.zenobase.com" \
  --validation-method DNS \
  --region us-east-1
```

Complete DNS validation by adding the CNAME records shown in the output. Note the certificate ARN for Pulumi config.

## AWS: EC2 Key Pair

Check for an existing key pair:

```sh
aws ec2 describe-key-pairs --key-names zeno --region us-east-1
```

If none exists (or we no longer have access to the private key file), create one:

```sh
aws ec2 create-key-pair \
  --key-name zeno \
  --query 'KeyMaterial' \
  --output text \
  --region us-east-1 > zeno.pem
chmod 400 zeno.pem
```

## GitHub: Actions Configuration

```sh
gh variable set AWS_ROLE_ARN --body "<IAM role ARN from above>"
gh secret set PULUMI_ACCESS_TOKEN --body "<token from Pulumi Cloud>"
```

## Pulumi: Initial Setup

```sh
cd infra
npm install
pulumi stack init prod
```

Config values are in `Pulumi.prod.yaml`.

## AWS: Secrets Manager

Store the production Play secrets file (API keys, credentials — not ES config, which is in `Pulumi.prod.yaml`):

```sh
aws secretsmanager create-secret \
  --name zenobase/prod-conf \
  --secret-string file://./prod.conf \
  --region us-east-1
```

The EC2 instance retrieves this secret on startup and mounts it at `/etc/play/prod.conf`.

## Bootstrap

1. Run `pulumi up --stack prod` locally to create all infrastructure.
2. Create a DNS CNAME record pointing `zenobase.com` to the ALB DNS name (`pulumi stack output albDnsName --stack prod`).
3. On the first run the instance will be unhealthy because there are no images in ECR yet.
4. Push to `master` to trigger the CI workflow (tests, builds, and pushes images to ECR), then the Deploy workflow (runs `pulumi up` with a blue/green flip and waits for the health check to pass).

## SSH Access

SSH is disabled by default (no `adminCidr` configured in the security group).

To enable temporarily:

```sh
pulumi config set zenobase:adminCidr "$(curl -s ifconfig.me)/32" --stack prod
pulumi up --stack prod
ssh -i <key>.pem ec2-user@<ip>
```

To disable:

```sh
pulumi config rm zenobase:adminCidr --stack prod
pulumi up --stack prod
```

## Architecture Overview

**Infrastructure:**
- VPC with two public subnets across two AZs
- Application Load Balancer with HTTPS (TLS 1.3), HTTP-to-HTTPS redirect
- Blue/green target groups for zero-downtime deployments
- EC2 instance (Amazon Linux 2023 ARM64) running Docker Compose
  - Play application container (port 9000)
  - Elasticsearch container (ports 9200, 9300)
- ECR repositories: `zenobase-play`, `zenobase-elasticsearch`
- CloudWatch log groups (`/zenobase/play`, `/zenobase/elasticsearch`) with 30-day retention
- CloudWatch alarms for unhealthy hosts and 5xx errors
- S3 bucket `zeno-snapshots` for Elasticsearch snapshots

**CI workflow** (on push to `master`):
1. Run tests (`sbt test`)
2. Build and push Docker images to ECR (tagged with commit SHA + `latest`)

**Deploy workflow** (triggered after CI succeeds on `master`):
1. Flip the active target group (blue &rarr; green or green &rarr; blue)
2. Run `pulumi up` to create a new EC2 instance attached to the inactive target group
3. Wait up to 30 minutes for the new instance to pass ALB health checks (`/status`)
