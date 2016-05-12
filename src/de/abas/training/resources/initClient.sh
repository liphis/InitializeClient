#!/bin/bash
#------- SEQUENCE : Module Identification -------------------------------------
#
# Module Name      : initClient.sh
# Function         : initialization of training clients for AJO basic and advanced trainings
# Usage	           : on server schulung and mobile servers
# Author           : jasc
# Responsible      : jasc
# Copyright        : (c) ABAS Software AG
#
#------------------------------------------------------------------------------
# usage
#------------------------------------------------------------------------------
usage()
{
  echo "" >&2
  echo "usage: $0 <training type> <host name> <clients>" >&2
  echo "       * training type: basic or advanced" >&2
  echo "       * host name: name of server (e.g. schulung)" >&2
  echo "       * clients: name of all clients to initialize separated by space" >&2
  exit 1
}
#------------------------------------------------------------------------------
# display usage
#------------------------------------------------------------------------------
array=("$@")
for arg in "${array[@]}";
do
  case $arg in
    -h) usage ;;
    --help) usage ;;
  esac
done
#------------------------------------------------------------------------------
# runs jar using java 7
#------------------------------------------------------------------------------
/usr/java/java7/bin/java -jar initClient.jar $*
