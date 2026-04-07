# Developer Guide

## Getting Started

Install Java 25.

Copy `conf/application-local.yaml.template` to `conf/application-local.yaml`
(and fill in API keys for third-party integrations as needed).

Run with Docker:

```sh
./run.sh
```

This starts both the app (port 9000) and OpenSearch (port 9200) in containers.

Alternatively, run without Docker for faster iteration:

```sh
docker compose up opensearch
./mvnw compile exec:exec -Dexec.executable=java \
  -Dexec.classpathScope=runtime \
  -Dexec.args="-classpath %classpath com.zenobase.Main"
```

## Running Tests

To run unit tests only (no Docker or AWS credentials needed):

```sh
./mvnw test
```

To run all tests including integration tests (needs Docker for OpenSearch):

```sh
./mvnw verify
```

To run a specific test class:

```sh
./mvnw test -Dtest=com.zenobase.models.LocationTest
```

To run a specific test method:

```sh
./mvnw test -Dtest="com.zenobase.models.LocationTest#testIsValid"
```

## Deploy Procedure

The container runs on ECS Fargate. Deployments use ECS rolling updates — a new
task starts, passes health checks, then the old task is drained automatically.

1. Push to master. CI runs tests, builds Docker images, and pushes to ECR.

2. If there are new secrets, update them:

   ```sh
   aws secretsmanager get-secret-value --secret-id zenobase/prod/zenobase-api-config \
     --query SecretString --output text > application-prod.yaml

   aws secretsmanager put-secret-value --secret-id zenobase/prod/zenobase-api-config \
     --secret-string file://./application-prod.yaml
   ```

3. If there are infrastructure changes, run Pulumi:

   ```sh
   cd infra
   pulumi up
   ```

4. Deploy by triggering the "Deploy" workflow in GitHub Actions for the
   branch to deploy. The workflow runs `pulumi up` with the branch's
   HEAD commit SHA as the image tag.

   ECS starts a new task with the updated image. The old task continues
   serving traffic until the new task passes the ALB health check.

5. Verify health (monitor in CloudWatch Logs, check ECS service in AWS console).

To rollback to a previous image version:

```sh
pulumi config set zenobase:imageTag <sha>
pulumi up
```

## Migration Procedure (Replay)

Use this procedure to rebuild the database by replaying the entire command history
(e.g. after data model changes). Creates a new managed OpenSearch domain and replays
from the old domain.

1. Set migration config (note: creating the domain takes 15-30 minutes):

   ```sh
   cd infra
   pulumi config set zenobase:opensearchDomain <new-domain-name>
   pulumi config set zenobase:opensearchReplayDomain <old-domain-name>
   pulumi up
   ```

   ECS starts a new task that connects to the new OpenSearch domain and
   replays all commands from the old domain. The old task continues serving
   traffic during the replay (`healthCheckGracePeriodSeconds` allows up to
   3 hours for the new task to become healthy).

2. Monitor replay progress in CloudWatch Logs.

3. When replay completes, the new task passes its health check and ECS
   automatically routes traffic to it.

4. Clear migration config for the next deploy:

   ```sh
   pulumi config set zenobase:opensearchReplayDomain ""
   ```

5. Delete the old OpenSearch domain via the AWS console.

## Migration Procedure (Rebuild)

Use this procedure to rebuild the database and compact the command history.
Same pattern as Replay but uses `opensearchRebuildDomain`.

1. Set migration config (note: creating the domain takes 15-30 minutes):

   ```sh
   cd infra
   pulumi config set zenobase:opensearchDomain <new-domain-name>
   pulumi config set zenobase:opensearchRebuildDomain <old-domain-name>
   pulumi up
   ```

   ECS starts a new task that connects to the new OpenSearch domain and
   rebuilds state from the old domain's raw data.

2. Monitor rebuild progress in CloudWatch Logs.

3. When rebuild completes, the new task passes its health check and ECS
   automatically routes traffic to it.

4. Clear migration config for the next deploy:

   ```sh
   pulumi config set zenobase:opensearchRebuildDomain ""
   ```

5. Delete the old OpenSearch domain via the AWS console.

## Restore Procedure

Use this procedure to restore from an S3 snapshot into a new managed OpenSearch domain.

