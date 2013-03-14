#!/bin/bash

CATALOGUE_JAR=$1
VOLTDB_PID=target/voltdb.pid
VOLTDB_ERR_LOG=target/voltdb-err.log
VOLTDB_LOG=target/voltdb-stdout.log

export LOG4J_CONFIG_PATH=`pwd`/src/test/resources/log4j.xml

# check if there are previously running volt processes, and kill them if so
ORPHANED_VOLTDBS=`ps aux | grep volt | grep -v grep | grep -v startvoltdb.sh | awk '{ print $2 }'`

if [ ! -z "$ORPHANED_VOLTDBS" ]; then
   echo "Found orphaned voltdb pid: $ORPHANED_VOLTDBS, killing them"
   kill $ORPHANED_VOLTDBS
   # give volt time to die
   sleep 2
fi
voltdb create catalog $CATALOGUE_JAR deployment src/test/resources/deployment.xml host localhost 2>$VOLTDB_ERR_LOG 1>$VOLTDB_LOG  &

PID=$!

echo $PID > $VOLTDB_PID

# wait for volt to come up
for i in 1 2 3 4 5 6 7 8 9 10
do
    RESPONSE=`curl -s http://localhost:8082/api/1.0/?Procedure=@SystemInformation`

    EXIT_CODE=$?
    if [ "$EXIT_CODE" == "0" ]; then
        if [[ "$RESPONSE" =~ .*status.:1,.* ]]; then
            echo Successfully started voltdb as pid $PID
            exit 0
        else
            # Status is not 'success' - pretend we couldn't connect to the port at all and fallthrough to the other sleep..
            EXIT_CODE=7
        fi
    fi

    if [ "$EXIT_CODE" == "7" ]; then
        sleep 1
    else
        echo "Curl responded with exit code $EXIT_CODE"
        exit 1
    fi
done

exit 1