# Flux Bank

This repository showcases a fundamental example of a banking application leveraging Flux technology. The application
proficiently manages bank accounts and their transactions. Without relying on a database, it utilizes Flux to store
events and messages, and bank accounts are either event-sourced or loaded from a local cache as needed.

## Features

- **Money Transfer with Autocorrect Mechanism:** The application demonstrates a secure money transfer process between
  two accounts. It incorporates a side-effect-free autocorrecting mechanism to address any failures during the transfer.

- **Automated Account Lifecycle Management:** To showcase message scheduling, the repository includes a component
  responsible for managing the lifecycle of bank accounts. Inactive accounts are automatically closed after a predefined
  duration.

For detailed information about Flux and its features, visit [flux.host](https://flux.host) or explore
the [GitHub repository](https://github.com/flux-capacitor-io/flux-capacitor-client) of Flux Client.

---

## Prerequisites

Before running the sample project, ensure that you have the following installed on your local machine:

- [Java](https://openjdk.java.net/install/) version 21 or later
- [Maven](https://maven.apache.org)
- [Docker](https://docs.docker.com/get-docker/) and [Docker-Compose](https://docs.docker.com/compose/install/)
- [Node.js](https://docs.npmjs.com/downloading-and-installing-node-js-and-npm) for the frontend

## Running the Sample

To execute the sample, start the following applications using either docker-compose or IntelliJ IDEA (see instructions
below):

- Flux Test server instance
- Flux Proxy server instance at `http://localhost:8080`
- Banking app instance
- Angular dev server serving the UI at `http://localhost:4200`

### Using IntelliJ IDEA (Preferred)

To run all applications, including the Angular dev server for the frontend UI, use the
run-configuration: `Backend + UI`. To run only the backend applications, execute `Backend`.

### Using docker-compose

Launch the application using docker-compose by running `./run.sh`. This script performs a Maven build, builds and
launches the Docker containers configured in `docker-compose.yml`, and initiates the frontend.

For subsequent runs, it's recommended to execute `./run-backend.sh` and/or `./run-frontend.sh` as they launch more
quickly.

## Sample UI

Access a (very basic) banking client from a browser by visiting [http://localhost:4200/](http://localhost:4200/).

Upon reaching the page, you will be prompted to log in or create an account. Click `Create account` and choose a name
for the account to proceed.

Subsequently, you'll be directed to an overview screen for your bank account, where you can deposit or transfer money to
another account and view your transactions.
