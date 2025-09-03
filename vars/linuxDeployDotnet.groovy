#!/usr/bin/env groovy

// Deploy a .NET app on Linux using systemd, copying from an archived artifact.
// Params:
// - serviceName (required)
// - serviceDir (default: /opt/Linux-Services/<service>)
// - logsDir (default: /opt/Logs/Linux-Services/<service>)
// - artifactPattern (default: **/target.zip)
// - dllName (optional; auto-detected from *.runtimeconfig.json if not provided)
// - aspnetEnv (default: Production)
// - urls (optional; sets ASPNETCORE_URLS)
// - serviceUser (default: root)
def call(Map p = [:]) {
  def service = p.serviceName
  if (!service) { error 'linuxDeployDotnet: serviceName is required' }

  def home = p.serviceDir ?: "/opt/Linux-Services/${service}"
  def logs = p.logsDir ?: "/opt/Logs/Linux-Services/${service}"
  def artifact = p.artifactPattern ?: '**/target.zip'
  def dll = p.dllName
  def aspnetEnv = p.aspnetEnv ?: 'Production'
  def urls = p.urls // e.g., http://0.0.0.0:5000
  def serviceUser = p.serviceUser ?: 'root'

  if (!isUnix()) {
    error 'linuxDeployDotnet can only run on Linux agents.'
  }

  stage('Fetch artifact') {
    sh '# ensure dirs'
    sh "mkdir -p ${home} ${logs}"
    sh "rm -rf ${home}/*"
    sh "unzip -o -d ${home} ${artifact} > /dev/null"
  }

  stage('Systemd unit') {
    if (!dll) {
      dll = sh(script: "basename ${home}/*.runtimeconfig.json .runtimeconfig.json", returnStdout: true).trim() + '.dll'
    }
    def envLines = [
      "Environment=ASPNETCORE_ENVIRONMENT=${aspnetEnv}"
    ]
    if (urls) envLines << "Environment=ASPNETCORE_URLS=${urls}"

    def unit = """
[Unit]
Description=${service}
After=network.target

[Service]
WorkingDirectory=${home}
ExecStart=/usr/bin/dotnet ${home}/${dll}
Restart=always
RestartSec=10
KillSignal=SIGINT
SyslogIdentifier=${service}
User=${serviceUser}
${envLines.join('\n')}

[Install]
WantedBy=multi-user.target
"""
    writeFile file: 'service.unit', text: unit
    sh "sudo mv service.unit /etc/systemd/system/${service}.service"
  }

  stage('Restart service') {
    sh 'sudo systemctl daemon-reload'
    sh "sudo systemctl enable ${service} || true"
    sh "sudo systemctl restart ${service} || sudo systemctl start ${service}"
    sleep 5
    def active = sh(script: "systemctl is-active ${service} || true", returnStdout: true).trim()
    echo "systemd is-active: ${active}"
    def status = sh(script: "systemctl status ${service} --no-pager --lines=5 || true", returnStdout: true).trim()
    echo status
  }
}
