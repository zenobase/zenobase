# Infrastructure

The Pulumi program in this directory manages the following AWS resources:

- Elastic Container Service (ECS), runs the application's Docker image as Fargate tasks
- Elastic Container Registry, (ECR), stores the application's Docker image
- Application Load Balancer (ALB), routes requests to ECS
- Amazon OpenSearch Service, stores application data
- Secrets Manager, stores API keys
- Web Application Firewall (WAF), protects access to the ALB
- CloudWatch Logs, collects logs from ECS and OpenSearch
- Amazon Simple Email Service, sends transactional emails
- S3 bucket for storing ALB logs
- (Transient) EC2 bastion instances, used for troubleshooting

The following AWS resources are shared with the frontend, and not managed by Pulumi:

- Route 53, manages DNS records for \*.zenobase.com
- CloudTrail, captures an audit trail of account activity
- GuardDuty, detects potential threats
- AWS Config, detects policy violations
- AWS User Notifications, sends alerts for GuardDuty and AWS Config
- S3 buckets for storing OpenSearch snapshots, and CloudTrail and AWS Config records

## Prerequisites

- AWS account with CLI configured
- [Session Manager plugin](https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager-working-with-install-plugin.html) (for bastion access):
    ```sh
    curl "https://s3.amazonaws.com/session-manager-downloads/plugin/latest/mac_arm64/session-manager-plugin.pkg" -o "session-manager-plugin.pkg"
    sudo installer -pkg session-manager-plugin.pkg -target /
    sudo ln -s /usr/local/sessionmanagerplugin/bin/session-manager-plugin /usr/local/bin/session-manager-plugin
    rm session-manager-plugin.pkg
    ```
- Pulumi (`brew install pulumi`) with a Pulumi Cloud account
- Node.js (see `../.nvmrc`) with pnpm via Corepack (`corepack enable`)

## AWS: GitHub OIDC Identity Provider

Create a GitHub OIDC provider (once per AWS account) so GitHub Actions can assume AWS roles without long-lived credentials. Skip if already created for another project.

```sh
aws iam create-open-id-connect-provider \
  --url https://token.actions.githubusercontent.com \
  --client-id-list sts.amazonaws.com \
  --thumbprint-list 6938fd4d98bab03faadb97b34396831e3780aea1
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

Complete DNS validation by adding the CNAME/ALIAS records shown in the output. Note the certificate ARN for Pulumi config.

## Pulumi: Initial Setup

```sh
cd infra
pnpm install
pulumi stack init prod
```

Set the certificate ARN (written to `Pulumi.prod.yaml`):

```sh
pulumi config set certificateArn <arn>
```

Other config values are in `Pulumi.prod.yaml`.

## AWS: Secrets Manager

Store the production secrets file (API keys, credentials — not ES config, which is in `Pulumi.prod.yaml`):

```sh
aws secretsmanager create-secret \
  --name zenobase/prod/zenobase-api-config \
  --secret-string file://./prod.yaml \
  --region us-east-1
```

The ECS task retrieves this secret on startup and writes it to `/etc/app/prod.yaml`.

## Bootstrap

1. Run `pulumi up` locally to create all infrastructure (including the GitHub Actions IAM role).
2. Configure the GitHub Actions role ARN:
    ```sh
    gh variable set AWS_ROLE_ARN --body "$(pulumi stack output ghActionsRoleArn)"
    ```
3. Create a DNS ALIAS record pointing `zenobase.com` to the ALB DNS name (`pulumi stack output albDnsName`).
4. On the first run the service will be unhealthy because there are no images in ECR yet.
5. Push to `main` to trigger the CI workflow (tests, builds, and pushes images to ECR).
6. Deploy using the procedure in [RUNBOOK.md](../RUNBOOK.md).
