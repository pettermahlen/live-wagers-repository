
The live-wagers-repository depends on:
* Java
* Maven
* VoltDB
* Lots of jars that are centrally available
* Some jars that are not available in the central Maven repository

To build, you first need to get Java and Maven installed. (See Google for how to do that). Once that's done, you need to get VoltDB
installed. See http://voltdb.com/community/learning-resources.php for a lot about that. A brief and unsatisfactory set of steps might be:

1. Download VoltDB 3.0 from http://voltdb.com/community/downloads.php
2. Unzip the archive, and place it somewhere (this will be referred to VOLTDB_HOME)
3. Define the VOLTDB_HOME environment variable in your system (using .profile or Windows settings, or what works best).
4. Add $VOLTDB_HOME/bin to your path.

Then, you need to install the following jars locally:
1. org.voltdb:voltdb:jar:3.0
1.1. Clone the https://github.com/pettermahlen/voltdb-pom git repo and 'cd' into that directory
1.2. run 'mvn install:install-file -Dfile=$VOLTDB_HOME/voltdb/voltdb-3.0.jar -DpomFile=voltdb-3.0.pom
2. org.zeromq:zmq:jar:2.1.11
2.1. 'cd' into the $VOLTDB_HOME/lib directory
2.2. run `mvn install:install-file -Dfile=zmq-2.1.11.jar -DgroupId=org.zeromq -DartifactId=zmq -Dpackaging=jar -Dversion=2.1.11`

Once this is all done, you should be able to build using `mvn package`.

# Running

1. Build using `mvn package`
2. Run using `./src/test/scripts/startvoltdb.sh target/<artifact-id>`

The `<artifact-id>` will generally be something like `live-wagers-repository-1.0-SNAPSHOT.jar`.

