HACEP
=====

HACEP Core project

Build instructions
==================

To build the HACEP core code you just need Maven. Default builds code using the Red Hat supported repositories. 

```shell
mvn clean install
```

Alternatively, you can build the core using community repository: simply add the "community" profile to the Maven command

```shell
mvn clean install -P community
```

Install the Red Hat supported Maven repositories
------------------------------------------------

If you want to use the Red Hat supported bits, you must install JDG/BRMS/EAP repos.

How to install the various repositories:
 
* [JDG 7.0 beta maven repository](https://access.redhat.com/jbossnetwork/restricted/softwareDetail.html?softwareId=43361&product=data.grid&version=7.0.0+Beta&downloadType=distributions)
 in the JDG 7.0 beta [Getting Started Guide] (https://access.redhat.com/documentation/en-US/Red_Hat_JBoss_Data_Grid/7.0/html/Getting_Started_Guide/chap-Install_and_Use_the_Maven_Repositories.html) 
* [BRMS 6.3.0 maven repository](https://access.redhat.com/jbossnetwork/restricted/softwareDownload.html?softwareId=43621) in the BRMS 6.3 [Installation Guide](https://access.redhat.com/documentation/en-US/Red_Hat_JBoss_BRMS/6.3/html/Installation_Guide/chap-Maven_Repositories.html)
* [EAP 7.0 maven repository](https://access.redhat.com/jbossnetwork/restricted/softwareDetail.html?softwareId=43861&product=appplatform&version=&downloadType=distributions)

For your reference, you will find an example settings.xml to copy in your .m2 directory in the example-maven-settings directory.
This Maven settings.xml assumes you have unzipped the repositories in the following locations, so edit it accordingly:

* /opt/jboss-datagrid-7.0.0-maven-repository
* /opt/jboss-brms-bpmsuite-6.3.0.GA-maven-repository
* /opt/jboss-eap-7.0.0.GA-maven-repository/maven-repository


Run an HACEP example
--------------------

Please refer to hacep-examples -> hacep-playground submodule Readme.md for detailed instructions on how to run an HACEP example.