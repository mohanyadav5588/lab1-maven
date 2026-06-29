pipeline {
    agent any

    environment {
        APP_NAME    = "lab1-status-app"
        IMAGE_NAME  = "lab1-java-maven"
        IMAGE_TAG   = "1.0"
        HEALTH_URL  = "http://localhost:8080/api/health"
    }

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Maven Build') {
            steps {
                sh 'mvn clean package -DskipTests -B'
            }
        }

        stage('Maven Test') {
            steps {
                sh 'mvn test -B'
            }
            post {
                always {
                    junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                sh "docker build -t ${IMAGE_NAME}:${IMAGE_TAG} ."
            }
        }

        stage('Stop Existing Stack') {
            steps {
                sh 'docker compose down || true'
            }
        }

        stage('Deploy Stack') {
            steps {
                sh 'docker compose up -d'
            }
        }

        stage('Wait for Healthy Containers') {
            steps {
                sh '''
                    echo "Waiting for lab1-db and lab1-app to report healthy..."
                    for i in $(seq 1 15); do
                        DB_STATUS=$(docker inspect -f '{{.State.Health.Status}}' lab1-db 2>/dev/null || echo "starting")
                        APP_STATUS=$(docker inspect -f '{{.State.Health.Status}}' lab1-app 2>/dev/null || echo "starting")
                        echo "db=$DB_STATUS app=$APP_STATUS"

                        if [ "$DB_STATUS" = "healthy" ] && [ "$APP_STATUS" = "healthy" ]; then
                            echo "Both containers healthy."
                            exit 0
                        fi
                        sleep 5
                    done

                    echo "Containers did not become healthy in time."
                    docker compose logs --tail=50
                    exit 1
                '''
            }
        }

        stage('Smoke Test') {
            steps {
                sh "curl -sf ${HEALTH_URL} || (echo 'Health check failed' && exit 1)"
            }
        }
    }

    post {
        success {
            echo "${APP_NAME} built and deployed successfully (image: ${IMAGE_NAME}:${IMAGE_TAG})."
        }
        failure {
            echo "Pipeline failed — dumping container logs for diagnosis."
            sh 'docker compose ps || true'
            sh 'docker compose logs --tail=100 || true'
        }
        always {
            sh 'docker image prune -f || true'
        }
    }
}
