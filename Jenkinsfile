pipeline {
    agent any

    environment {
        IMAGE_NAME = "lab1-java-maven"
        IMAGE_TAG  = "1.0"
        COMPOSE_PROJECT_DIR = "."
    }

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build Maven App') {
            steps {
                sh '''
                    mvn clean package -DskipTests
                '''
            }
        }

        stage('Run Unit Tests') {
            steps {
                sh '''
                    mvn test
                '''
            }
            post {
                always {
                    junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                sh '''
                    docker build -t ${IMAGE_NAME}:${IMAGE_TAG} .
                '''
            }
        }

        stage('Stop Existing Containers') {
            steps {
                sh '''
                    docker compose down || true
                '''
            }
        }

        stage('Deploy with Docker Compose') {
            steps {
                sh '''
                    docker compose up -d
                '''
            }
        }

        stage('Verify Deployment') {
            steps {
                sh '''
                    echo "Waiting for containers to report healthy..."
                    for i in $(seq 1 15); do
                        APP_STATUS=$(docker inspect -f '{{.State.Health.Status}}' lab1-app 2>/dev/null || echo "starting")
                        DB_STATUS=$(docker inspect -f '{{.State.Health.Status}}' lab1-db 2>/dev/null || echo "starting")
                        echo "app=$APP_STATUS db=$DB_STATUS"
                        if [ "$APP_STATUS" = "healthy" ] && [ "$DB_STATUS" = "healthy" ]; then
                            echo "Both containers healthy."
                            break
                        fi
                        sleep 5
                    done

                    docker compose ps

                    if [ "$APP_STATUS" != "healthy" ] || [ "$DB_STATUS" != "healthy" ]; then
                        echo "Containers did not become healthy in time."
                        exit 1
                    fi

                    curl -sf http://localhost:8080/api/health || (echo "Health check failed" && exit 1)
                '''
            }
        }
    }

    post {
        success {
            echo "Build and deployment completed successfully."
        }
        failure {
            echo "Build or deployment failed. Dumping logs..."
            sh 'docker compose logs --tail=100 || true'
        }
        always {
            sh 'docker image prune -f || true'
        }
    }
}
