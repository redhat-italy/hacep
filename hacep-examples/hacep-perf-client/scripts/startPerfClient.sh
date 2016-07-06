#!/usr/bin/env bash

# Script with preload (10 players, preload of 20.000 fact for each one, 8 hours total duration with an interval per player of 1 second)
java -Dduration=480 -Dconcurrent.players=10 -Ddelay.range=5 -Devent.interval=1 -Dtest.preload=true -Dtest.messages=20000 -Dbroker.host="localhost:61616" -Dbroker.authentication=true -Dbroker.usr=admin -Dbroker.pwd=admin -cp hacep-perf-client-1.0-SNAPSHOT.jar it.redhat.hacep.hacep.client.App
