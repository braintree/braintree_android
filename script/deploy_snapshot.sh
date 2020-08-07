#!/bin/bash

set -e

SLUG="braintree/braintree_android"
JDK="oraclejdk8"
BRANCH="master"

if [ "$TRAVIS_REPO_SLUG" != "$SLUG" ]; then
  echo "Skipping snapshot deployment: wrong repository. Expected '$SLUG' but was '$TRAVIS_REPO_SLUG'."
elif [ "$TRAVIS_JDK_VERSION" != "$JDK" ]; then
  echo "Skipping snapshot deployment: wrong JDK. Expected '$JDK' but was '$TRAVIS_JDK_VERSION'."
elif [ "$TRAVIS_PULL_REQUEST" != "false" ]; then
  echo "Skipping snapshot deployment: was pull request."
elif [ "$TRAVIS_BRANCH" != "$BRANCH" ]; then
  echo "Skipping snapshot deployment: wrong branch. Expected '$BRANCH' but was '$TRAVIS_BRANCH'."
elif [[ $(./gradlew properties | grep versionName) != *-SNAPSHOT ]]; then
  echo "Skipping snapshot deployment: not a snapshot version."
else
  echo "Deploying snapshot..."
  ./gradlew :Core:publishToSonatype :BraintreeDataCollector:publishToSonatype :PayPalDataCollector:publishToSonatype :PayPalOneTouch:publishToSonatype :Braintree:publishToSonatype
  echo "Snapshot deployed!"
fi
