#!/bin/bash

#The following environment variables must be defined for this script to work:
# VOLTDB_HOME -> VoltDB 2.1+ Community Edition
# JAVA_HOME -> Java 1.6.0_21+

if [ -z $JAVA_HOME ]; then
  echo "JAVA_HOME must be set to start VoltDB!"
  exit -1
fi

if [ -z $VOLTDB_HOME ]; then
  echo "VOLTDB_HOME must be set!"
  exit -1
fi

if [ $# -lt 1 ]; then
  echo "Catalogue jar must be specified - defaults used for others:"
  echo "  catalogue jar: either use explicit path or version number => the UOR catalogue is looked up in the ~/.m2 repository"
  echo "  deployment.xml: ./src/test/resources/deployment.xml"
  echo "  leader: localhost"
  echo "  voltdb log: ./target/voltdb-stdout.txt"
  echo "  voltdb error log: ./target/voltdb-err.txt"
  echo "  voltdb pid: ./target/voltdb.pid"
  exit -1
fi

if [ $# == 7 ]; then
  if [[ "$7" =~ "log4j" ]]; then
    echo "Log4J has been specified."
    LOG4J_PARAM="-Dlog4j.configuration=$7"
  else
    echo "Clover has been specified."
    CLOVER=$7
  fi
fi

echo "Using JAVA_HOME $JAVA_HOME"
echo "Using VOLTDB_HOME $VOLTDB_HOME"


CATALOG=$1
DEPLOYMENT_XML=$2
LEADER=$3
VOLTDB_LOG=$4
VOLTDB_ERR_LOG=$5
VOLTDB_PID=$6

if [[ $CATALOG =~ ".jar" ]]; then
   echo "$CATALOG is valid"
else
   CATALOG=~/.m2/repository/com/shopzilla/inventory/imp/uor-catalogue/$1/uor-catalogue-$1.jar
fi

if [ -z "$DEPLOYMENT_XML" ]; then
   DEPLOYMENT_XML=./src/test/resources/deployment.xml
fi

if [ -z "$LEADER" ]; then
   LEADER=localhost
fi

if [ -z "$VOLTDB_LOG" ]; then
   VOLTDB_LOG=./target/voltdb-stdout.log
fi

if [ -z "$VOLTDB_ERR_LOG" ]; then
   VOLTDB_ERR_LOG=./target/voltdb-err.log
fi

if [ -z "$VOLTDB_PID" ]; then
   VOLTDB_PID=./target/voltdb.pid
fi


echo "Loading catalog $CATALOG"
echo "Using deployment configuration: $DEPLOYMENT_XML"
echo "Using leader: $LEADER"
echo "Using std log: $VOLTDB_LOG"
echo "Using std err log: $VOLTDB_ERR_LOG"
echo "Using pid file: $VOLTDB_PID"
echo "Using log4j param: $LOG4J_PARAM"

export CLASSPATH="./:$VOLTDB_HOME/lib/*:$VOLTDB_HOME/voltdb/*"

# If a clover version was passed, use it on the classpath. Second argument passed to this script.
if [ ! -z $CLOVER ]; then
    echo "Clover version passed: $CLOVER"
    export CLOVER_VERSION=$CLOVER
    export CLASSPATH="$CLASSPATH:$HOME/.m2/repository/com/atlassian/maven/plugins/maven-clover2-plugin/$CLOVER_VERSION/*:$HOME/.m2/repository/com/cenqua/clover/clover/$CLOVER_VERSION/*"
fi

ORPHANED_VOLTDB=`ps -ef | grep volt | awk '$3 == 1 { print $2 }'`

if [ ! -z "$ORPHANED_VOLTDB" ]; then
   echo "Found orphaned voltdb pid: $ORPHANED_VOLTDB, killing it"
   kill $ORPHANED_VOLTDB
   # give the previous instance some time to die
   sleep 2
fi

JAVA_OPTS="-Xmx256m -XX:MaxPermSize=256m"

${JAVA_HOME}/bin/java $JAVA_OPTS $LOG4J_PARAM -Djava.library.path="$VOLTDB_HOME/voltdb" -Dlog4j.configuration=file:src/main/resources/log4j.properties org.voltdb.VoltDB catalog $CATALOG deployment $DEPLOYMENT_XML leader $LEADER 2>$VOLTDB_ERR_LOG 1>$VOLTDB_LOG &

echo $! > $VOLTDB_PID

echo "Stdout log written to: $VOLTDB_LOG"
echo "Err log written to: $VOLTDB_ERR_LOG"

# Give VoltDB some time to start
sleep 10

echo "VoltDB has been started"

exit 0