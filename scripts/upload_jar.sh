
# use maven production profile that does not include aws sdk in the jar
mvn_profile=production

mvn --activate-profiles ${mvn_profile} install --file common/pom.xml -DskipTests

mvn --activate-profiles ${mvn_profile} --file manager/pom.xml clean
mvn --activate-profiles ${mvn_profile} --file manager/pom.xml compile assembly:single -DskipTests
cp manager/target/dsp-1-manager-1.0-SNAPSHOT-jar-with-dependencies.jar production

mvn --activate-profiles ${mvn_profile} --file local/pom.xml clean
mvn --activate-profiles ${mvn_profile} --file local/pom.xml compile assembly:single -DskipTests
cp local/target/dsp1-1.0-SNAPSHOT-jar-with-dependencies.jar production

mvn --activate-profiles ${mvn_profile} --file worker/pom.xml clean
mvn --activate-profiles ${mvn_profile} --file worker/pom.xml compile assembly:single -DskipTests
cp worker/target/dsp-1-worker-1.0-SNAPSHOT-jar-with-dependencies.jar production

# aws s3 cp production s3://dsp-jars --recursive --exclude "*" --include "*.jar"
