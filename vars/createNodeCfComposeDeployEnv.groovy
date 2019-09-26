import groovy.json.*

def call(Map config, String filename) {
  withCredentials([file(credentialsId: config.firebaseSACredentials, variable: 'CREDENTIALS')]) {
    template = [
      version: '3.1',
      services: [
        main: [
          image: config.dockerImageTag,
          build: [
            context: './repo',
          ],
          environment: [
            'GOOGLE_APPLICATION_CREDENTIALS': readFile(CREDENTIALS)
          ],
          volumes: []
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
