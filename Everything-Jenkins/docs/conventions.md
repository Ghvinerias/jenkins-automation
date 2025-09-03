Conventions

General
- Use Shared Library steps for repeated logic (build, deploy, notifications)
- Store secrets in Jenkins Credentials; never hardcode tokens/URLs
- Prefer parameterized pipelines; avoid environment hardcoding

Naming
- Folders: kebab-case or PascalCase consistently per org standard
- Jobs: <service>/<env>/<action> where applicable

Pipelines
- Keep Jenkinsfile small; delegate to library steps
- Use archiveArtifacts for reproducible downstream consumption
