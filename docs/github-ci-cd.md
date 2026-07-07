# GitHub and CI/CD Setup

This project includes GitHub Actions workflows for:

- validating Java services and dashboard assets;
- validating Docker Compose;
- building service Docker images;
- publishing service images to GitHub Container Registry.

## Workflows

### CI

File:

```text
.github/workflows/ci.yml
```

Runs on:

- pull requests;
- pushes to `main`;
- pushes to `develop`.

Checks:

- Java 17 setup;
- Node setup for dashboard CSS;
- `npm ci`;
- `npm run build:css`;
- Maven tests for all three services;
- Maven package for all three services;
- `docker compose config`;
- Docker image builds for local service images.

### Docker image publish

File:

```text
.github/workflows/docker-publish.yml
```

Runs on:

- pushes to `main`;
- tags matching `v*.*.*`;
- manual workflow dispatch.

Publishes images to GitHub Container Registry:

```text
ghcr.io/<owner>/<repo>/ingestion-service
ghcr.io/<owner>/<repo>/scoring-service
ghcr.io/<owner>/<repo>/scheduling-service
```

## First-time GitHub connection

From the project root:

```powershell
cd "D:\Build With Codex"
git init
git add .
git commit -m "Initial AI Talent Pipeline"
```

Create an empty GitHub repository, then connect it:

```powershell
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO.git
git push -u origin main
```

If you use GitHub CLI:

```powershell
gh repo create YOUR_REPO --private --source . --remote origin --push
```

## Package permissions

The Docker publishing workflow uses:

```yaml
permissions:
  contents: read
  packages: write
```

No custom secret is required for GitHub Container Registry publishing because the workflow uses `GITHUB_TOKEN`.

## Local pre-push checks

Before pushing:

```powershell
cd "D:\Build With Codex\scoring-service"
npm.cmd run build:css
.\mvnw.cmd test

cd "D:\Build With Codex\ingestion-service"
.\mvnw.cmd test

cd "D:\Build With Codex\scheduling-service"
.\mvnw.cmd test

cd "D:\Build With Codex"
docker compose config
docker compose build ingestion-service scoring-service scheduling-service
```

