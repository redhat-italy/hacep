#!/usr/bin/env bash

# Script with preload (10 players, preload 20.000 fact for each one)
java -Dconcurrent.players=10 -Ddelay.range=5 -Devent.interval=1 -Dtest.preload=true -Dtest.messages=20000 -Dbroker.host="localhost:61616" -Dbroker.security=true -Dbroker.usr=admin -Dbroker.pwd=admin -Dduration=480 -cp hacep-perf-client-1.0-SNAPSHOT.jar it.redhat.hacep.hacep.client.App
