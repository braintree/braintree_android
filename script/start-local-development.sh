#!/usr/bin/env bash

# exit with failure when an error occurs
set -e

# run gradle in continuous build mode to upload archives to local maven when changes occur
./gradlew -t assembleDebug uploadArchives 

