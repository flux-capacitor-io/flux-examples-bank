#!/usr/bin/env bash

blue=`tput setaf 4`
bold=`tput bold`
reset=`tput sgr0`

set -e

printf "\n%s\n" "${blue}${bold}----------------------Starting frontend----------------------${reset}"
npm install --prefix static
npm run --prefix static start

printf "\n%s\n" "${blue}${bold}-----------------------------Done------------------------------${reset}"
