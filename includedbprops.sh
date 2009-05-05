#!/bin/bash

echo "configuring harvester in deployment folder for NLA devel oracle usage"

REPLACEMENTS="s/\${oracle\.url}/DB CONNECTION STRING HERE/g
s/\${oracle\.username}/USERNAME HERE/g
s/\${oracle\.password}/PASSWORD HERE/g
s/\${smtp\.gateway}/SMTP GATEWAY HERE/g"

sed "$REPLACEMENTS" deployment/HarvesterClient/WEB-INF/classes/HarvesterClient.properties > HCProptmp
sed "$REPLACEMENTS" deployment/HarvesterProcessor/WEB-INF/classes/HarvesterProcessor.properties > HPProptmp
sed "$REPLACEMENTS" deployment/HarvesterProcessor/WEB-INF/classes/hibernate.cfg.xml > HPHibtmp
sed "$REPLACEMENTS" deployment/Scheduler/WEB-INF/classes/quartz.properties > Squartztmp

mv HCProptmp deployment/HarvesterClient/WEB-INF/classes/HarvesterClient.properties
mv HPProptmp deployment/HarvesterProcessor/WEB-INF/classes/HarvesterProcessor.properties
mv HPHibtmp deployment/HarvesterProcessor/WEB-INF/classes//hibernate.cfg.xml
mv Squartztmp deployment/Scheduler/WEB-INF/classes/quartz.properties

