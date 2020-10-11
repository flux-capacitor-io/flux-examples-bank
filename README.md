# Flux Capacitor bank
Basic example of a banking application that uses Flux Capacitor.

## Run in-memory

To create a new app just create a new repository from this template and change the maven groupId to something else.

To run locally start AppMain.java or create a docker-compose file.
Use -DdevMode=true to run in development mode, meaning for one thing that jwt tokens will be parsed but not verified.

To run remotely first build the application:
- execute mvn install
- build the Dockerfile in /app.
- deploy to a platform of choice. Make sure you set environment variables for:
    - MESSAGING_ENDPOINT, e.g. `https://flux-service.example.com`
    - DOCKER_ENVIRONMENT (defaults to `dev`), e.g. `production`
    - ENCRYPTION_KEY (optional, encrypted properties are not supported if missing), e.g. `jhfdskjfhuwe74y837ghf3u7g327gf`

You can add application properties in two ways:
- add global properties to `app/config/application.properties.tmpl` or `adapter/config/application.properties.tmpl`
- add environment specific properties to `app/config/env.<YOUR_ENV>.properties` or `adapter/config/env.<YOUR_ENV>.properties`

That's about it!

## To-do's when starting new project

- change groupId in all pom.xml's
- in deploy-app.yml, change "replacewithmaster" with "master"
- in common/pom.xml, change typescript generator target class "app-base" to the location of your models
