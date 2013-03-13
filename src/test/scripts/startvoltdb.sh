#!/bin/bash

CATALOGUE_JAR=$1
VOLTDB_PID=target/voltdb.pid
VOLTDB_ERR_LOG=target/voltdb-err.log
VOLTDB_LOG=target/voltdb-stdout.log


voltdb create catalog $CATALOGUE_JAR deployment src/test/resources/deployment.xml host localhost 2>$VOLTDB_ERR_LOG 1>$VOLTDB_LOG  &

echo $! > $VOLTDB_PID

exit 0