#!/bin/bash

android_path="$PWD"
if [ -z "${ANDROID_HOME}" ] && [ -d "/usr/local/android/android-sdk-linux" ]; then
  export ANDROID_HOME=/usr/local/android/android-sdk-linux
fi
android_adb=$ANDROID_HOME/platform-tools/adb
export PATH=$ANDROID_HOME/platform-tools:$PATH
test_screenshots_directory=$android_path/test_screenshots

export rvm_trust_rvmrcs_flag=1
gateway_path="$PWD/../$JOB_NAME-gateway"
gateway_pid="/tmp/$JOB_NAME-gateway-server"
gateway_port=3000

cd_android() {
  cd $android_path
}

cd_gateway() {
  cd $gateway_path
  source $HOME/.rvm/scripts/rvm
  source .rvmrc_ci
}

init_gateway_repo() {
  if [ -d $gateway_path ]; then
    (cd $gateway_path && git clean -dxf && git checkout . && git pull)
  else
    (git clone git@github.braintreeps.com:braintree/gateway.git $gateway_path)
    (cd $gateway_path && git checkout $1)
  fi
}

init_gateway() {
  ./ci.sh prepare
}

start_gateway() {
  stop_gateway

  bundle exec thin --port $gateway_port --pid "$gateway_pid" --daemonize start
  sleep 30
}

stop_gateway() {
  if [ -f $gateway_pid ];  then
    bundle exec thin --pid "$gateway_pid" stop
  fi
}

build_cleanup() {
  cd_gateway
  stop_gateway
  $android_adb emu kill
  $android_adb kill-server
  kill -9 `cat /tmp/httpsd.pid`
}

start_adb() {
  echo "Starting ADB server"
  $android_adb start-server
  echo "ADB server started"
}

start_emulator() {
  echo "Starting emulator"
  $ANDROID_HOME/tools/emulator -avd android19 -no-boot-anim -wipe-data -no-audio -no-window &
}

wait_for_emulator() {
  echo "Waiting for emulator to start"
  $android_adb wait-for-device

  # This is a hack - wait-for-device just checks power on.
  # By polling until the package manager is ready, we can make sure a device is actually booted
  # before attempting to run tests.
  echo "Waiting for device package manager to load"
  while [[ `$android_adb shell pm path android` == 'Error'* ]]; do
    sleep 2
  done
  echo "Emulator fully armed and operational, starting tests"
}

download_screenshots() {
  rm -rf $test_screenshots_directory
  mkdir $test_screenshots_directory
  $android_adb pull /sdcard/BraintreeUITestScreenshots $test_screenshots_directory && $android_adb shell rm -r /sdcard/BraintreeUITestScreenshots
}

cd_android
start_adb
start_emulator

init_gateway_repo $GATEWAY_BRANCH
cd_gateway
init_gateway
start_gateway

cd_android
wait_for_emulator

ruby script/httpsd.rb /tmp/httpsd.pid

$android_path/gradlew --info --no-color clean runAllTests connectedAndroidTest
test_return_code=$?

download_screenshots
build_cleanup

exit $test_return_code;
