import groovy.json.*

def call(Map config, String filename) {
  def secrets = []
  for(c in config.secretsInjection.secrets){
    secrets.push([path: c.vaultSecretPath, keyMap: c.keyMap])
  }

  def secretData = obtainNodeCfVaultSecrets(
    config.secretsInjection.vaultUrl,
    secrets,
    config.secretsInjection.jenkinsCredentialsId
  )

  def outputData = [:]

  for(c in config.secretsInjection.secrets){
    for(k in c.keyMap) {
      if(secretData."${k.local}"){
        outputData.put(k.local, secretData."${k.local}")
      } else {
        error "${k.vault} @ ${c.vaultSecretPath} seems to be non-existent!"
      }
    }
  }

  def manifest = JsonOutput.toJson(outputData)
  writeFile(file: filename, text: manifest)
}
