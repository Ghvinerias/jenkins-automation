#!/usr/bin/env groovy

// Global step: dotnetBuild
// Builds .NET projects on Linux/Windows agents and archives published artifacts.
//
// Parameters (with defaults):
// - project (String, default '.')
// - configuration (String, default 'Release')
// - restore (boolean, default true)
// - test (boolean, default false)
// - publish (boolean, default true)
// - publishOutput (String, default 'artifacts')
// - archive (boolean, default true)
// - archivePattern (String, default 'artifacts/**/*')
// - nugetConfigPath (String, optional)
// - version (String/Number, optional) -> adds -p:Version=<value>
// - runtime (String, optional) -> adds -r <RID> to publish
// - additionalArgs (String, optional) -> appended to dotnet commands
// - clean (boolean, default false)
// - noRestoreOnBuild (boolean, default true)

def call(Map params = [:]) {
  def cfg = [
    project: params.get('project', '.'),
    configuration: params.get('configuration', 'Release'),
    restore: params.get('restore', true) as boolean,
    test: params.get('test', false) as boolean,
    publish: params.get('publish', true) as boolean,
    publishOutput: params.get('publishOutput', 'artifacts'),
    archive: params.get('archive', true) as boolean,
    archivePattern: params.get('archivePattern', 'artifacts/**/*'),
    nugetConfigPath: params.get('nugetConfigPath', null),
    version: params.get('version', null),
    runtime: params.get('runtime', null),
    additionalArgs: params.get('additionalArgs', ''),
    clean: params.get('clean', false) as boolean,
    noRestoreOnBuild: params.get('noRestoreOnBuild', true) as boolean,
  ]

  // Helper to run a command on the current OS
  def run = { String cmd ->
    if (isUnix()) {
      sh label: cmd, script: cmd
    } else {
      bat label: cmd, script: cmd
    }
  }

  // Simple quoter for paths/args with spaces
  def q = { String s ->
    if (s == null) return ''
    return '"' + s.replace('"','\\"') + '"'
  }

  withEnv(['DOTNET_CLI_TELEMETRY_OPTOUT=1', 'DOTNET_SKIP_FIRST_TIME_EXPERIENCE=1']) {
    echo "Using .NET SDK on ${isUnix() ? 'Linux/Unix' : 'Windows'} agent"
    run 'dotnet --info'

    if (cfg.clean) {
      echo 'Cleaning project'
      run "dotnet clean ${q(cfg.project)} -c ${cfg.configuration} ${cfg.additionalArgs}".trim()
    }

    if (cfg.restore) {
      echo 'Restoring packages'
      def restoreCmd = new StringBuilder("dotnet restore ${q(cfg.project)}")
      if (cfg.nugetConfigPath) {
        restoreCmd.append(" --configfile ${q(cfg.nugetConfigPath)}")
      }
      if (cfg.additionalArgs?.trim()) {
        restoreCmd.append(' ').append(cfg.additionalArgs.trim())
      }
      run restoreCmd.toString()
    }

    echo 'Building project'
    def buildCmd = new StringBuilder("dotnet build ${q(cfg.project)} -c ${cfg.configuration}")
    if (cfg.noRestoreOnBuild && cfg.restore) {
      buildCmd.append(' --no-restore')
    }
    if (cfg.version) {
      buildCmd.append(" -p:Version=${cfg.version}")
    }
    if (cfg.additionalArgs?.trim()) {
      buildCmd.append(' ').append(cfg.additionalArgs.trim())
    }
    run buildCmd.toString()

    if (cfg.test) {
      echo 'Running tests'
      def testCmd = new StringBuilder("dotnet test ${q(cfg.project)} -c ${cfg.configuration} --no-build")
      if (cfg.additionalArgs?.trim()) {
        testCmd.append(' ').append(cfg.additionalArgs.trim())
      }
      run testCmd.toString()
    }

    if (cfg.publish) {
      echo "Publishing to ${cfg.publishOutput}"
      def publishCmd = new StringBuilder("dotnet publish ${q(cfg.project)} -c ${cfg.configuration} -o ${q(cfg.publishOutput)} --no-build")
      if (cfg.runtime) {
        publishCmd.append(" -r ${cfg.runtime}")
      }
      if (cfg.version) {
        publishCmd.append(" -p:Version=${cfg.version}")
      }
      if (cfg.additionalArgs?.trim()) {
        publishCmd.append(' ').append(cfg.additionalArgs.trim())
      }
      run publishCmd.toString()
    } else if (cfg.archive) {
      echo 'Warning: archive=true but publish=false; ensure archivePattern matches your build outputs.'
    }

    if (cfg.archive) {
      echo "Archiving artifacts: ${cfg.archivePattern}"
      archiveArtifacts artifacts: cfg.archivePattern, allowEmptyArchive: false, fingerprint: true, onlyIfSuccessful: true
    }
  }
}
