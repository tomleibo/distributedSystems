SET mvn_profile=production

call mvn --activate-profiles %mvn_profile% --file common\pom.xml clean
call mvn --activate-profiles %mvn_profile% --file common\pom.xml install assembly:single -DskipTests
call copy common\target\common-1.0-SNAPSHOT-jar-with-dependencies.jar production\common-1.0-SNAPSHOT-jar-with-dependencies.jar

call mvn --activate-profiles %mvn_profile% --file manager\pom.xml clean
call mvn --activate-profiles %mvn_profile% --file manager\pom.xml compile assembly:single -DskipTests
call copy manager\target\dsp-1-manager-1.0-SNAPSHOT-jar-with-dependencies.jar production\dsp-1-manager-1.0-SNAPSHOT-jar-with-dependencies.jar

call mvn --activate-profiles %mvn_profile% --file local\pom.xml clean
call mvn --activate-profiles %mvn_profile% --file local\pom.xml compile assembly:single -DskipTests
call copy local\target\dsp1-1.0-SNAPSHOT-jar-with-dependencies.jar production\dsp1-1.0-SNAPSHOT-jar-with-dependencies.jar

call mvn --activate-profiles %mvn_profile% --file worker\pom.xml clean
call mvn --activate-profiles %mvn_profile% --file worker\pom.xml compile assembly:single -DskipTests
call copy worker\target\dsp-1-worker-1.0-SNAPSHOT-jar-with-dependencies.jar production\dsp-1-worker-1.0-SNAPSHOT-jar-with-dependencies.jar 

call echo '* In order to upload the jars to s3 run the following command:*'
call echo  '* aws s3 cp production s3://dsp-jars --recursive --exclude "*" --include "*.jar" --acl public-read *'