#!/bin/bash

source $(dirname $0)/utils.sh

VERSION="$(get_version)"
BUILD="$(git_sha)"

JAR="target/babysitter-${VERSION}-${BUILD}.jar"

if [[ ! -e ${JAR} ]]; then
  echo "ERROR: JAR file ${JAR} does not exist"
  exit 1
fi

java -Dspring.profiles.default="test" \
     -Dbootstrap.location="file://`pwd`/config/bootstrap.json" \
     -jar ${JAR}

