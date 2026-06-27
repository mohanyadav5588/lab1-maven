pipeline {
    agent any

    environment {
        IMAGE_NAME    = "lab1-java-maven"
        IMAGE_TAG     = "${env.BUILD_NUMBER}"
        REGISTRY      = "" // e.g. "docker.io/yourusername" — leave blank to skip push
        DOCKER_CREDS  = "dockerhub-credentials" // Jenkins credentials ID, if pushing
        COMPOSE_FILE  = "docker-compose.yml"
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
        disableConcurrentBuilds()
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build (Maven)') {
            agent {
                docker {
                    image 'maven:3.9-eclipse-temurin-17'
                    args '-v $HOME/.m2:/root/.m2'
                }
            }
            steps {
                sh 'mvn -B -q clean compile'
            }
        }

        stage('Unit Tests') {
            agent {
                docker {
                    image 'maven:3.9-eclipse-temurin-17'
                    args '-v $HOME/.m2:/root/.m2'
                }
            }
            steps {
                sh 'mvn -B test'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Package Jar') {
            agent {
                docker {
                    image 'maven:3.9-eclipse-temurin-17'
                    args '-v $HOME/.m2:/root/.m2'
                }
            }
            steps {
                sh 'mvn -B -q package -DskipTests'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

        stage('Build Docker Image') {
            steps {
                sh "docker build -t ${IMAGE_NAME}:${IMAGE_TAG} -t ${IMAGE_NAME}:latest ."
            }
        }

        stage('Integration Test (Compose)') {
            steps {
                sh """
                    docker compose -f ${COMPOSE_FILE} down -v || true
                    docker compose -f ${COMPOSE_FILE} up -d --build
                """
                script {
                    sh '''
                        echo "Waiting for app health check..."
                        for i in $(seq 1 20); do
                          STATUS=$(docker inspect --format='{{.State.Health.Status}}' lab1-app 2>/dev/null || echo "starting")
                          if [ "$STATUS" = "healthy" ]; then
                            echo "App is healthy"
                            exit 0
                          fi
                          sleep 5
                        done
                        echo "App did not become healthy in time"
                        docker compose -f docker-compose.yml logs app
                        exit 1
                    '''
                }
                sh 'curl -f http://localhost:8080/api/status'
            }
            post {
                always {
                    sh "docker compose -f ${COMPOSE_FILE} down -v || true"
                }
            }
        }

        stage('Push Image') {
            when {
                expression { return env.REGISTRY?.trim() }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: "${DOCKER_CREDS}",
                                                   usernameVariable: 'DOCKER_USER',
                                                   passwordVariable: 'DOCKER_PASS')]) {
                    sh """
                        echo \$DOCKER_PASS | docker login -u \$DOCKER_USER --password-stdin
                        docker tag ${IMAGE_NAME}:${IMAGE_TAG} ${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}
                        docker tag ${IMAGE_NAME}:latest ${REGISTRY}/${IMAGE_NAME}:latest
                        docker push ${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}
                        docker push ${REGISTRY}/${IMAGE_NAME}:latest
                    """
                }
            }
        }
    }

    post {
        success {
            echo "✅ Pipeline succeeded — image ${IMAGE_NAME}:${IMAGE_TAG} built and verified."
        }
        failure {
            echo "❌ Pipeline failed. Check stage logs above."
        }
        always {
            sh 'docker system prune -f || true'
        }
    }
}
