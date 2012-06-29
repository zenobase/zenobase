=== Elasticsearch Logging ===

sudoedit /etc/elasticsearch/logging.yml
# replace console,file with syslog and add:
--
  syslog:
    type: syslog
    syslogHost: localhost
    facility: user
    facilityPrinting: true
    layout:
      type: pattern
      conversionPattern: "[%p][%c] %m%n"
--

sudoedit /etc/elasticsearch/elasticsearch.yml


=== rsyslogd for Loggly ===

sudoedit /etc/rsyslog.d/80-loggly.conf
---
$ModLoad imudp
$UDPServerRun 514
*.warn  @@logs.loggly.com:28388
---

sudo service rsyslog restart

logger -p err just kidding


=== Sematext SPM ===

wget http://apps.sematext.com/spm-dist/spm-client-es-ubuntu-1.5.0.zip
unzip -q spm-client-es-ubuntu-*.zip
cd spm-client-es-ubuntu-*
nano nano spm-setup.properties
--
spm_sender_system_token=XXX
--
sudo bash spm-client-install.sh 2>&1 | tee -a /tmp/spm-install.log
sudoedit /etc/init.d/elasticsearch
--
ES_JAVA_OPTS="-server -Dcom.sun.management.jmxremote -javaagent:/spm/spm-monitor/lib/spm-monitor-es-1.5.0-withdeps.jar=/spm/spm-monitor/conf/spm-monitor-config-83dae3f9-1a35-458c-b6ca-e1e6eec43d1b-default.xml"
--
sudo service elasticsearch restart
cd ..
rm -rf spm-client-es-ubuntu-*


=== New Relic Agent ===

# download newrelic_agent2.6.0.zip
sudo unzip -q -d /usr/lib/newrelic newrelic_agent2.6.0.zip
rm newrelic_agent2.6.0.zip
sudoedit /usr/lib/newrelic/newrelic.yml
# set app_name=Play|Elasticsearch

sudoedit /etc/default/elasticsearch
--
ES_JAVA_OPTS="-server -javaagent:/usr/lib/newrelic/newrelic.jar -Dnewrelic.environment=elasticsearch"
--
sudo service elasticsearch restart

sudoedit /etc/init/play.conf
--
env JAVA_OPTS="... -javaagent:/usr/lib/newrelic/newrelic.jar -Dnewrelic.environment=play"
--
sudo service play restart
