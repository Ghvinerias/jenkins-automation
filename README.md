Jenkins Shared Library: .NET build and archive

This repository provides a Jenkins Shared Library exposing a `dotnetBuild` step to build .NET (Core/5+/6+/7+) projects on Linux or Windows agents and archive the published artifacts.

Features
- Works on both Linux (sh) and Windows (bat) agents using Jenkins' `isUnix()` detection
- Optional restore, test, and publish phases
- Configurable project path, configuration, output folder, and extra arguments
- Archives published artifacts on successful builds (with fingerprinting)

Library structure
- `vars/dotnetBuild.groovy` – global step implementation
- `Jenkinsfile.example` – sample pipeline usage

Usage
1) Configure this repository as a Global Shared Library in Jenkins:
   - Manage Jenkins → System → Global Pipeline Libraries
   - Name: `jenkins-automation` (or any name you prefer)
   - Default version: `main` (or a tag/branch)
   - Retrieval method: Modern SCM → your Git settings

2) In your pipelines, load and use the step:

@Library('jenkins-automation') _

pipeline {
  agent any
  stages {
    stage('Build .NET') {
      steps {
        dotnetBuild(
          project: 'src/MyApp.sln',
          configuration: 'Release',
          restore: true,
          test: false,            // set true to run tests
          publish: true,
          publishOutput: 'artifacts',
          archive: true,
          archivePattern: 'artifacts/**/*',
          version: env.BUILD_NUMBER, // optional
          additionalArgs: ''          // optional
        )
      }
    }
  }
}

Parameters
- project (string, default `.`): Path to `.sln` or `.csproj` to build.
- configuration (string, default `Release`): Build configuration.
- restore (bool, default `true`): Run `dotnet restore`.
- test (bool, default `false`): Run `dotnet test` with `--no-build`.
- publish (bool, default `true`): Run `dotnet publish` with `--no-build`.
- publishOutput (string, default `artifacts`): Output directory for publish.
- archive (bool, default `true`): Archive artifacts after publish.
- archivePattern (string, default `artifacts/**/*`): Glob for archived files.
- nugetConfigPath (string, optional): Path to a `NuGet.config` for restore.
- version (string/number, optional): Sets `-p:Version=<value>`.
- runtime (string, optional): Adds `-r <RID>` to publish.
- additionalArgs (string, optional): Extra args appended to dotnet commands.
- clean (bool, default `false`): Runs `dotnet clean` before build.
- noRestoreOnBuild (bool, default `true`): Adds `--no-restore` to build when restore runs separately.

Notes
- This step assumes the agent has the required .NET SDK installed and on PATH.
- On Windows, `bat` is used; on Linux, `sh` is used. No change is required in your Jenkinsfile; detection is automatic.
- If your project outputs a specific artifact (e.g., zip), set `archivePattern` to match it (e.g., `artifacts/**/*.zip`).

Example
See `Jenkinsfile.example` for a complete pipeline.
