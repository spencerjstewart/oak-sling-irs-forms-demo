# Use the Sling base image with Maven and Git installed
FROM docker.io/eclipse-temurin:17

USER root

# Install Maven and Git
RUN apt-get update && apt-get install -y maven git && rm -rf /var/lib/apt/lists/*

# Expose port 8080 for Sling
EXPOSE 8080

# Clone the Sling Starter app
RUN git clone https://github.com/apache/sling-org-apache-sling-starter.git /app/sling-starter

# Clone your GitHub repository
RUN git clone https://github.com/spencerjstewart/oak-sling-irs-forms-demo.git /app/oak-sling-irs-forms-demo

# Set the working directory to the sling starter root
WORKDIR /app/sling-starter

# Build the Sling Starter project
RUN mvn clean install

# Set the working directory to our project root
WORKDIR /app/oak-sling-irs-forms-demo

# Build your project (without installing it to Sling yet)
RUN mvn clean install

# Entry point to start Sling and then install your project
ENTRYPOINT ["/bin/bash", "-c", "\
  /app/sling-starter/target/dependency/org.apache.sling.feature.launcher/bin/launcher -f /app/sling-starter/target/slingfeature-tmp/feature-oak_tar.json & \
  echo 'Waiting for Sling to start...' && \
  sleep 20 && \
  cd /app/oak-sling-irs-forms-demo && \
  mvn clean install sling:install && \
  tail -f /dev/null"]