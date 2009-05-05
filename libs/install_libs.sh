#!/bin/bash
mvn install:install-file \
-DgroupId=quartz \
-DartifactId=quartz \
-Dversion=1.6.1 \
-Dfile=quartz-1.6.1.jar \
-Dpackaging=jar

mvn install:install-file \
-DgroupId=glowacki \
-DartifactId=glowacki \
-Dversion=0.1 \
-Dfile=glowacki-0.1.jar \
-Dpackaging=jar


if [ ! -f "ojdbc5.jar" ]
then
  echo "please download the oracle ojdbc5.jar from the oracle website and place it in this folder, then rerun this script."
  echo "Use version 11.1.0.7.0 unless you have a reason not to"
  echo "http://www.oracle.com/technology/software/tech/java/sqlj_jdbc/htdocs/jdbc_111060.html"

else
  mvn install:install-file \
    -DgroupId=com.oracle \
    -DartifactId=ojdbc \
    -Dversion=11.1.0.7.0 \
    -Dfile=ojdbc5.jar \
    -Dpackaging=jar
fi
