#!/usr/bin/env bash

# Local test with 2 nodes
java -Xms4g -Xmx4g -Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=localhost -Dgrid.buffer=5000 -Dqueue.url=tcp://localhost:61616 -Dqueue.security=true -Dqueue.usr=admin -Dqueue.pwd=admin -Dorg.apache.activemq.SERIALIZABLE_PACKAGES="*" -DnodeName=node1 -jar hacep-jvm-playground-1.0-SNAPSHOT.jar
java -Xms4g -Xmx4g -Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=localhost -Dgrid.buffer=5000 -Dqueue.url=tcp://localhost:61616 -Dqueue.security=true -Dqueue.usr=admin -Dqueue.pwd=admin -Dorg.apache.activemq.SERIALIZABLE_PACKAGES="*" -DnodeName=node2 -jar hacep-jvm-playground-1.0-SNAPSHOT.jar
