HACEP
=====

HACEP base project

Build instructions
==================

To build the code you just need Maven.

Install the Maven repositories
------------------------------

If you want to use the Red Hat supported bits, you must install JDG repos and edit the <version.org.infinispan> attribute in POM.xml.

You'll find detailed instructions on how to install the [JDG 6.6.0 maven repository](https://access.redhat.com/jbossnetwork/restricted/softwareDownload.html?softwareId=42231&product=data.grid)
 in the JDG 6.6 [Getting Started Guide] (https://access.redhat.com/documentation/en-US/Red_Hat_JBoss_Data_Grid/6.6/html/Getting_Started_Guide/chap-Install_and_Use_the_Maven_Repositories.html) and the [BRMS 6.2.0 maven repository](https://access.redhat.com/jbossnetwork/restricted/softwareDownload.html?softwareId=41051) in the BRMS 6.2 [Installation Guide](https://access.redhat.com/documentation/en-US/Red_Hat_JBoss_BRMS/6.2/html/Installation_Guide/chap-Maven_Repositories.html)

For your reference, you will find an example settings.xml to copy in your .m2 directory in the example-maven-settings directory.

This Maven settings.xml assumes you have unzipped the repositories in the following locations, so edit it accordingly:

* /opt/jboss-datagrid-6.6.0-maven-repository

Build the code
--------------

You can optionally run these commands if you want to build every module upfront.

```shell
mvn clean install
```

Run the examples
----------------

To run some nodes, just enter in one of the modules and execute the correct profile.

For example to launch four nodes on a single machine for the basic playground just run these commands using different terminals:

```shell
mvn -P run -Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=localhost

mvn -P run -Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=localhost

mvn -P run -Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=localhost

mvn -P run -Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=localhost
```

If you bind on localhost and use UDP, you'll probably have to configure your routing table accordingly:

```shell
sudo route add -net 224.0.0.0/5 127.0.0.1
```

Or on four different machines:

```shell
cd playground
mvn -P run -Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=ip1

cd playground
mvn -P run -Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=ip2

cd playground
mvn -P run -Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=ip3

cd playground
mvn -P run -Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=ip4
```

Usage
-----

Please refer to every submodule Readme.md for detailed instructions
