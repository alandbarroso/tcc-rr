#! /bin/bash

THE_CLASSPATH=
for i in `ls ./Simuladores/roborescue-2013/lib/*.jar`
do
  THE_CLASSPATH=${THE_CLASSPATH}:${i}
done
for i in `ls ./Simuladores/roborescue-2013/jars/*.jar`
do
  THE_CLASSPATH=${THE_CLASSPATH}:${i}
done

THE_CLASSPATH=${THE_CLASSPATH}:./bin

javaArgs="-cp $THE_CLASSPATH"

launchClass="lti.LaunchLTIAgents"

launchArgs="$1 $2 $3 $4 $5 $6 $7 -v"

all="$javaArgs $launchClass $launchArgs"

java $all
