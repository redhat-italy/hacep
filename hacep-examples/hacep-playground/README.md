JDG-playground
==============

A small project to learn and explore Jboss Data Grid (Infinispan).
This is the common base project.

Build instructions
==================

For example to launch four nodes on a single machine just run these commands using different terminals:

```shell
mvn -P run -Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=localhost -DnodeName=node1

mvn -P run -Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=localhost -DnodeName=node2

mvn -P run -Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=localhost -DnodeName=node3

mvn -P run -Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=localhost -DnodeName=node4
```

Or on four different machines:

```shell
mvn -P run -Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=ip1

mvn -P run -Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=ip2

mvn -P run -Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=ip3

mvn -P run -Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=ip4
```

You can override HACEP behaviour passing -D options. Default values are:
* jgroups.configuration=jgroups-tcp.xml
* grid.mode=DIST_SYNC
* grid.owners=2
* grid.buffer=10 (size of the companion session buffer)
* queue.url=tcp://localhost:61616
* queue.name=HACEP.FACT
* queue.prefetch=5
* queue.consumers=5
* session.compression=false

For example :

```shell
mvn -P run -Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=localhost -Dqueue.name=BB_session_handler_0 -Dqueue.security=true -Dqueue.usr=fdmq -Dqueue.pwd=fdmq -DnodeName=node1"
```


Please refer to the parent project Readme.md for more details

Usage
-----

Every node will have its own command line interface "attached", which you can use to play with your Data Grid.
Type 'help' on the command line to show a list of commands:

```shell

all
     List all valuesFromKeys.

get id
     Get an object from the grid.

put id value
     Put an object (id, value) in the grid.

putIfAbsent|putifabsent|pia id value
     Put an object (id, value) in the grid if not already present

locate id
     Locate an object in the grid.

loadtest
     Load example values in the grid

local
     List all local valuesFromKeys.

primary
     List all local valuesFromKeys for which this node is primary.

clear
     Clear all valuesFromKeys.

info
     Information on cache.

replica
    List all local valuesFromKeys for which this node is a replica.
    
routing
     Print routing table.

help
     List of commands.

exit|quit|q|x
     Exit the shell.
```
