#!/usr/bin/env bash
export HACEP_REPO=/opt/repos
mvn -s example-maven-settings/settingsEAP7.xml -Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=localhost clean install
