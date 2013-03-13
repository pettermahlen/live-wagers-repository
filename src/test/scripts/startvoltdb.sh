#!/bin/bash

CATALOGUE_JAR=$1
VOLTDB_PID=target/voltdb.pid
VOLTDB_ERR_LOG=target/voltdb-err.log
VOLTDB_LOG=target/voltdb-stdout.log

export LOG4J_CONFIG_PATH=`pwd`/src/test/resources/log4j.xml

voltdb create catalog $CATALOGUE_JAR deployment src/test/resources/deployment.xml host localhost 2>$VOLTDB_ERR_LOG 1>$VOLTDB_LOG  &

echo $! > $VOLTDB_PID

exit 0