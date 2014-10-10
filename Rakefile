require 'rake'

task :default => :tests

task :lint do
  sh "./gradlew clean lint"
end

task :tests => :lint do
  output = `adb devices`
  if output.match(/device$/)
    begin
      sh "ruby script/httpsd.rb /tmp/httpsd.pid"
      log_listener_pid = fork { exec 'ruby', 'log_listener.rb' }
      sh "./gradlew --info runAllTests :BraintreeData:connectedAndroidTest :BraintreeApi:connectedAndroidTest :Drop-In:connectedAndroidTest"
    ensure
      `kill -9 \`cat /tmp/httpsd.pid\``
      `kill -9 #{log_listener_pid}`
    end
  else
    puts "Please connect a device or start an emulator and try again"
    exit 1
  end
end

task :release => :lint do
  braintree_data_build_file = "BraintreeData/build.gradle"
  braintree_api_build_file = "BraintreeApi/build.gradle"
  braintree_drop_in_build_file = "Drop-In/build.gradle"
  braintree_demo_build_file = "Demo/build.gradle"
  braintree_fake_wallet_build_file = "FakeWallet/build.gradle"
  braintree_test_utils_build_file = "TestUtils/build.gradle"

  last_version = `git tag | tail -1`.chomp
  puts "Changes since #{last_version}:"
  sh "git log --pretty=format:\"%h %ad%x20%s%x20%x28%an%x29\" --date=short #{last_version}.."
  puts "Please update your CHANGELOG.md. Press ENTER when you are done"
  $stdin.gets

  puts "What version are you releasing? (x.x.x format)"
  version = $stdin.gets.chomp

  increment_version_code(braintree_data_build_file)
  update_version(braintree_data_build_file, version)
  sh "./gradlew clean :BraintreeData:uploadArchives"
  puts "BraintreeData was uploaded, please promote it on oss.sonatype.org. Press ENTER when you have promoted it"
  $stdin.gets

  increment_version_code(braintree_api_build_file)
  update_version(braintree_api_build_file, version)
  replace_string(braintree_api_build_file, "compile project(':BraintreeData')", "compile 'com.braintreepayments.api:data:#{version}'")
  sh "./gradlew clean :BraintreeApi:uploadArchives"
  puts "BraintreeApi was uploaded, please promote it on oss.sonatype.org. Press ENTER when you have promoted it"
  $stdin.gets

  increment_version_code(braintree_drop_in_build_file)
  update_version(braintree_drop_in_build_file, version)
  replace_string(braintree_drop_in_build_file, "compile project(':BraintreeApi')", "compile 'com.braintreepayments.api:braintree-api:#{version}'")
  sh "./gradlew clean :Drop-In:uploadArchives"
  puts "Drop-In was uploaded, please promote it on oss.sonatype.org. Press ENTER when you have promoted it"
  $stdin.gets

  increment_version_code(braintree_demo_build_file)
  update_version(braintree_demo_build_file, version)

  increment_version_code(braintree_fake_wallet_build_file)
  update_version(braintree_fake_wallet_build_file, version)

  increment_version_code(braintree_test_utils_build_file)
  update_version(braintree_test_utils_build_file, version)

  puts "Archives are uploaded! Commiting and tagging #{version} and preparing for the next development iteration"
  sh "git commit -am 'Release #{version}'"
  sh "git tag #{version} -am '#{version}'"

  replace_string(braintree_api_build_file, "compile 'com.braintreepayments.api:data:#{version}'", "compile project(':BraintreeData')")
  replace_string(braintree_drop_in_build_file, "compile 'com.braintreepayments.api:braintree-api:#{version}'", "compile project(':BraintreeApi')")
  sh "git commit -am 'Prepare for development'"

  puts "Done. Commits and tags have been created. If everything appears to be in order, hit ENTER to push."
  $stdin.gets

  sh "git push origin master #{version}"

  puts "Pushed to GHE! Press ENTER to push to public Github."
  $stdin.gets

  sh "git push github master #{version}"
end

def increment_version_code(filepath)
  new_build_file = ""
  File.foreach(filepath) do |line|
    if line.match(/versionCode (\d+)/)
      new_build_file += line.gsub(/versionCode \d+/, "versionCode #{$1.to_i + 1}")
    else
      new_build_file += line
    end
  end
  IO.write(filepath, new_build_file)
end

def update_version(filepath, version)
  replace_string(filepath, /versionName '\d+\.\d+\.\d+'/, "versionName '#{version}'")
end

def replace_string(filepath, string_to_replace, new_string)
  IO.write(filepath,
    File.open(filepath) do |file|
      file.read.gsub(string_to_replace, new_string)
    end
  )
end
