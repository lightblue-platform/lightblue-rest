#!/usr/bin/env sh

echo "DEPLOY MASTER TRAVIS BUILD"
echo "Current directory is $(pwd)"
'[[ $TRAVIS_BRANCH == "master" ]] && [[ $TRAVIS_JDK_VERSION == "openjdk7" ]] && { mvn clean deploy -DskipTests; };'
mvn -B clean verify cobertura:cobertura coveralls:cobertura -Dorg.apache.maven.user-settings=~/settings-arquillian.xml  -Dorg.apache.maven.global-settings=~/settings-arquillian.xml
