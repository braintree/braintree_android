require 'rake'

TMP_CHANGELOG_FILE = "/tmp/braintree-android-release.md"

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
      sh "./gradlew --continue runAllTests test connectedAndroidTest -x :TestUtils:connectedAndroidTest"
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

  sh "./gradlew clean :Core:uploadArchives :BraintreeDataCollector:uploadArchives :PayPalDataCollector:uploadArchives :PayPalOneTouch:uploadArchives :Braintree:uploadArchives"
end

desc "Interactive release to publish new version"
task :release do
  puts "What version are you releasing? (x.x.x format)"
  version = $stdin.gets.chomp

  prompt_for_change_log(version)
  increment_version_code
  update_version(version)

  Rake::Task["release_braintree"].invoke
  Rake::Task["release_paypal"].invoke

  post_release(version)
end

task :release_braintree do
  sh "./gradlew clean :Core:uploadArchives :BraintreeDataCollector:uploadArchives :Braintree:uploadArchives"
  sh "./gradlew :Braintree:closeRepository"
  puts "Sleeping for one minute to allow Braintree modules to close"
  sleep 60
  sh "./gradlew :Braintree:promoteRepository"
  puts "Sleeping for ten minutes to allow Braintree modules to be promoted"
  sleep 600
  puts "Braintree modules have been released"
end

task :release_paypal do
  sh "./gradlew clean :PayPalDataCollector:uploadArchives :PayPalOneTouch:uploadArchives"
  sh "./gradlew :PayPalOneTouch:closeRepository"
  puts "Sleeping for one minute to allow PayPal modules to close"
  sleep 60
  sh "./gradlew :PayPalOneTouch:promoteRepository"
  puts "PayPal modules have been released"
end

def prompt_for_change_log(version)
  last_version = `git tag | tail -1`.chomp
  tmp_change_log = "#{version}"
  tmp_change_log += "\n\n# Please enter a summary of the changes below."
  tmp_change_log += "\n# Lines starting with '# ' will be ignored."
  tmp_change_log += "\n#"
  tmp_change_log += "\n# Changes since #{last_version}:"
  tmp_change_log += "\n#"
  tmp_change_log += "\n# "
  tmp_change_log += `git log --pretty=format:"%h %ad%x20%s%x20%x28%an%x29" --date=short #{last_version}..`.gsub("\n", "\n# ")
  tmp_change_log += "\n#"
  tmp_change_log += "\n"
  File.foreach("CHANGELOG.md") do |line|
    tmp_change_log += "# #{line}"
  end
  IO.write(TMP_CHANGELOG_FILE, tmp_change_log)

  puts "\n"
  sh "$EDITOR #{TMP_CHANGELOG_FILE}"

  new_changes = ""
  File.foreach(TMP_CHANGELOG_FILE) do |line|
    if !line.start_with?("# ") && !line.start_with?("#\n")
      new_changes += line
    end
  end

  IO.write("CHANGELOG.md",
    File.open("CHANGELOG.md") do |file|
      file.read.gsub("# Braintree Android SDK Release Notes\n", "# Braintree Android SDK Release Notes\n\n## #{new_changes.chomp}")
    end
  )
end

def post_release(version)
  if !`git remote`.include?("github")
    sh "git remote add github https://github.com/braintree/braintree_android.git"
  end

  puts "\nArchives are uploaded! Committing and tagging #{version} and preparing for the next development iteration"
  sh "git commit -am 'Release #{version}'"
  sh "git tag -aF #{TMP_CHANGELOG_FILE} #{version}"

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
end

def get_current_version
  current_version = nil
  File.foreach("build.gradle") do |line|
    if match = line.match(/version = '(\d+\.\d+\.\d+(-SNAPSHOT)?)'/)
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
      file.read.gsub(/version = '\d+\.\d+\.\d+(-SNAPSHOT)?'/, "version = '#{version}'")
    end
  )
end
