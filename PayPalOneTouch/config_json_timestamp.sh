#!/bin/bash

# Updates the timestamp within the `./otc-config.android.json` and unit test

now=`date -u +%FT%TZ`
sed -i '' 's/"file_timestamp": ".*",/"file_timestamp": "'$now'",/' ./otc-config.android.json
sed -i '' 's/assertEquals(".*"/assertEquals("'$now'"/' ./src/test/java/com/paypal/android/sdk/onetouch/core/config/ConfigManagerTest.java
echo "=> file_timestamp updated to" $now
