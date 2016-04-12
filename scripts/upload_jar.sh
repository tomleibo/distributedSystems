mvn install --file common/pom.xml

mvn --file manager/pom.xml clean compile assembly:single -DskipTests
cp manager/target/dsp-1-manager-1.0-SNAPSHOT-jar-with-dependencies.jar production

mvn --file local/pom.xml clean compile assembly:single -DskipTests
cp local/target/dsp1-1.0-SNAPSHOT-jar-with-dependencies.jar production

mvn --file worker/pom.xml clean compile assembly:single -DskipTests
ccp worker/target/dsp-1-worker-1.0-SNAPSHOT-jar-with-dependencies.jar production

# aws s3 cp production s3://dsp-jars --recursive --exclude "*" --include "*.jar"