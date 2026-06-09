# Zenobase API

Java 25 / Helidon SE backend for [zenobase/zenobase-web](https://github.com/zenobase/zenobase-web/), deployed to [api.zenobase.com](https://api.zenobase.com/).

## Getting Started

1. Install Java 25
2. Install Node.js (see `.nvmrc`) with pnpm via Corepack (`corepack enable`)
3. Run `pnpm install` to install Prettier
4. Copy `conf/application-local.yaml.template` to `conf/application-local.yaml`, and optionally set API keys for third-party integrations
5. `./run.sh` builds and starts a backend container on http://localhost:9000, including OpenSearch and localauth0 containers

## Development

- Format code with `./mvnw spotless:apply`
- `./mvnw compile` automatically performs [static code analysis](https://errorprone.info/)
- Run unit tests with `./mvnw test`, and integration tests with `./mvnw verify`
- Pushing to `main` triggers a GitHub Actions workflow that runs all checks, and builds and pushes an image to ECR
- Running the "Deploy" workflow deploys the image to ECS Fargate
- Changes to [infra/](./infra/) must be deployed first by running `./deploy.sh` locally, which runs `pulumi up` with the compliance [policy pack](./infra/policy/) enforced (`imageTag` can be left blank)
- For instructions on updating secrets, rolling back deployments, performing migrations, or restoring from backups, see [RUNBOOK.md](RUNBOOK.md)
