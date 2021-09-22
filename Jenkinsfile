/**
  Licensed to the Indoqa Software Design und Beratung GmbH (Indoqa) under
  one or more contributor license agreements. See the NOTICE file distributed
  with this work for additional information regarding copyright ownership.
  Indoqa licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License. You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
**/
pipeline {
  
  agent any

  parameters {
      booleanParam(name: 'DEPLOY_ARTIFACTS', defaultValue: false, description: 'Deploy artifacts to nexus')
      booleanParam(name: 'RUN_SONAR', defaultValue: false, description: 'Run sonar analysis')
  }

  environment {
    MAVEN_BUILD_PROPERTIES=''
    DEPLOY_BRANCH='master'
  }

  triggers {
    snapshotDependencies()
    parameterizedCron '15 23 * * * % DEPLOY_ARTIFACTS=true;RUN_SONAR=true;CRON=true'
  }

  tools {
    maven 'Maven 3.5.x' 
    jdk 'J2SDK 1.8'
  }

  stages {
    stage('Build') {
      options {
        timeout(time: 5, unit: 'MINUTES') 
      }    
      steps {
        sh 'mvn clean install -Ptest-coverage,indoqa-release ${MAVEN_BUILD_PROPERTIES}'
      }
    }

    stage('SonarQube analysis') { 
      when {
        environment name: 'RUN_SONAR', value: 'true'
      }

      steps {
        withSonarQubeEnv('sonar') { 
          sh 'mvn -Ptest-coverage,indoqa-release sonar:sonar -Dsonar.host.url=$SONAR_HOST_URL -Dsonar.login=$SONAR_AUTH_TOKEN -Dsonar.password= '
        }
      }
    }

    stage('Deploy to nexus') {
      when {
        allOf {
          branch "${DEPLOY_BRANCH}"
          environment name: 'DEPLOY_ARTIFACTS', value: 'true'
        }
      }
      
      steps {
        sh 'mvn deploy ${DEPLOY_SETTINGS} ${MAVEN_BUILD_PROPERTIES}'
      }
    }
  }

  post {
    changed {
      echo "Changed to ${currentBuild.result}"
      script {
        if(currentBuild.resultIsBetterOrEqualTo('SUCCESS')) {
          slackSend channel: '#ci_oss', color: '#008000', tokenCredentialId: 'Slack_IntegrationToken', message: "${env.JOB_NAME} has recovered at ${env.BUILD_NUMBER} status: ${currentBuild.currentResult} (<${env.BUILD_URL}|Open>)"
        }
        if(currentBuild.resultIsWorseOrEqualTo('UNSTABLE')) {
          slackSend channel: '#ci_oss', color: '#800000', tokenCredentialId: 'Slack_IntegrationToken', message: "${env.JOB_NAME} has failed at ${env.BUILD_NUMBER} status: ${currentBuild.currentResult} (<${env.BUILD_URL}|Open>)"
        }
      }
    }
  }
}

