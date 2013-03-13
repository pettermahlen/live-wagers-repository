#!/bin/sh

echo "Stopping VoltDB"
cat target/voltdb.pid | xargs kill -9
exit 0
