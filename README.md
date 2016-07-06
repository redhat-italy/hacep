HACEP
=====

HACEP base project

Build instructions
==================

To build the code you just need Maven. This way you are building code using the Red Hat supported repositories. 

```shell
mvn clean install
```

Instead to build code using community repository simply add the "community" profile to the Maven command

```shell
mvn clean install -P community
```

Install the Maven repositories
------------------------------

If you want to use the Red Hat supported bits, you must install JDG repos and edit accordingly the <version.org.infinispan> attribute in POM.xml.

You'll find detailed instructions on how to install the [JDG 6.6.0 maven repository](https://access.redhat.com/jbossnetwork/restricted/softwareDownload.html?softwareId=42231&product=data.grid)
 in the JDG 6.6 [Getting Started Guide] (https://access.redhat.com/documentation/en-US/Red_Hat_JBoss_Data_Grid/6.6/html/Getting_Started_Guide/chap-Install_and_Use_the_Maven_Repositories.html) 
 and the [BRMS 6.3.0 maven repository](https://access.redhat.com/jbossnetwork/restricted/softwareDownload.html?softwareId=43621) in the BRMS 6.3 [Installation Guide](https://access.redhat.com/documentation/en-US/Red_Hat_JBoss_BRMS/6.3/html/Installation_Guide/chap-Maven_Repositories.html)

For your reference, you will find an example settings.xml to copy in your .m2 directory in the example-maven-settings directory.

This Maven settings.xml assumes you have unzipped the repositories in the following locations, so edit it accordingly:

* /opt/jboss-datagrid-6.6.0-maven-repository
* /opt/jboss-brms-bpmsuite-6.3.0.GA-maven-repository


Run the example
---------------

Please refer to hacep-playground submodule Readme.md for detailed instructions on how to run an example.