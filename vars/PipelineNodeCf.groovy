def call(body) {

  def cfg = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = cfg
  body()

  def agent = (cfg.agent != null) ? cfg.agent : ''

  node(agent) {
    // set current step for the notification handler
    def pipelineStep = "start"

    def config = processNodeCfConfig(cfg, env.BRANCH_NAME, env.BUILD_NUMBER, scm)

    try {

      // start of Checkout stage
      stage('Checkout') {
        pipelineStep = "checkout"
        if (!fileExists('repo')){ sh(script: 'mkdir -p repo') }
        dir('repo') {
          if(config.pipelineMode == 'Pipeline') {
            withCredentials([string(credentialsId: config.gitlabCredentialsId, variable: 'credentials')]) {
              checkout([$class: 'GitSCM',
                branches: [[name: "*/${config.branch}"]],
                doGenerateSubmoduleConfigurations: false,
                extensions: [[$class: 'CleanCheckout']],
                submoduleCfg: [],
                userRemoteConfigs: [[credentialsId: credentials, url: config.repositoryUrl]]
              ])
            }
          } else if(config.pipelineMode == 'MultibranchPipeline') {
            checkout(scm)
          } else {
            error(message: "Unknown pipeline mode")
          }
        }

        def changelog = getChangelog()
        echo(changelog)
        writeFile(file: "./changelog.txt", text: changelog)
        config.startedBy = getNodeCfAuthorName()
      }
      // end of Checkout stage

      // start of Build stage
      stage('Build') {
        pipelineStep = "build"
        createNodeCfComposeBuildEnv(config, './build.json') // create docker-compose file
        sh(script: "docker-compose -f ./build.json build")
      }
      // end of Build stage

      stage('Test') {}

      stage('Deploy') {

      }

     } catch(err) {
       currentBuild.result = "FAILURE"
       println(err.toString());
       println(err.getMessage());
       println(err.getStackTrace());
       throw err
     } finally {

     }
  }

}
