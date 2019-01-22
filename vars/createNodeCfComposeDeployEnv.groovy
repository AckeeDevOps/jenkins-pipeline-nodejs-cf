import groovy.json.*

def call(Map config, String filename) {
  withCredentials([string(credentialsId: config.firebaseCredentialsId, variable: 'KEY')]) {
    template = [
      version: '3.1',
      services: [
        main: [
          image: config.dockerImageTag,
          build: [
            context: './repo',
          ],
          volumes: [],
          environment: [
            'FIREBASE_TOKEN': env.KEY 
          ]
        ]
      ]
    ]

    // create object with projectId
    def outputData = [
      "FIRESTORE_PROJECT": config.envDetails.gcpProjectId
    ]

    // insert specified secrets if needed
    if(config.secretsInjection) {
      
      // prepare structure for obtainNodeVaultSecrets
      def secrets = []
      for(c in config.secretsInjection.secrets){
        secrets.push([path: c.vaultSecretPath, keyMap: c.keyMap])
      }

      // get secrets from Valut
      def secretData = obtainNodeCfVaultSecrets(
        config.secretsInjection.vaultUrl,
        secrets,
        config.secretsInjection.jenkinsCredentialsId
      )

      // push secrets to the flat object
      for(c in config.secretsInjection.secrets){
        for(k in c.keyMap) {
          // select data from the obtained Map according to configuration
          if(secretData."${k.local}"){
            def dataPlain = secretData."${k.local}"
            outputData.put(k.local, dataPlain)
          } else {
            error "${k.vault} @ ${c.vaultSecretPath} seems to be non-existent!"
          }
        }
      }
    }

    // mount secrets to the docker container
    template.services.main.volumes.push(
      "${config.workspace}/secrets.json:/usr/src/app/dist/import/credentials.json"
    );

    // create file with secrets
    def outputDataJson = JsonOutput.toJson(outputData)
    def outputPrettyJson = JsonOutput.prettyPrint(outputDataJson)
    writeFile(file: "./secrets.json", text: outputPrettyJson)

    // create docker compose manifest
    def manifest = JsonOutput.toJson(template)
    writeFile(file: filename, text: manifest)
  }
}
