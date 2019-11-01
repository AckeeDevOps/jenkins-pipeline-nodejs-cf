import groovy.json.*

def call(Map cfg, String branch, String build, scm = null){
  def config = [:]

  // process simple stuff first
  config.workspace = pwd()
  config.buildNumber = build
  config.pipelineMode = cfg.pipelineMode
  config.gitlabCredentialsId = cfg.gitlabCredentialsId
  config.sshCredentialsId = cfg.sshCredentialsId
  config.firebaseSACredentials = cfg.firebaseSACredentials // required
  config.debugMode = cfg.debugMode
  config.runtimeConfig = cfg.runtimeConfig ? cfg.runtimeConfig : [:]
  config.envDefaults = cfg.envDefaults ? cfg.envDefaults : [:]

  // create dummy image tag
  config.dockerImageTag = "build-nodejs-cf-build${config.buildNumber}-${UUID.randomUUID().toString()}"

  // TO-DO:
  // support for multibranch

  if(config.pipelineMode == 'MultibranchPipeline') {
    echo('running in Multibranch pipeline mode')
    //config.repositoryUrl = scm.getUserRemoteConfigs()[0].getUrl()
    config.branch = branch // get branch from the Jenkins runtime details
    // merge defaults with the actual values, actual values have always precedence
    config.envDetails = config.envDefaults + getNodeBranchConfig(cfg, config.branch)
    config.secretsInjection = config.envDetails.secretsInjection
  } else {
    echo('running in Simple pipeline mode')
    // convert top-level configuration to universal envDetails format
    config.envDetails = config.envDefaults
    config.envDetails.repositoryUrl = cfg.repositoryUrl
    config.envDetails.gcpProjectId = cfg.gcpProjectId
    config.envDetails.friendlyEnvName = cfg.friendlyEnvName
    config.branch = cfg.branch // get branch from Jenkinsfile
    config.secretsInjection = cfg.secretsInjection
  }

  validateEnvDetailsString('gcpProjectId', config)
  validateEnvDetailsString('repositoryUrl', config)

  return config
}

def getNodeBranchConfig(Map cfg, branch) {
  def branchConfig = cfg.branchEnvs."${branch}"
  if(branchConfig) {
    return branchConfig
  } else {
    error(message: "Branch '${branch}' does not exist in the 'branchEnvs' Map.")
  }
}

def validateEnvDetailsString(String input, Map config) {
  if(!config.envDetails."${input}" || config.envDetails."${input}" == "") {
    error(message: "${input} has to be always set!")
  }
}
