HACEP-EAP-Playground
====================

A small project that shows a HACEP application running on JBoss EAP 7.

Introduction
--------------------
In this example, a HACEP enabled application (WAR) is deployed on a JBoss EAP 7 cluster running in "domain-mode". The HACEP application runs JBoss DataGrid in library-mode and will form a JDG cluster between the various EAP server-nodes on which the application is deployed. A JBoss A-MQ queue is used sas the event-channel, the transport mechanism over which events flow into the system. A small test-client is used to send events to the A-MQ queues. The HACEP application processes the events from the queue, and provides a visual representation of the Drools CEP sessions, and their backups, which are processed and managed by the various EAP/JDG nodes.

Required Software
--------------------
The HACEP EAP Playground demo requires the following software:
* [JBoss A-MQ 6.2.1](https://access.redhat.com/jbossnetwork/restricted/softwareDownload.html?softwareId=41271)
* [JBoss EAP 7.0] (https://access.redhat.com/jbossnetwork/restricted/softwareDownload.html?softwareId=43891)

Conventions
--------------------
In this documentation, we install all the software in the */opt/jboss* directory. However, this is not mandatory, you can install the software in any location.


Installing JBoss A-MQ
--------------------
To install the JBoss A-MQ platform:
* Unzip the `jboss-a-mq-6.2.1.redhat-084.zip` distribution in `/opt/jboss`.
* Enabled the *admin* user by opening the `/opt/jboss/jboss-a-mq-6.2.1.redhat-084/etc/users.properties` file and uncommenting the line
```admin=admin,admin,manager,viewer,Operator, Maintainer, Deployer, Auditor, Administrator, SuperUser```
* Start the JBoss A-MQ platform using the `amq` (Linux, Max OS X) or `amq.bat` (Windows) script.
* The A-MQ web-console can be found at http://localhost:8181, u:admin / p:admin.
* If you are in a virtual environment A-MQ might take a wile to start due to lack of entropy generated, a workaround to speed thing up is to pass `-Djava.security.egd=file:/dev/./urandom` option to use the pseudo-random device see: [https://issues.jboss.org/browse/ENTESB-3938](https://issues.jboss.org/browse/ENTESB-3938). 

Installing JBoss EAP 7
--------------------
To install the JBoss EAP 7 platform:
* Unzip the `jboss-eap-7.0.0.zip` distribution in `/opt/zip`.
* Create a new management user using the `add-user.sh` (Linux, Mac OS X) or `add-user.bat` (Windows) script in `/opt/jboss/jboss-eap-7.0/bin`. Add the user to the *Management Realm* and give it the username `admin` and password `jboss@01`. The user does not need to have any roles.
* Start the platform in `domain-mode by running the `domain.sh` (Linux, Mac OS X) or `domain.bat` (Windows) script in /opt/jboss/jboss-eap-7.0/bin` directory. This will start the platform in 'domain-mode' with the `/opt/jboss/jboss-eap-7.0/domain/configuration/host.xml` and `/opt/jboss/jboss-eap-7.0/domain/configuration/domain.xml` configuration files. Refer to the JBosS EAP 7 documentation for more information about these files.
* The management console of JBoss EAP 7 can now be accessed at http://localhost:9990, u:admin / p:jboss@01

Configuring JBoss EAP 7
--------------------
To be able to run our HACEP EAP Playground application, we need to configure the JBoss EAP 7 environment (profile), configure the server nodes and deploy the application. To configure the environment, we will use the JBoss EAP 7 Command Line Interface (CLI).

Before we can configure the EAP 7 platform, we first need to retrieve the JBoss A-MQ 6.2 Resource Adapter (RAR) from the JBoss A-MQ installation. This resource adapter is used to connect JBoss EAP to JBoss A-MQ through the EAP 7 JCA container.

The RAR file can be found in `/opt/jboss/jboss-a-mq-6.2.1.redhat-084/extras/apache-activemq-5.11.0.redhat-621084-bin.zip`. Unzip this file in the `/tmp` directory. The RAR file can now be found at `/tmp/apache-activemq-5.11.0.redhat-621084/lib/optional/activemq-rar-5.11.0.redhat-621084.rar`. Copy this file to `/opt/jboss`.

Start the `jboss-cli.sh` (Linux, Mac OS X) or `jboss-cli.bat` (Windows) CLI client located in the `/opt/jboss/jboss-eap-7.0/bin` directory. We can now configure our profile and server instances.

* In the JBoss CLI client, execute `connect` to connect the client to the Domain Controller:
```
You are disconnected at the moment. Type 'connect' to connect to the server or 'help' for the list of supported commands.
[disconnected /] connect
[domain@localhost:9990 /]
````
* We will first create a new server profile based on the provided `full` profile. We do this by *cloning* the `full` profile:
```
[domain@localhost:9990 /] /profile=full:clone(to-profile=hacep-full)
{
    "outcome" => "success",
    "result" => undefined,
    "server-groups" => undefined
}
```
* Next, we configure a new *server-group* which uses this profile:
```
[domain@localhost:9990 /] /server-group=hacep:add(socket-binding-group=full-sockets, profile=hacep-full)
{
    "outcome" => "success",
    "result" => undefined,
    "server-groups" => undefined
}
```
* We will now add 4 servers to the server-group. This is done with the following CLI commands. Note that we start with a port-offset of 400 on the first server, 500 on the second, etc.
```
/host=master/server-config=hacep-1:add(group=hacep,socket-binding-port-offset=400,auto-start=false)
/host=master/server-config=hacep-2:add(group=hacep,socket-binding-port-offset=500,auto-start=false)
/host=master/server-config=hacep-3:add(group=hacep,socket-binding-port-offset=600,auto-start=false)
/host=master/server-config=hacep-4:add(group=hacep,socket-binding-port-offset=700,auto-start=false)
```
* Deploy the A-MQ Resource Adapter (the RAR file we copied to `/opt/jboss`) to the `hacep` server group:
```
deploy /opt/jboss/activemq-rar-5.11.0.redhat-621084.rar --name=activemq-rar.rar --server-groups=hacep
```
* Set JBoss A-MQ related system properties:
```
/server-group=hacep/system-property=org.apache.activemq.SERIALIZABLE_PACKAGES:add(value="*")
```
* If you have built the project directly using one fo the maven settings files in `hacep/example-maven-settings` remember to instruct hacep runtime to use the same using `kie.maven.settings.custom` property and also set `HACEP_REPO` env variable:
```
/server-group=hacep/system-property=kie.maven.settings.custom:add(value="<path_to_settings_xml_used>")
/server-group=hacep/jvm=default:add(heap-size=2G,max-heap-size=2G)
/server-group=hacep/jvm=default/:write-attribute(name=environment-variables.HACEP_REPO,value=</path/to/extracted/repos>)
```
* We now need to configure the A-MQ Resource Adapter to provide a ConnectionFactory which creates connections to the JBoss A-MQ platform we installed earlier. This requires the following CLI commands:
```
/profile=hacep-full/subsystem=resource-adapters/resource-adapter=activemq-rar.rar:add(archive="activemq-rar.rar", transaction-support=XATransaction)
/profile=hacep-full/subsystem=resource-adapters/resource-adapter=activemq-rar.rar/config-properties=UserName:add(value="admin")
/profile=hacep-full/subsystem=resource-adapters/resource-adapter=activemq-rar.rar/config-properties=Password:add(value="admin")
/profile=hacep-full/subsystem=resource-adapters/resource-adapter=activemq-rar.rar/config-properties=ServerUrl:add(value="tcp://localhost:61616?jms.rmIdFromConnectionId=true")
/profile=hacep-full/subsystem=resource-adapters/resource-adapter=activemq-rar.rar/connection-definitions=ConnectionFactory:add(class-name="org.apache.activemq.ra.ActiveMQManagedConnectionFactory", jndi-name="java:/HACEPConnectionFactory", enabled=true, min-pool-size=1, max-pool-size=20, pool-prefill=false, same-rm-override=false, recovery-username="admin", recovery-password="admin", recovery-plugin-class-name="org.jboss.jca.core.recovery.ConfigurableRecoveryPlugin", recovery-plugin-properties={"EnableIsValid" => "false","IsValidOverride" => "true"})
```
The platform is now fully configured and the HACEP EAP Playground application can be deployed.


Deploying the application
--------------------
Before we can deploy the application, we first need to build it. This is explained in the *Build instruction* in the main `README.md` file of the project. After the libraries and applications have been built, we can deploy the application on the EAP 7 runtime. The WAR file to be deployed is located in your Maven (.m2) repository after you've built the project. On my machine, the location is `~/.m2/repository/it/redhat/jdg/examples/hacep-eap-playground/1.0-SNAPSHOT/hacep-eap-playground-1.0-SNAPSHOT.war`. Copy this WAR file to `/opt/jboss`.

The application can be deployed to the `hacep` server-group with the following CLI command:
```
deploy /opt/jboss/hacep-eap-playground-1.0-SNAPSHOT.war --server-groups=hacep
```

Starting the EAP nodes
--------------------
Now that we've fully configured the A-MQ and EAP platforms and deployed the application, we can start the server nodes. This is done with the following CLI commands (one can also do this from the EAP Management consoler at http://localhost:9990):
```
/server-group=hacep:start-servers
```
The HACEP EAP Playground GUI can now be accessed at http://localhost:8480/hacep-playground. If all is running correctly, the HACEP applications on all 4 nodeds will have formed a JBoss Data Grid cluster. The UI should show 4 black dots, each dot representing a HACEP node.

Running the demo
--------------------
To run the demo, we need to feed events into the JBoss A-MQ queue. This can be done using the `hacep-perf-client` provided in the `hacep-examples` directory. To run this client, simply go to the `hacep-examples/hacep-perf-client` directory and run:
```
mvn exec:java -Dduration=480 -Dconcurrent.players=20 -Ddelay.range=5 -Devent.interval=1 -Dtest.preload=true -Dtest.messages=10 -Dbroker.host="localhost:61616" -Dbroker.authentication=true -Dbroker.usr=admin -Dbroker.pwd=admin -Dorg.apache.activemq.SERIALIZABLE_PACKAGES="*"
```

In the HACEP GUI, sessions (balls in UI) will be shown, connected to the black dots representing the server nodes. The larger balls represent live sessions running in their primary owner, the smaller balls represent the backups of these sessions on their backup nodes, providing high availability.

To demonstrate the HA characteristics of the platform you can stop and start nodes of the cluster. The HACEP Playground GUI will show how the JDG cluster rebalances and performs its state-transfer, transferring and starting the Drools/BRMS CEP sessions on correct nodes. You can start and stop nodes using the following commands:
```
 /host=master/server-config=hacep-4:stop
```
```
 /host=master/server-config=hacep-4:start
```
