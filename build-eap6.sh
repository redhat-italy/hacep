export HACEP_REPO=/opt
mvn -s example-maven-settings/settingsEAP6.xml -Psupported-GA-ee6 -Djava.net.preferIPv4Stack=true -Djgroups.bind_addr=localhost clean install
