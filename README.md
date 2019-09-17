# Jenkins pipeline library for Nodejs deployment (@ GCP Cloud Functions)

## Features

- Secrets injection (done via [Environment Variables](https://cloud.google.com/functions/docs/env-var)) from Vault secure store

## Example Jenkinsfile for _Pipeline_ mode

```groovy
PipelineNodeCf{
  pipelineMode = 'Pipeline'
  gitlabCredentialsId = 'gitlab01-credentials'
  sshCredentialsId = 'ssh-private-key-01'
  repositoryUrl = 'git@git-server.co.uk/repo'
  branch = 'master'
  gcpProjectId = 'my-project-12345'
}
```

## Example Jenkinsfile for _MultibranchPipeline_ mode

```groovy
PipelineNodeCf{
  pipelineMode = 'MultibranchPipeline'
  gitlabCredentialsId = 'gitlab01-credentials'
  sshCredentialsId = 'ssh-private-key-01'

  branchEnvs = [
    master: [
      gcpProjectId: 'my-project-12345',
    ]
  ]
}
```

## Configuration options

**Always top-level**
- `pipelineMode` *Pipeline* or *MultibranchPipeline*
- `gitlabCredentialsId` e.g. *gitlab01-credentials*
- `sshCredentialsId` e.g. *ssh-private-key-01*

**Only for Simple pipeline mode; always top-level**
- `repositoryUrl` e.g. *git@git-server.co.uk/repo*
- `branch` e.g. *master*

**Top-level for Simple pipeline mode, nested for MultibranchPipeline mode**
- `gcpProjectId` e.g. *my-project-12345*
