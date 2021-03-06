def call(body) {

  def cfg = [:]
  body.resolveStrategy = Closure.DELEGATE_FIRST
  body.delegate = cfg
  body()

  def agent = (cfg.agent != null) ? cfg.agent : ''

  node(agent) {
    // set current step for the notification handler
    def pipelineStep = "start"
    def config = processNodeCfConfig(cfg, env.BRANCH_NAME, env.BUILD_NUMBER)

    try {
      // start of Checkout stage
      stage('Checkout') {
        pipelineStep = "checkout"
        echo "Clonning ${config.branch} of ${config.repositoryUrl}"
        if (!fileExists('repo')){ sh(script: 'mkdir -p repo') }
        dir('repo') {
          withCredentials([string(credentialsId: config.gitlabCredentialsId, variable: 'credentials')]) {
            checkout([$class: 'GitSCM',
              branches: [[name: "*/${config.branch}"]],
              doGenerateSubmoduleConfigurations: false,
              extensions: [[$class: 'CleanCheckout']],
              submoduleCfg: [],
              userRemoteConfigs: [[credentialsId: credentials, url: config.envDetails.repositoryUrl]]
            ])
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

      // start of Test stage
      stage('Test') {
        pipelineStep = "test"
        echo("No tests so far :)")
      }
      // end of Test stage

      // start of Lint stage
      stage('Lint') {
        pipelineStep = "test"
        echo("No linting so far :)")
      }
      // end of Lint stage

      // start of Deploy stage
      stage('Deploy') {
        pipelineStep = "deploy"
        // if specified, obtain secrets
        createNodeSecretsManifest(config)
        // prepare docker-compose file (and secrets)
        createNodeCfComposeDeployEnv(config, './deploy.json')
        // do stuff in the Firebase container
        sh(script: "docker-compose -f deploy.json up --no-start")
        sh(script: "docker-compose -f deploy.json run main firebase ${config.envDetails.deployCmd} --project=${config.envDetails.gcpProjectId}")
      }
      // end of Deploy stage

     } catch(err) {
       currentBuild.result = "FAILURE"
       println(err.toString());
       println(err.getMessage());
       println(err.getStackTrace());
       throw err
     } finally {
       // remove build and deploy containers
       if(fileExists('build.json')) { sh(script: 'docker-compose -f build.json rm -s -f') }
       if(fileExists('deploy.json')) { sh(script: 'docker-compose -f deploy.json rm -s -f') }

       if(!config.debugMode) {
         sh(script: 'rm -rf ./build.json')
         sh(script: 'rm -rf ./deploy.json')
         sh(script: 'rm -rf ./secrets')
         sh(script: 'rm -rf ./secrets.json')
         sh(script: 'rm -rf ./values.json')
         sh(script: 'rm -rf ./secrets-test.json')
         sh(script: 'rm -rf ./secrets-deployment.json')
       }
     }
  }

}
