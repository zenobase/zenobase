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
- **GitHub Actions** in `.github/workflows/`
- **Java version** — when a new LTS is available, update `maven.compiler.source`
  in `pom.xml`, the `eclipse-temurin` tag in `docker/Dockerfile`, and
  `java-version` in the workflow files together
- **Docker base image patch** — patch-level `eclipse-temurin` bumps (same Java
  major); also keep the OpenSearch image in `OpenSearchTestSupport.java` and
  the CI service definitions in sync with the `opensearch-java` client version
  in `pom.xml`
- **Node.js / pnpm** — `.nvmrc`, the `engines` field in `package.json`, and
  GitHub Actions
