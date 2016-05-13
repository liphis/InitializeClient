#!/bin/bash
#------- SEQUENCE : Module Identification -------------------------------------
#
# Module Name      : infosys_install.sh
# Function         : infosystem installation for ajo training
# Usage            : on server vm-ajo and mobile servers
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
  echo "usage $0 <infosystem> <clients>" >&2
  echo "      * installs infosystem in all specified clients" >&2
  exit 1
}
#------------------------------------------------------------------------------
# install_infosys
#------------------------------------------------------------------------------
install_infosys()
{
  su -c "cd $1 && eval \$(sh denv.sh) && tar -xf is.OW1.$2.tgz && infosysimport.sh -p sy -w OW1 -s $2 && ajo_install.sh -c -I -q ow1/$2" $1
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
# main
#------------------------------------------------------------------------------
array=("${array[@]:1}")
for arg in "${array[@]}";
do
  install_infosys $arg $1
done
exit 0
