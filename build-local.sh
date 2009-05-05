#!/bin/bash
cd scripts
echo "entering `pwd`"
sh compileandcopytodeploymentfolder
cd ..
echo "entering `pwd`"
sh includedbprops.sh 
