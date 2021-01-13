require 'rake'
require 'io/console'

task :default => :tests

desc "Run Android unit tests and tests on a device or emulator"
task :tests => [:unit_tests, :integration_tests]

desc "Run Android lint on all modules"
task :lint do
  sh "./gradlew clean lint"
end

# Usage:
#   rake unit_tests
#   rake unit_tests"[com.braintreepayments.api.CardClientUnitTest,Card]"
#   rake unit_tests"[com.braintreepayments.api.CardClientUnitTest,Card,tokenize_sendsAnalyticsEventOnSuccess]"
desc "Run Android unit tests"
task :unit_tests, [:qualified_class, :module_name, :test_name] => :lint do |task, args|
  if args.module_name.nil?
    sh "./gradlew --continue testRelease"
  elsif args.test_name.nil?
    sh "./gradlew #{args[:module_name]}:testRelease --tests #{args[:qualified_class]}"
  else
    sh "./gradlew #{args[:module_name]}:testRelease --tests #{args[:qualified_class]}.#{args[:test_name]}"
  end
end

# Usage:
#   rake integration_tests
#   rake integration_tests"[com.braintreepayments.demo.test.DropInTest,demo]"
#   rake integration_tests"[com.braintreepayments.demo.test.DropInTest,demo,tokenizesACard]"
desc "Run Android tests on a device or emulator"
task :integration_tests, [:qualified_class, :module_name, :test_name] do |task, args|
  output = `adb devices`
  if output.match(/device$/)
    begin
      log_listener_pid = fork { exec 'ruby', 'script/log_listener.rb' }
      sh "ruby script/httpsd.rb /tmp/httpsd.pid"
      if args.file_path.nil?
        sh "./gradlew --continue runAllTests connectedAndroidTest -x :TestUtils:connectedAndroidTest"
      elsif args.test_name.nil?
        sh "./gradlew -Pandroid.testInstrumentationRunnerArguments.class=#{args[:qualified_class]} #{args[:module_name]}:connectedAndroidTest"
      else
        sh "./gradlew -Pandroid.testInstrumentationRunnerArguments.class=#{args[:qualified_class]}##{args[:test_name]} #{args[:module_name]}:connectedAndroidTest"
      end
    ensure
      `kill -9 \`cat /tmp/httpsd.pid\``
      `kill -9 #{log_listener_pid}`
    end
  else
    puts "Please connect a device or start an emulator and try again"
    exit 1
  end
end

desc "Interactive release to publish new version to maven local"
task :release_local do
  sh "./gradlew clean publishToMavenLocal"
end

desc "Interactive release to publish new version to nexus sonatype"
task :release => :unit_tests do
  Rake::Task["assumptions"].invoke

  puts "What version are you releasing? (x.x.x format)"
  version = $stdin.gets.chomp

  update_version(version)
  update_readme_version(version)

  prompt_for_sonatype_username_and_password

  Rake::Task["release_braintree"].invoke

  post_release(version)
end

task :assumptions do
    puts "Release Assumptions"
    puts "* [ ] You are on the branch and commit you want to release."
    puts "* [ ] You have already merged hotfixes and pulled changes."
    puts "* [ ] You have already reviewed the diff between the current release and the last tag, noting breaking changes in the semver and CHANGELOG."
    puts "* [ ] Tests (rake integration_tests) are passing, manual verifications complete."

    puts "Ready to release? Press any key to continue. "
    $stdin.gets
end

task :release_braintree do
  sh "./gradlew clean :AmericanExpress:publishToSonatype :BraintreeCore:publishToSonatype :BraintreeDataCollector:publishToSonatype :Card:publishToSonatype :LocalPayment:publishToSonatype :PayPal:publishToSonatype :SharedUtils:publishToSonatype :ThreeDSecure:publishToSonatype :UnionPay:publishToSonatype :Venmo:publishToSonatype"
  sh "./gradlew closeAndReleaseRepository"
  puts "Braintree modules have been released"
end

desc "Complete Github merge if Sonatype times out"
task :run_post_release do
    puts "What version are you running post release on? (x.x.x format)"
    version = $stdin.gets.chomp
    post_release(version)
end

desc "Create Demo APK"
task :create_demo_apk do
  puts "Enter keystore file path:"
  ENV["KEYSTORE_FILE"] = $stdin.gets.chomp.strip

  puts "Enter keystore password:"
  ENV["KEYSTORE_PASSWORD"] = $stdin.noecho(&:gets).chomp

  puts "Enter key alias:"
  ENV["KEY_ALIAS"] = $stdin.gets.chomp

  puts "Enter key password:"
  ENV["KEY_PASSWORD"] = $stdin.noecho(&:gets).chomp

  sh "./gradlew clean :Demo:assembleRelease"
end

def prompt_for_sonatype_username_and_password
  puts "Enter Sonatype username:"
  ENV["SONATYPE_USERNAME"] = $stdin.gets.chomp

  puts "Enter Sonatype password:"
  ENV["SONATYPE_PASSWORD"] = $stdin.noecho(&:gets).chomp
end

def post_release(version)
  if !`git remote`.include?("github")
    sh "git remote add github git@github.com:braintree/braintree_android.git"
  end

  puts "\nArchives are uploaded! Committing and tagging #{version} and preparing for the next development iteration"
  sh "git commit -am 'Release #{version}'"
  sh "git tag #{version} -a -m 'Release #{version}'"

  version_values = version.split('.')
  version_values[2] = version_values[2].to_i + 1
  update_version("#{version_values.join('.')}-SNAPSHOT")
  increment_version_code
  sh "git commit -am 'Prepare for development'"

  puts "\nDone. Commits and tags have been created. If everything appears to be in order, hit ENTER to push."
  $stdin.gets

  sh "git push origin master #{version}"

  puts "\nPushed to GHE! Press ENTER to push to public Github."
  $stdin.gets

  sh "git push github master #{version}"

  $stdin.gets
end

def get_current_version
  current_version = nil
  File.foreach("build.gradle") do |line|
    if match = line.match(/^version '(\d+\.\d+\.\d+(-SNAPSHOT)?)'/)
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
  IO.write("build.gradle",
    File.open("build.gradle") do |file|
      file.read.gsub(/^version '\d+\.\d+\.\d+(-SNAPSHOT)?'/, "version '#{version}'")
    end
  )
end

def update_readme_version(version)
  IO.write("README.md",
    File.open("README.md") do |file|
      file.read.gsub(/:braintree:\d+\.\d+\.\d+'/, ":braintree:#{version}'")
    end
  )
end

