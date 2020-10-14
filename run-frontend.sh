#!/usr/bin/env bash

blue=`tput setaf 4`
bold=`tput bold`
reset=`tput sgr0`

set -e

printf "\n%s\n" "${blue}${bold}----------------------Starting frontend----------------------${reset}"
npm run --prefix static docker

printf "\n%s\n" "${blue}${bold}-----------------------------Done------------------------------${reset}"
