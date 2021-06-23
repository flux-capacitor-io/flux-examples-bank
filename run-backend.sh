#!/usr/bin/env bash

blue=`tput setaf 4`
bold=`tput bold`
reset=`tput sgr0`

set -e

printf "\n%s\n" "${blue}${bold}----------------------Maven build----------------------${reset}"
mvn -q clean install -DskipTests -T 4

printf "\n%s\n" "${blue}${bold}----------------------Build Docker images----------------------${reset}"
docker-compose build web app
docker-compose up -d --scale app=2 --remove-orphans

printf "\n%s\n" "${blue}${bold}-----------------------------Done------------------------------${reset}"
