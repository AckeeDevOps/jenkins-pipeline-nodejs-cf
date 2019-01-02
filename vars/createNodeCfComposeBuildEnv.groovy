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
            args: [
              PRIVATE_KEY: env.KEY
            ]
          ],
          volumes: [
            "./build:/usr/src/app-compiled/"
          ]
        ]
      ]
    ]

    // remove old builds
    sh(script: 'rm -rf ./build')
    sh(script: 'mkdir -p ./build')

    def manifest = JsonOutput.toJson(template)
    writeFile(file: filename, text: manifest)
  }
}
