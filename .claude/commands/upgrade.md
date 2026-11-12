# Dependency upgrade pass

Each item below is one commit. PR description: before/after version table,
decisions on any major-version bumps, follow-up notes.

## Policy

- **24-hour cool-down**: do not apply any release less than 24 hours old.
- **Majors**: for each available major-version bump, summarize what changed
  (breaking changes, new features, migration effort) and ask whether to go
  ahead, skip, or defer. Record the decision and reasoning in the PR.
- Check Snyk alerts on GitHub for any outstanding CVEs.

## Upgrades

- **Java dependencies** in `pom.xml`
- **Prettier** — `prettier` and `prettier-plugin-java` in the root
  `package.json`; keep in sync with the matching `<devDependencies>` block
  inside `spotless-maven-plugin` in `pom.xml`; include any spotless reformats
- **Infra packages** in `infra/package.json`
- **Compliance policy pack** — `@pulumi/aws-compliance-policies` and
  `@pulumi/compliance-policy-manager` in `infra/policy/package.json`; after
  bumping, re-run the policy check (`pulumi preview --stack prod --policy-pack
./policy --policy-pack-config ./policy/config.json` from `infra/`) and triage
  any new findings — new pack versions add rules, and the pack is mandatory so
  new violations block `./deploy.sh`
- **GitHub Actions** in `.github/workflows/` — third-party actions use exact
  versions, GitHub-provided ones (`actions/*`) use major versions only. After
  editing refs, run `gh actions-lock --no-narrow` (never without `--no-narrow`:
  it rewrites refs) and `gh actions-lock --verify`, and commit `actions.lock`
  with the workflow change. `gh` needs network access to GitHub, so run it
  outside the sandbox if blocked. The lockfile doesn't cover container images
  (see the OpenSearch service below).
- **Java version** — when a new LTS is available, update `maven.compiler.source`
  in `pom.xml`, the `eclipse-temurin` tag in `docker/Dockerfile`, and
  `java-version` in the workflow files together (then re-run
  `gh actions-lock --no-narrow`)
- **Docker base image patch** — patch-level `eclipse-temurin` bumps (same Java
  major); also keep the OpenSearch image in `OpenSearchTestSupport.java` and
  the CI service definitions in sync with the `opensearch-java` client version
  in `pom.xml`
- **Node.js / pnpm** — `.nvmrc`, the `engines` field in `package.json`, and
  GitHub Actions (then re-run `gh actions-lock --no-narrow`)
