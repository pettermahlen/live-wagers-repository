#!/bin/bash

CATALOGUE_JAR=$1
VOLTDB_PID=target/voltdb.pid
VOLTDB_ERR_LOG=target/voltdb-err.log
VOLTDB_LOG=target/voltdb-stdout.log

export LOG4J_CONFIG_PATH=`pwd`/src/test/resources/log4j.xml

voltdb create catalog $CATALOGUE_JAR deployment src/test/resources/deployment.xml host localhost 2>$VOLTDB_ERR_LOG 1>$VOLTDB_LOG  &

echo $! > $VOLTDB_PID

# wait for volt to come up
for i in 1 2 3 4 5 6 7 8 9 10
do
    RESPONSE=`curl -s http://localhost:8082/api/1.0/?Procedure=@SystemInformation`

    EXIT_CODE=$?
    if [ "$EXIT_CODE" == "0" ]; then
        echo $RESPONSE

        if [[ "$RESPONSE" =~ .*status.:1,.* ]]; then
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