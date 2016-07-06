HACEP-playground
================

A small project to learn and explore HACEP using a command line.

Running HACEP-playground
------------------------

An example script is in the scripts directory. Alternatively, you can use mvn -P run.

For example to launch four nodes on a single machine for the basic playground just run these commands using different terminals:

```shell
mvn -P run -DnodeName=node1 -Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=localhost -Dgrid.buffer=5000 -Dqueue.url=tcp://localhost:61616 -Dqueue.security=true -Dqueue.usr=admin -Dqueue.pwd=admin

mvn -P run -DnodeName=node2 -Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=localhost -Dgrid.buffer=5000 -Dqueue.url=tcp://localhost:61616 -Dqueue.security=true -Dqueue.usr=admin -Dqueue.pwd=admin

mvn -P run -DnodeName=node3 -Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=localhost -Dgrid.buffer=5000 -Dqueue.url=tcp://localhost:61616 -Dqueue.security=true -Dqueue.usr=admin -Dqueue.pwd=admin

mvn -P run -DnodeName=node4 -Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=localhost -Dgrid.buffer=5000 -Dqueue.url=tcp://localhost:61616 -Dqueue.security=true -Dqueue.usr=admin -Dqueue.pwd=admin
```

If you bind on localhost and use UDP, you'll probably have to configure your routing table accordingly, see Troubleshooting section

Or on four different machines:

```shell
cd playground
mvn -P run -Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=localhost -Dgrid.buffer=5000 -Dqueue.url=tcp://localhost:61616 -Dqueue.security=true -Dqueue.usr=admin -Dqueue.pwd=admin

cd playground
mvn -P run -Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=localhost -Dgrid.buffer=5000 -Dqueue.url=tcp://localhost:61616 -Dqueue.security=true -Dqueue.usr=admin -Dqueue.pwd=admin

cd playground
mvn -P run -Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=localhost -Dgrid.buffer=5000 -Dqueue.url=tcp://localhost:61616 -Dqueue.security=true -Dqueue.usr=admin -Dqueue.pwd=admin

cd playground
mvn -P run -Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=localhost -Dgrid.buffer=5000 -Dqueue.url=tcp://localhost:61616 -Dqueue.security=true -Dqueue.usr=admin -Dqueue.pwd=admin
```

Configuring HACEP
-----------------

You can override HACEP behaviour passing -D options from the CLI. 

TOD: update with final version of parameters

Default values are:

* jgroups.configuration=jgroups-tcp.xml
* grid.mode=DIST_SYNC
* grid.owners=2
* grid.buffer=1000 (size of the facts session buffer)
* queue.url=tcp://localhost:61616
* queue.name=HACEP.FACT
* queue.prefetch=5
* queue.consumers=5

Please refer to the parent project Readme.md for more details

HACEP-Playground CLI Usage
--------------------------

Every node has attached its own command line interface, which you can use to play with HACEP.

TOD: update with final version of commands

Type 'help' on the command line to show a list of commands:

```shell

info
     Information on cache.

help
     List of commands.

exit|quit|q|x
     Exit the shell.
```

Troubleshooting
===============

Multicast routing
-----------------

When using UDP, IP multicasting is required

On some systems, could be needed to add multicast route(s) 

Otherwise, the default route will be used

Note that some systems don’t consult the routing table for IP multicast routing, only for unicast routing

MacOS example:

```shell
# When binding to 127.0.0.1, UDP.mcast_addr should be set to a value between 224.0.0.1 and 231.255.255.254
# Adds a multicast route for 224.0.0.1-231.255.255.254
sudo route add -net 224.0.0.0/5 127.0.0.1

# Adds a multicast route for 232.0.0.1-239.255.255.254
sudo route add -net 232.0.0.0/5 192.168.1.3
```

Linux example:

```shell
# When binding to 127.0.0.1, UDP.mcast_addr should be set to a value between 224.0.0.1 and 231.255.255.254
route add -net 224.0.0.0 netmask 240.0.0.0 dev lo
```
