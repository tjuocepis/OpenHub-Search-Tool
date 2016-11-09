set -e
set -v

# Talk to the metadata server to get the project id
PROJECTID=$(curl -s "http://metadata.google.internal/computeMetadata/v1/project/project-id" -H "Metadata-Flavor: Google")
BUCKET=$(curl -s "http://metadata.google.internal/computeMetadata/v1/instance/attributes/BUCKET" -H "Metadata-Flavor: Google")

echo "Project ID: ${PROJECTID}  Bucket: ${BUCKET}"

# get our file(s)
gsutil cp "gs://${BUCKET}/gce/"** .

# Install dependencies from apt
apt-get update
apt-get install  -yq openjdk-8-jdk

# Make Java8 the default
update-alternatives --set java /usr/lib/jvm/java-8-openjdk-amd64/jre/bin/java

# Jetty Setup
mkdir -p /opt/jetty/temp
mkdir -p /var/log/jetty

# Get Jetty
curl -L http://eclipse.org/downloads/download.php?file=/jetty/stable-9/dist/jetty-distribution-9.3.8.v20160314.tar.gz\&r=1 -o jetty9.tgz
tar xf jetty9.tgz  --strip-components=1 -C /opt/jetty

# Add a Jetty User
useradd --user-group --shell /bin/false --home-dir /opt/jetty/temp jetty

cd /opt/jetty
# Add running as "jetty"
java -jar /opt/jetty/start.jar --add-to-startd=setuid
cd /

# very important - by renaming the war to root.war, it will run as the root servlet.
mv bookshelf-1.0-SNAPSHOT.war /opt/jetty/webapps/root.war

# Make sure "jetty" owns everything.
chown --recursive jetty /opt/jetty

# Configure the default paths for the Jetty service
cp /opt/jetty/bin/jetty.sh /etc/init.d/jetty
echo "JETTY_HOME=/opt/jetty" > /etc/default/jetty
{
  echo "JETTY_BASE=/opt/jetty"
  echo "TMPDIR=/opt/jetty/temp"
  echo "JAVA_OPTIONS=-Djetty.http.port=80"
  echo "JETTY_LOGS=/var/log/jetty"
} >> /etc/default/jetty

# -Dorg.eclipse.jetty.util.log.class=org.eclipse.jetty.util.log.JavaUtilLog


# Help with debugging
service jetty status

# Install logging monitor. The monitor will automatically pickup logs sent to syslog.
curl -s "https://storage.googleapis.com/signals-agents/logging/google-fluentd-install.sh" | bash
service google-fluentd restart &

service jetty start
service jetty check

echo "Startup Complete"
