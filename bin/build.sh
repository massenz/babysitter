#!/bin/bash

source $(dirname $0)/utils.sh

BUILD="$(git_sha)"

mvn package -Dbuild.number="${BUILD}"

