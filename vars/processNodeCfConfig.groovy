import groovy.json.*

def call(Map cfg, String branch, String build, scm = null){
  def config = [:]

  // process simple stuff first
  config.workspace = pwd()
  config.buildNumber = build
  config.pipelineMode = cfg.pipelineMode
  config.gitlabCredentialsId = cfg.gitlabCredentialsId
  config.sshCredentialsId = cfg.sshCredentialsId
  config.firebaseCredentialsId = cfg.firebaseCredentialsId // required
  config.debugMode = cfg.debugMode

  // create dummy image tag
  config.dockerImageTag = "build-nodejs-cf-build${config.buildNumber}-${UUID.randomUUID().toString()}"

  // TO-DO:
  //   - validations
  // support for multibranch

  if(config.pipelineMode == 'MultibranchPipeline') {
    echo('running in Multibranch pipeline mode')
    //config.repositoryUrl = scm.getUserRemoteConfigs()[0].getUrl()
    config.branch = branch // get branch from the Jenkins runtime details
    config.envDetails = getNodeBranchConfig(cfg, config.branch)
    config.secretsInjection = config.envDetails.secretsInjection
  } else {
    echo('running in Simple pipeline mode')
    // convert top-level configuration to universal envDetails format
    config.envDetails = [:]
    config.envDetails.repositoryUrl = cfg.repositoryUrl
    config.envDetails.gcpProjectId = cfg.gcpProjectId
    config.branch = cfg.branch // get branch from Jenkinsfile
    config.secretsInjection = cfg.secretsInjection
  }

  echo config.dump()

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
