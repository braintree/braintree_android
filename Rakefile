require 'rake'

BRAINTREE_API_BUILD_GRADLE = "BraintreeApi/build.gradle"
DROP_IN_BUILD_GRADLE = "Drop-In/build.gradle"

task :default => :tests

desc "Run Android lint on all modules"
task :lint do
  sh "./gradlew clean lint"
end

desc "Run all Android tests"
task :tests => :lint do
  output = `adb devices`
  if output.match(/device$/)
    begin
      log_listener_pid = fork { exec 'ruby', 'script/log_listener.rb' }
      sh "ruby script/httpsd.rb /tmp/httpsd.pid"
      sh "./gradlew --continue runAllTests test connectedAndroidTest"
    ensure
      `kill -9 \`cat /tmp/httpsd.pid\``
      `kill -9 #{log_listener_pid}`
    end
  else
    puts "Please connect a device or start an emulator and try again"
    exit 1
  end
end

desc "Publish current version as a SNAPSHOT"
task :publish_snapshot => :tests do
  abort("Version must contain '-SNAPSHOT'!") unless get_current_version.end_with?('-SNAPSHOT')

  sh "./gradlew clean :BraintreeDataCollector:uploadArchives"

  replace_string(BRAINTREE_API_BUILD_GRADLE, "compile project(':BraintreeDataCollector')", "compile 'com.braintreepayments.api:data-collector:#{get_current_version}'")
  sh "./gradlew clean :BraintreeApi:uploadArchives"

  replace_string(DROP_IN_BUILD_GRADLE, "compile project(':BraintreeApi')", "compile 'com.braintreepayments.api:braintree:#{get_current_version}'")
  sh "./gradlew clean :Drop-In:uploadArchives"

  replace_string(BRAINTREE_API_BUILD_GRADLE, "compile 'com.braintreepayments.api:data-collector:#{get_current_version}'", "compile project(':BraintreeDataCollector')")
  replace_string(DROP_IN_BUILD_GRADLE, "compile 'com.braintreepayments.api:braintree:#{get_current_version}'", "compile project(':BraintreeApi')")
end

desc "Interactive release to publish new version"
#task :release => :tests do
task :release do
  last_version = `git tag | tail -1`.chomp
  puts "\nChanges since #{last_version}:"
  sh "git log --pretty=format:\"%h %ad%x20%s%x20%x28%an%x29\" --date=short #{last_version}.."
  puts "Please update your CHANGELOG.md. Press ENTER when you are done"
  $stdin.gets

  puts "What version are you releasing? (x.x.x format)"
  version = $stdin.gets.chomp

  increment_version_code
  update_version(version)

  sh "./gradlew clean :BraintreeDataCollector:uploadArchives"
  puts "BraintreeDataCollector was uploaded, press ENTER to release it"
  $stdin.gets
  sh "./gradlew :BraintreeDataCollector:closeRepository"
  puts "Sleeping for one minute to allow closing to finish"
  sleep 60
  sh "./gradlew :BraintreeDataCollector:promoteRepository"
  puts "Sleeping for ten minutes to allow promotion to finish"
  sleep 600

  replace_string(BRAINTREE_API_BUILD_GRADLE, "compile project(':BraintreeDataCollector')", "compile 'com.braintreepayments.api:data-collector:#{version}'")
  sh "./gradlew clean :BraintreeApi:uploadArchives"
  puts "BraintreeApi was uploaded, press ENTER to release it"
  $stdin.gets
  sh "./gradlew :BraintreeApi:closeRepository"
  puts "Sleeping for one minute to allow closing to finish"
  sleep 60
  sh "./gradlew :BraintreeApi:promoteRepository"
  puts "Sleeping for ten minutes to allow promotion to finish"
  sleep 600

  replace_string(DROP_IN_BUILD_GRADLE, "compile project(':BraintreeApi')", "compile 'com.braintreepayments.api:braintree:#{version}'")
  sh "./gradlew clean :Drop-In:uploadArchives"
  puts "Drop-In was uploaded, press ENTER to release it"
  $stdin.gets
  sh "./gradlew :Drop-In:closeRepository"
  puts "Sleeping for one minute to allow closing to finish"
  sleep 60
  sh "./gradlew :Drop-In:promoteRepository"

  puts "\nArchives are uploaded! Committing and tagging #{version} and preparing for the next development iteration"
  sh "git commit -am 'Release #{version}'"
  sh "git tag #{version} -am '#{version}'"

  replace_string(BRAINTREE_API_BUILD_GRADLE, "compile 'com.braintreepayments.api:data-collector:#{version}'", "compile project(':BraintreeDataCollector')")
  replace_string(DROP_IN_BUILD_GRADLE, "compile 'com.braintreepayments.api:braintree:#{version}'", "compile project(':BraintreeApi')")
  update_version("#{version}-SNAPSHOT")
  sh "git commit -am 'Prepare for development'"

  puts "\nDone. Commits and tags have been created. If everything appears to be in order, hit ENTER to push."
  $stdin.gets

  sh "git push origin master #{version}"

  puts "\nPushed to GHE! Press ENTER to push to public Github."
  $stdin.gets

  sh "git push github master #{version}"

  puts "\nSend release notification email to braintree-sdk-announce@googlegroups.com. Press ENTER when done."
  $stdin.gets

  puts "\nUpdate client_releases.yml in the docs. Press ENTER when done."
  $stdin.gets
end

def get_current_version
  current_version = nil
  File.foreach("build.gradle") do |line|
    if match = line.match(/versionName = '(\d+\.\d+\.\d+(-SNAPSHOT)?)'/)
      current_version = match.captures
    end
  end

  return current_version[0]
end

def increment_version_code
  new_build_file = ""
  File.foreach("build.gradle") do |line|
    if line.match(/versionCode = (\d+)/)
      new_build_file += line.gsub(/versionCode = \d+/, "versionCode = #{$1.to_i + 1}")
    else
      new_build_file += line
    end
  end
  IO.write('build.gradle', new_build_file)
end

def update_version(version)
  replace_string("build.gradle", /versionName = '\d+\.\d+\.\d+(-SNAPSHOT)?'/, "versionName = '#{version}'")
end

def replace_string(filepath, string_to_replace, new_string)
  IO.write(filepath,
    File.open(filepath) do |file|
      file.read.gsub(string_to_replace, new_string)
    end
  )
end
