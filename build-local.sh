#!/bin/bash
cd scripts
echo "entering `pwd`"
sh compileandcopytodeploymentfolder
if [ "$?" -ne 0 ]; then
  exit 1
fi
cd ..
echo "entering `pwd`"
sh includedbpropsNLA.sh 
