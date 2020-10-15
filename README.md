# Flux Capacitor bank
Basic example of a banking application that uses Flux Capacitor.

## What it does
The application manages bank accounts and their transactions. Other than events and messages stored in Flux Capacitor
the application needs no database. Bank accounts are event sourced anytime they're needed.

The application shows how you can safely transfer money between 2 accounts, with a side-effect free
 autocorrecting mechanism if the transfer should fail.

 To demonstrate scheduling of messages it also includes a component that manages the lifecycle of bank accounts.
 New accounts that are inactive for a long time are automatically closed.

 For more information about the features of Flux Capacitor check out:
 <https://github.com/flux-capacitor-io/flux-capacitor-client>.

## The setup
A docker compose file configures the setup.
Once everything is started (see below) the following is running:
- 1 Flux Capacitor instance
- 2 instances of app
- 1 instance of web at `http://localhost:8090`
- Angular dev server at `http://localhost:4200`

The web instance's responsibilities are to forward commands and queries (posted to `/api/command` and `/api/query`
respectively) to Flux Capacitor. It also establishes the identity of the user (via JWT parsing).

All commands and queries are handled by the app instances, so business rules are applied only there.

## Build and run
Make sure you have the `ARTIFACTORY_ENCRYPTED_PASSWORD` set as an environment variable (e.g. in your `.bash_profile`).

    export ARTIFACTORY_ENCRYPTED_PASSWORD=xxxxxxx

Execute `./run.sh` for the initial run. This performs a Maven build, builds and launches the dockers configured in
`docker-compose.yml` and launches the frontend.

For later runs you're recommended to execute `./run-backend.sh` and / or `./run-frontend.sh` as they launch a little
faster.
