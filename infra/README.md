# Infrastructure

## Prerequisites

- AWS account with CLI configured
- [Session Manager plugin](https://docs.aws.amazon.com/systems-manager/latest/userguide/session-manager-working-with-install-plugin.html) (for bastion access):
  ```sh
  curl "https://s3.amazonaws.com/session-manager-downloads/plugin/latest/mac_arm64/session-manager-plugin.pkg" -o "session-manager-plugin.pkg"
  sudo installer -pkg session-manager-plugin.pkg -target /
  sudo ln -s /usr/local/sessionmanagerplugin/bin/session-manager-plugin /usr/local/bin/session-manager-plugin
  rm session-manager-plugin.pkg
  ```
- GitHub repo (zenobase/zenobase)
- [Pulumi Cloud](https://app.pulumi.com/) account
- Node.js 18+

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

## Pulumi: Initial Setup

```sh
cd infra
npm install
pulumi stack init prod
```

Config values are in `Pulumi.prod.yaml`.

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
3. Create a DNS CNAME record pointing `zenobase.com` to the ALB DNS name (`pulumi stack output albDnsName`).
4. On the first run the instance will be unhealthy because there are no images in ECR yet.
5. Push to `master` to trigger the CI workflow (tests, builds, and pushes images to ECR).
6. Deploy using the procedure in the root README.
