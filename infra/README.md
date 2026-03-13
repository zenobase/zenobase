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

# Attach permissions (EC2, VPC, ELB, ECR, IAM, Secrets Manager, CloudWatch, S3, SES, OpenSearch)
for policy in AmazonEC2FullAccess AmazonVPCFullAccess \
  ElasticLoadBalancingFullAccess AmazonEC2ContainerRegistryFullAccess \
  IAMFullAccess SecretsManagerReadWrite CloudWatchFullAccess \
  AmazonS3FullAccess AmazonSESFullAccess AmazonOpenSearchServiceFullAccess; do
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

1. Run `pulumi up` locally to create all infrastructure.
2. Create a DNS CNAME record pointing `zenobase.com` to the ALB DNS name (`pulumi stack output albDnsName`).
3. On the first run the instance will be unhealthy because there are no images in ECR yet.
4. Push to `master` to trigger the CI workflow (tests, builds, and pushes images to ECR).
5. Deploy using the procedure in the root README.
