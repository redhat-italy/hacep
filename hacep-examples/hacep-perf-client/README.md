HACEP-perf-client
=================

A simple client to load HACEP through a simgle JMS Queue.

Running HACEP-perf-client
-------------------------

An example script is in the scripts directory. 

```shell
# Script with preload (10 players, preload of 20.000 fact for each one, 8 hours total duration with an interval per player of 1 second)
java -Dduration=480 -Dconcurrent.players=10 -Ddelay.range=5 -Devent.interval=1 -Dtest.preload=true -Dtest.messages=20000 -Dbroker.host="localhost:61616" -Dbroker.authentication=true -Dbroker.usr=admin -Dbroker.pwd=admin -cp hacep-perf-client-1.0-SNAPSHOT.jar it.redhat.hacep.client.App
```
Configuring HACEP-perf-client
-----------------------------

You can override HACEP-perf-client behaviour passing -D options from the CLI. 

Parameters:

* duration

Duration of the test. Default is 15 (in minutes) 

* concurrent.players

How many different concurrent players. Default is 5.

* delay.range

Each player starts at a random moment in the delay.range interval. Default is 15 (in seconds)

* event.interval

Interval time between each player event. Default is 3 (in seconds)

* test.preload

If true, HACEP-perf-client will preload the Queue with test.messages. Default is false

* test.messages

If test.preload is true, HACEP-perf-client will preload the Queue with test.messages. Default is 1000

* broker.host

The Host Broker to connect.Defaultis "localhost:61616"
 
* broker.authentication
 
If your broker needs authentication. Default is false

* broker.usr 

Broker authentication User

* broker.pwd 

Broker authentication User