1. Enable the bastion host (see [Troubleshooting](#troubleshooting)):

   ```sh
   pulumi config set zenobase:bastionEnabled true
   pulumi up
   ```

2. Create a new domain (without changing the running service):

   ```sh
   pulumi config set zenobase:opensearchDomain "zeno-opensearch-NNN"
   pulumi preview
   ```

   Copy the URN of the new OpenSearch domain from the preview output, then
   create only that resource:

   ```sh
   pulumi up --target '<opensearch-domain-urn>'
   ```

   Wait for the domain to become active (15-30 minutes).

3. Gather values needed on the bastion, then connect:

   ```sh
   SOURCE_DOMAIN=<source-domain-name>
   TARGET_ENDPOINT=$(pulumi stack output opensearchEndpoint)
   SNAPSHOT_ROLE_ARN=$(pulumi stack output opensearchSnapshotRoleArn)

   echo "SOURCE_DOMAIN=$SOURCE_DOMAIN"
   echo "TARGET_ENDPOINT=$TARGET_ENDPOINT"
   echo "SNAPSHOT_ROLE_ARN=$SNAPSHOT_ROLE_ARN"

   aws ssm start-session --target $(pulumi stack output bastionInstanceId)
   ```

   Once connected, set the variables on the bastion using the echoed values,
   then load instance profile credentials for the curl commands below:

   ```sh
   TOKEN=$(curl -s -X PUT "http://169.254.169.254/latest/api/token" \
     -H "X-aws-ec2-metadata-token-ttl-seconds: 3600")
   ROLE=$(curl -s -H "X-aws-ec2-metadata-token: $TOKEN" \
     http://169.254.169.254/latest/meta-data/iam/security-credentials/)
   CREDS=$(curl -s -H "X-aws-ec2-metadata-token: $TOKEN" \
     http://169.254.169.254/latest/meta-data/iam/security-credentials/$ROLE)
   export AWS_ACCESS_KEY_ID=$(echo "$CREDS" | python3 -c "import sys,json; print(json.load(sys.stdin)['AccessKeyId'])")
   export AWS_SECRET_ACCESS_KEY=$(echo "$CREDS" | python3 -c "import sys,json; print(json.load(sys.stdin)['SecretAccessKey'])")
   export AWS_SESSION_TOKEN=$(echo "$CREDS" | python3 -c "import sys,json; print(json.load(sys.stdin)['Token'])")
   ```

4. Register the S3 snapshot repository (the `base_path` must match the
   source domain name, as snapshots are stored under that path in S3):

   ```sh
   curl -X PUT "https://$TARGET_ENDPOINT/_snapshot/$SOURCE_DOMAIN" \
     --aws-sigv4 "aws:amz:us-east-1:es" \
     --user "$AWS_ACCESS_KEY_ID:$AWS_SECRET_ACCESS_KEY" \
     -H "x-amz-security-token: $AWS_SESSION_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{
       "type": "s3",
       "settings": {
         "bucket": "zeno-snapshots",
         "base_path": "'$SOURCE_DOMAIN'",
         "region": "us-east-1",
         "role_arn": "'$SNAPSHOT_ROLE_ARN'"
       }
     }'
   ```

5. List available snapshots:

   ```sh
   curl -s "https://$TARGET_ENDPOINT/_snapshot/$SOURCE_DOMAIN/_all?pretty=true" \
     --aws-sigv4 "aws:amz:us-east-1:es" \
     --user "$AWS_ACCESS_KEY_ID:$AWS_SECRET_ACCESS_KEY" \
     -H "x-amz-security-token: $AWS_SESSION_TOKEN"
   ```

6. Delete default indices that conflict with the snapshot:

   ```sh
   curl -X DELETE "https://$TARGET_ENDPOINT/.*" \
     --aws-sigv4 "aws:amz:us-east-1:es" \
     --user "$AWS_ACCESS_KEY_ID:$AWS_SECRET_ACCESS_KEY" \
     -H "x-amz-security-token: $AWS_SESSION_TOKEN"
   ```

7. Restore a snapshot:

   ```sh
   SNAPSHOT_ID=<snapshot-id>

   curl -X POST "https://$TARGET_ENDPOINT/_snapshot/$SOURCE_DOMAIN/$SNAPSHOT_ID/_restore" \
     --aws-sigv4 "aws:amz:us-east-1:es" \
     --user "$AWS_ACCESS_KEY_ID:$AWS_SECRET_ACCESS_KEY" \
     -H "x-amz-security-token: $AWS_SESSION_TOKEN" \
     -H "Content-Type: application/json"
   ```

8. Monitor progress:

   ```sh
   curl -s "https://$TARGET_ENDPOINT/_cat/recovery" \
     --aws-sigv4 "aws:amz:us-east-1:es" \
     --user "$AWS_ACCESS_KEY_ID:$AWS_SECRET_ACCESS_KEY" \
     -H "x-amz-security-token: $AWS_SESSION_TOKEN"
   ```

9. Switch the service to the new domain:

   ```sh
   pulumi up
   ```

   This updates the ECS task definition to point at the new domain.

10. Disable the bastion and delete the old domain when done:

    ```sh
    pulumi config set zenobase:bastionEnabled ""
    pulumi up
    ```

## Troubleshooting

An on-demand bastion instance can be enabled for direct access to the OpenSearch cluster. It uses SSM Session Manager, so no SSH keys or open inbound ports required.

1. Enable:

   ```sh
   pulumi config set zenobase:bastionEnabled true
   pulumi up
   ```

2. Connect:

   ```sh
   aws ssm start-session --target $(pulumi stack output bastionInstanceId)
   ```

3. Example queries from the bastion:

   ```sh
   curl -s https://<os-endpoint>/_cat/indices?v
   curl -s https://<os-endpoint>/_cat/shards?v
   curl -s https://<os-endpoint>/_cluster/settings?include_defaults
   ```

4. Disable when done:

   ```sh
   pulumi config set zenobase:bastionEnabled ""
   pulumi up
   ```

The bastion creates a `t4g.nano` instance, a security group, an IAM role with SSM access, and an ingress rule on the OpenSearch security group. All resources are removed when disabled.

## Scripts

One-off admin scripts are located in `src/main/java/com/zenobase/scripts/`. Some scripts need AWS credentials configured in the environment, or an API token in `~/.zeno/token`.

Example — list all buckets for a user:

```sh
./mvnw compile exec:exec -Dexec.executable=java \
  -Dexec.args="-classpath %classpath com.zenobase.scripts.ListBuckets <userId>"
```

Show usage help for a script:

```sh
./mvnw compile exec:exec -Dexec.executable=java \
  -Dexec.args="-classpath %classpath com.zenobase.scripts.ListBuckets --help"
```
