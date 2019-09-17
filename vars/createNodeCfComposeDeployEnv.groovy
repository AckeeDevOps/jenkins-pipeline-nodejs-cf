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

    // mount secrets to the docker container
    template.services.main.volumes.push(
      "${config.workspace}/secrets-deployment.json:/usr/src/app/functions/credentials.json"
    );

    // create docker compose manifest
    def manifest = JsonOutput.toJson(template)
    writeFile(file: filename, text: manifest)
  }
}
