# Runbook

## Deploy

The container runs on ECS Fargate. Deployments use ECS rolling updates — a new
task starts, passes health checks, then the old task is drained automatically.

1. Pushing to main builds and pushes Docker images to ECR; on other branches, builds must be triggered manually

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

4. Deploy by triggering the "Deploy" workflow in GitHub Actions for the target branch

5. Monitor the deployment in the AWS Console (ECS, or CloudWatch Logs)

Roll back to a previous image:

```sh
pulumi config set zenobase:imageTag <sha>
pulumi up
```

## Run a replay migration

Use this procedure to rebuild the database by replaying the entire command history into a new managed OpenSearch domain
(e.g. after data model changes, and to purge data associated with closed accounts).

1. Set migration config (note: creating the domain takes 15-30 minutes):

    ```sh
    cd infra
    pulumi config set zenobase:opensearchDomain <new-domain-name>
    pulumi config set zenobase:opensearchReplayDomain <old-domain-name>
    pulumi up
    ```

    ECS starts a new task that connects to the new OpenSearch domain and
    replays all commands from the old domain. The old task continues serving
    traffic during the replay.

2. Monitor replay progress in the AWS Console (ECS or CloudWatch Logs)

3. When replay completes with no errors, the new task passes its health check and ECS automatically routes traffic to it

4. Clear migration config for the next deploy:

    ```sh
    pulumi config set zenobase:opensearchReplayDomain ""
    ```

5. Delete the old OpenSearch domain via the AWS Console

## Run a rebuild migration

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

2. Monitor rebuild progress in the AWS Console (ECS or CloudWatch Logs)

3. When rebuild completes with no errors, the new task passes its health check and ECS automatically routes traffic to it

4. Clear migration config for the next deploy:

    ```sh
    pulumi config set zenobase:opensearchRebuildDomain ""
    ```

5. Delete the old OpenSearch domain via the AWS Console

## Restore an OpenSearch snapshot

Use this procedure to restore from an S3 snapshot into a new managed OpenSearch domain.

1. Enable the bastion host (see below):

    ```sh
    pulumi config set zenobase:bastionEnabled true
    pulumi up
    ```

2. Create a new domain (without changing the running service):

    ```sh
    pulumi config set zenobase:opensearchDomain "zeno-opensearch-NNN"
    pulumi preview
    ```

    Copy the URN of the new OpenSearch domain from the preview output, then create only that resource:

    ```sh
    pulumi up --target '<opensearch-domain-urn>'
    ```

    Wait for the domain to become active (15-30 minutes)

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

    Once connected, set the variables on the bastion using the echoed values.
    `awscurl` picks up instance profile credentials automatically.

4. Register the S3 snapshot repository (the `base_path` must match the source domain name, as snapshots are stored under that path in S3):

    ```sh
    awscurl -X PUT --service es --region us-east-1 \
      "https://$TARGET_ENDPOINT/_snapshot/$SOURCE_DOMAIN" \
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
    awscurl --service es --region us-east-1 \
      "https://$TARGET_ENDPOINT/_snapshot/$SOURCE_DOMAIN/_all?pretty=true"
    ```

6. Delete default indices that conflict with the snapshot:

    ```sh
    awscurl -X DELETE --service es --region us-east-1 \
      "https://$TARGET_ENDPOINT/.*"
    ```

7. Restore a snapshot:

    ```sh
    SNAPSHOT_ID=<snapshot-id>

    awscurl -X POST --service es --region us-east-1 \
      "https://$TARGET_ENDPOINT/_snapshot/$SOURCE_DOMAIN/$SNAPSHOT_ID/_restore" \
      -H "Content-Type: application/json"
    ```

8. Monitor progress:

    ```sh
    awscurl --service es --region us-east-1 \
      "https://$TARGET_ENDPOINT/_cat/recovery"
    ```

9. Switch the service to the new domain:

    ```sh
    pulumi up
    ```

    This updates the ECS task definition to point at the new domain.

10. Disable the bastion and delete the old domain when done:

    ```sh
    pulumi config rm zenobase:bastionEnabled
    pulumi up
    ```

## Connect to OpenSearch

An on-demand bastion instance can be enabled for direct access to the OpenSearch cluster. It uses SSM Session Manager,
so no SSH keys or open inbound ports are required. Requests to OpenSearch must be SigV4-signed; use `brew install awscurl`.

1. Enable:

    ```sh
    pulumi config set zenobase:bastionEnabled true
    pulumi up
    ```

2. Start an SSM port-forward to the OpenSearch endpoint:

    ```sh
    OS_ENDPOINT=$(pulumi stack output opensearchEndpoint)
    aws ssm start-session \
      --target $(pulumi stack output bastionInstanceId) \
      --document-name AWS-StartPortForwardingSessionToRemoteHost \
      --parameters "host=$OS_ENDPOINT,portNumber=443,localPortNumber=9201"
    ```

3. Example queries (from another terminal — SigV4 signs against the real domain, so pass it via `Host`):

    ```sh
    OS_ENDPOINT=$(pulumi stack output opensearchEndpoint)
    awscurl --service es --region us-east-1 -k \
      -H "Host: $OS_ENDPOINT" \
      https://localhost:9201/_cat/indices?v
    ```

4. Disable when done:

    ```sh
    pulumi config rm zenobase:bastionEnabled
    pulumi up
    ```

The bastion creates a `t4g.nano` instance, a security group, an IAM role with SSM access, and an ingress rule on the OpenSearch security group. All resources are removed when disabled.

## Run a script

One-off admin scripts are located in `src/main/java/com/zenobase/scripts/`. Some scripts need AWS credentials configured in the environment, or an API token in `~/.zeno/token`.

For example, list all buckets for a user:

```sh
./mvnw compile exec:exec -Dexec.executable=java \
  -Dexec.args="-classpath %classpath com.zenobase.scripts.ListBuckets <userId>"
```

Show usage help for a script:

```sh
./mvnw compile exec:exec -Dexec.executable=java \
  -Dexec.args="-classpath %classpath com.zenobase.scripts.ListBuckets --help"
```
