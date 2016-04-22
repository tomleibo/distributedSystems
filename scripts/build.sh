# Fail the script if anything bad happens
set -e

# use maven production profile that does not include aws sdk in the jar
mvn_profile=production

mvn --activate-profiles ${mvn_profile} --file common/pom.xml clean
mvn --activate-profiles ${mvn_profile} --file common/pom.xml compile assembly:single -DskipTests
cp common/target/common-1.0-SNAPSHOT-jar-with-dependencies.jar production

mvn --activate-profiles ${mvn_profile} --file manager/pom.xml clean
mvn --activate-profiles ${mvn_profile} --file manager/pom.xml compile assembly:single -DskipTests
cp manager/target/dsp-1-manager-1.0-SNAPSHOT-jar-with-dependencies.jar production

mvn --activate-profiles ${mvn_profile} --file local/pom.xml clean
mvn --activate-profiles ${mvn_profile} --file local/pom.xml compile assembly:single -DskipTests
cp local/target/dsp1-1.0-SNAPSHOT-jar-with-dependencies.jar production

mvn --activate-profiles ${mvn_profile} --file worker/pom.xml clean
mvn --activate-profiles ${mvn_profile} --file worker/pom.xml compile assembly:single -DskipTests
cp worker/target/dsp-1-worker-1.0-SNAPSHOT-jar-with-dependencies.jar production

# upload all the jars in the production dir, and make it public

RED='\033[0;31m'
NO_COLOR='\033[0m'

echo "${RED}* ------------------------------------------------------------------------------------------------ *"
echo '* In order to upload the jars to s3 run the following command:                                     *'
echo '* aws s3 cp production s3://dsp-jars --recursive --exclude "*" --include "*.jar" --acl public-read *'
echo "* ------------------------------------------------------------------------------------------------ *${NO_COLOR}"