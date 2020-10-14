#!/usr/bin/env bash

if [[ "$ARTIFACTORY_ENCRYPTED_PASSWORD" == "" ]]; then
  echo "Please set your ARTIFACTORY_ENCRYPTED_PASSWORD environment variable"
  echo "For example by \"export ARTIFACTORY_ENCRYPTED_PASSWORD=xxxxx\""
  exit 1
fi

blue=`tput setaf 4`
bold=`tput bold`
reset=`tput sgr0`

set -e

printf "\n%s\n" "${blue}${bold}----------------------Maven build----------------------${reset}"
mvn -q clean install -DskipTests -T 4

printf "\n%s\n" "${blue}${bold}----------------------Build Docker images----------------------${reset}"
docker-compose build bank-web bank-app
docker-compose up -d --scale bank-app=2 --remove-orphans

printf "\n%s\n" "${blue}${bold}-----------------------------Done------------------------------${reset}"
