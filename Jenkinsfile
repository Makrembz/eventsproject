pipeline {
    agent any
  triggers {
    githubPush()
  }
    tools {
        // Project uses Spring Boot 3.x and compiles with Java 21 locally.
        // Update this tool name to a JDK installation available in your Jenkins (e.g. 'JDK21').
        // If your Jenkins only provides a different name for JDK 21 (or 17), change it accordingly.
        jdk 'JDK21'
        maven 'Maven3'
    }

    environment {
        // Replace the placeholder below with your SonarQube server URL or set this value
        // in Jenkins global environment variables. Example: 'https://sonar.mycompany.com'
        SONAR_HOST_URL = 'http://localhost:9000'
        
        // Nexus configuration
        NEXUS_URL = 'http://localhost:8081'
        NEXUS_REPOSITORY = 'maven-releases'
        NEXUS_USER = 'admin'
        NEXUS_PASS = 'admin'

        // Docker configuration
        DOCKER_IMAGE = 'makrembz/eventsproject'
        DOCKER_TAG = "${BUILD_NUMBER}"
        
        // Auto-incrementing version: 1.0.BUILD_NUMBER
        RELEASE_VERSION = "1.0.${BUILD_NUMBER}"
    }

    stages {
        stage('Print env / tool info (debug)') {
            steps {
                echo "=== Debug: Java & Maven versions and environment ==="
                sh 'java -version'
                sh 'javac -version'
                sh 'mvn -v'
                sh 'printenv'
            }
        }

        stage('Checkout') {
            steps {
                git branch: 'master', url: 'https://github.com/Makrembz/eventsproject.git'
            }
        }

        stage('Build with Maven') {
            steps {
                sh 'mvn -B clean compile'
            }
        }

        stage('Unit Tests (with mocks)') {
            steps {
                sh 'mvn -B test'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                    // Archive JaCoCo coverage reports (HTML and XML)
                    // Note: Install the JaCoCo Jenkins plugin to enable the 'jacoco' step for interactive coverage reports
                    sh 'mvn jacoco:report || true'
                    archiveArtifacts artifacts: 'target/site/jacoco/**/*', allowEmptyArchive: true
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                // Bind a secret text credential (create a Secret Text credential in Jenkins with id 'SonarQube_jenkins')
                withCredentials([string(credentialsId: 'SonarQube_jenkins', variable: 'SONAR_TOKEN')]) {
                    sh '''#!/bin/bash
set -euo pipefail

echo "Running Maven Sonar analysis..."
mvn -B sonar:sonar \
    -Dsonar.host.url=${SONAR_HOST_URL} \
    -Dsonar.login=${SONAR_TOKEN} \
    -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml 2>&1 | tee sonar-output.txt

# Extract the CE task URL from scanner output
TASK_ID=$(grep -o "api/ce/task?id=[A-Za-z0-9_-]*" sonar-output.txt | head -n1 | cut -d= -f2) || true

if [ -z "${TASK_ID}" ]; then
    echo "ERROR: Could not find Sonar CE task id in mvn output."
    cat sonar-output.txt
    exit 1
fi
echo "Found CE task id: ${TASK_ID}"

# Remove trailing slash from SONAR_HOST_URL if present
SONAR_HOST="${SONAR_HOST_URL%/}"

# Poll CE task until it is finished (max ~2 minutes)
MAX_ATTEMPTS=40
ATTEMPT=0
CE_STATUS=""

while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
    ATTEMPT=$((ATTEMPT + 1))
    CE_JSON=$(curl -s -u "${SONAR_TOKEN}:" "${SONAR_HOST}/api/ce/task?id=${TASK_ID}")
    
    # Extract status using grep and cut (no backslashes needed)
    CE_STATUS=$(echo "$CE_JSON" | grep -o '"status":"[^"]*"' | head -n1 | cut -d: -f2 | tr -d '"')
    echo "CE status (attempt ${ATTEMPT}/${MAX_ATTEMPTS}): ${CE_STATUS}"
    
    if [ "${CE_STATUS}" = "SUCCESS" ]; then
        break
    fi
    if [ "${CE_STATUS}" = "FAILED" ] || [ "${CE_STATUS}" = "CANCELED" ]; then
        echo "ERROR: CE task finished with status ${CE_STATUS}"
        echo "$CE_JSON"
        exit 1
    fi
    sleep 3
done

if [ "${CE_STATUS}" != "SUCCESS" ]; then
    echo "ERROR: Timed out waiting for Sonar analysis to complete"
    exit 1
fi

# Extract analysisId using grep and cut
ANALYSIS_ID=$(echo "$CE_JSON" | grep -o '"analysisId":"[^"]*"' | head -n1 | cut -d: -f2 | tr -d '"')
if [ -z "${ANALYSIS_ID}" ]; then
    echo "ERROR: no analysisId available from CE task response"
    echo "$CE_JSON"
    exit 1
fi
echo "Analysis id: ${ANALYSIS_ID}"

# Query quality gate for the analysis
QG_JSON=$(curl -s -u "${SONAR_TOKEN}:" "${SONAR_HOST}/api/qualitygates/project_status?analysisId=${ANALYSIS_ID}")
QG_STATUS=$(echo "$QG_JSON" | grep -o '"status":"[^"]*"' | head -n1 | cut -d: -f2 | tr -d '"')
echo "Quality gate status: ${QG_STATUS}"

if [ "${QG_STATUS}" != "OK" ]; then
    echo "Quality Gate did not pass: ${QG_STATUS}"
    echo "$QG_JSON"
    
    # Get detailed issues information
    echo ""
    echo "=== Fetching detailed issues ==="
    ISSUES_JSON=$(curl -s -u "${SONAR_TOKEN}:" "${SONAR_HOST}/api/issues/search?componentKeys=tn.esprit:eventsProject&resolved=false&ps=20")
    echo "$ISSUES_JSON" | python3 -c "import sys,json; d=json.load(sys.stdin); [print(f'- {i.get(\"component\",\"?\").split(\":\")[-1]}:{i.get(\"line\",\"?\")} - {i.get(\"message\",\"?\")}') for i in d.get('issues',[])]" 2>/dev/null || echo "$ISSUES_JSON"
    
    exit 1
fi
echo "Quality Gate passed!"
'''
                }
            }
        }

        stage('Package') {
            steps {
                sh 'mvn -B package -DskipTests'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                }
            }
        }

        stage('Deploy to Nexus') {
            steps {
                echo "Deploying artifact to Nexus repository..."
                sh '''#!/bin/bash
set -euo pipefail

# Extract artifact info from pom.xml
GROUP_ID=$(mvn help:evaluate -Dexpression=project.groupId -q -DforceStdout)
ARTIFACT_ID=$(mvn help:evaluate -Dexpression=project.artifactId -q -DforceStdout)
PACKAGING=$(mvn help:evaluate -Dexpression=project.packaging -q -DforceStdout)

# Use auto-incrementing version from Jenkins build number
VERSION="${RELEASE_VERSION}"

# Find the JAR file
JAR_FILE=$(ls target/*.jar | grep -v original | head -n1)

echo "Deploying ${GROUP_ID}:${ARTIFACT_ID}:${VERSION} to Nexus (${NEXUS_REPOSITORY})..."

# Convert groupId dots to slashes for Nexus path
GROUP_PATH=$(echo "${GROUP_ID}" | tr '.' '/')

# Deploy to Nexus using curl (fail on HTTP errors)
HTTP_CODE=$(curl -s -o /tmp/nexus-response.txt -w "%{http_code}" \
    -u "${NEXUS_USER}:${NEXUS_PASS}" \
    --upload-file "${JAR_FILE}" \
    "${NEXUS_URL}/repository/${NEXUS_REPOSITORY}/${GROUP_PATH}/${ARTIFACT_ID}/${VERSION}/${ARTIFACT_ID}-${VERSION}.${PACKAGING}")

if [ "$HTTP_CODE" -ge 200 ] && [ "$HTTP_CODE" -lt 300 ]; then
    echo "Artifact deployed successfully to Nexus! (HTTP $HTTP_CODE)"
else
    echo "ERROR: Failed to deploy to Nexus (HTTP $HTTP_CODE)"
    cat /tmp/nexus-response.txt
    exit 1
fi
'''
            }
        }

        stage('Build Docker Image') {
            steps {
                echo "Building Docker image..."
                sh """
                    docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} .
                    docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest
                """
            }
        }

        stage('Push to DockerHub') {
            steps {
                echo "Pushing Docker image to DockerHub..."
                withCredentials([usernamePassword(credentialsId: 'dockerhub-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    sh '''#!/bin/bash
set -euo pipefail

echo "${DOCKER_PASS}" | docker login -u "${DOCKER_USER}" --password-stdin

docker push ${DOCKER_IMAGE}:${DOCKER_TAG}

docker logout

echo "Docker images pushed successfully!"
'''
                }
            }
        }

        stage('Deploy with Docker Compose') {
            steps {
                echo "Deploying application with Docker Compose..."
                sh '''#!/bin/bash
set -euo pipefail

# Stop and remove existing containers if any
docker-compose down --remove-orphans || true

# Pull latest images and start all services
export DOCKER_IMAGE=${DOCKER_IMAGE}
export DOCKER_TAG=${DOCKER_TAG}

docker-compose up -d

# Wait for services to be healthy
echo "Waiting for services to start..."
sleep 30

# Check if all containers are running
echo "=== Container Status ==="
docker-compose ps

# Verify application health
APP_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8089/events/actuator/health || echo "000")
if [ "$APP_HEALTH" = "200" ]; then
    echo "✓ Spring Boot application is healthy!"
else
    echo "⚠ Spring Boot application health check returned: $APP_HEALTH"
fi

# Verify Prometheus is running
PROM_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:9091/-/healthy || echo "000")
if [ "$PROM_HEALTH" = "200" ]; then
    echo "✓ Prometheus is healthy!"
else
    echo "⚠ Prometheus health check returned: $PROM_HEALTH"
fi

# Verify Grafana is running
GRAFANA_HEALTH=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:3000/api/health || echo "000")
if [ "$GRAFANA_HEALTH" = "200" ]; then
    echo "✓ Grafana is healthy!"
else
    echo "⚠ Grafana health check returned: $GRAFANA_HEALTH"
fi

echo ""
echo "=== Deployment Complete ==="
echo "Application URL: http://localhost:8089/events"
echo "Prometheus URL: http://localhost:9091"
echo "Grafana URL: http://localhost:3000 (admin/admin)"
echo "cAdvisor URL: http://localhost:8081"
'''
            }
        }
    }

    post {
        success {
            echo '''
=== Pipeline Completed Successfully! ===
All stages passed. Your application is now deployed with monitoring.

Access points:
- Application: http://localhost:8089/events
- Prometheus: http://localhost:9091
- Grafana: http://localhost:3000 (admin/admin)
- cAdvisor: http://localhost:8081
'''
        }
        failure {
            echo 'Pipeline failed! Check the logs for details.'
        }
        always {
            // Clean up workspace but keep docker-compose running
            cleanWs(cleanWhenNotBuilt: false,
                    deleteDirs: true,
                    disableDeferredWipeout: true,
                    notFailBuild: true)
        }
    }
}
