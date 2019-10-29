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

    def credentials = readJSON(file: "${config.workspace}/secrets-deployment.json")
    def runtimeConfig = new JsonSlurper().parseText(JsonOutput.toJson(config.runtimeConfig)) + credentials
    writeFile(file: "${config.workspace}/runtime.config.json", text: JsonOutput.toJson(runtimeConfig))

    // mount secrets to the docker container
    template.services.main.volumes.push(
      "${config.workspace}/runtime.config.json:/usr/src/app/runtime.config.json"
    );

    // create docker compose manifest
    def manifest = JsonOutput.toJson(template)
    writeFile(file: filename, text: manifest)
  }
}
