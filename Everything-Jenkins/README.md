Everything-Jenkins

Purpose
- Centralize a reusable Jenkins Shared Library with minimal scaffolding. Job DSL and JCasC content are optional and can be ignored or removed.

Structure
- shared-library/ – Jenkins Shared Library (vars/, resources/) — use this
- pipelines/ – Example Jenkinsfiles showing how to call the library (optional)
- jenkins/casc/ – JCasC skeleton (optional; safe to delete if not used)
- jenkins/job-dsl/ – Seed and sample DSL jobs (optional; safe to delete if not used)
- docs/ – Conventions and notes

Using the Shared Library
1) In Jenkins: Manage Jenkins → System → Global Pipeline Libraries
	- Name: jenkins-automation (or any name you choose)
	- Default version: main
	- Retrieval: your Git SCM pointing to this repo
2) In a Jenkinsfile:
	- @Library('jenkins-automation') _
	- Call steps like dotnetBuild(...) or linuxDeployDotnet(...)

Notes
- Avoid hardcoding secrets. Use Jenkins Credentials and bind at runtime.
- If you won’t use DSL or JCasC, you can delete the jenkins/ and job-dsl/ folders.
