import groovy.json.*

def call(Map config, String filename) {
  withCredentials([string(credentialsId: config.sshCredentialsId, variable: 'KEY')]) {
    template = [
      version: '3.1',
      services: [
        main: [
          image: config.dockerImageTag,
          build: [
            context: './repo',
          ],
          volumes: []
        ]
      ]
    ]

    if(config.secretsInjection) {
      def secrets = []
      for(c in config.secretsInjection.secrets){
        secrets.push([path: c.vaultSecretPath, keyMap: c.keyMap])
      }

      def secretData = obtainNodeVaultSecrets(
        config.secretsInjection.vaultUrl,
        secrets,
        config.secretsInjection.jenkinsCredentialsId
      )

      def outputData = [:]
      for(c in config.secretsInjection.secrets){
        for(k in c.keyMap) {
          // select data from the obtained Map according to configuration
          if(secretData."${k.local}"){
            def dataPlain = secretData."${k.local}"
            outputData.put(k.local, kubernetesData)
          } else {
            error "${k.vault} @ ${c.vaultSecretPath} seems to be non-existent!"
          }
        }
      }
    }

    def manifest = JsonOutput.toJson(template)
    writeFile(file: filename, text: manifest)
  }
}
