require 'rake'

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
      sh "ruby script/httpsd.rb /tmp/httpsd.pid"
      log_listener_pid = fork { exec 'ruby', 'script/log_listener.rb' }
      sh "./gradlew --info --continue runAllTests connectedAndroidTest"
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

  sh "./gradlew clean :BraintreeData:uploadArchives :BraintreeApi:uploadArchives :Drop-In:uploadArchives"
end

desc "Interactive release to publish new version"
task :release do
  last_version = `git tag | grep "^1." | tail -1`.chomp
  puts "\nChanges since #{last_version}:"
  sh "git log --pretty=format:\"%h %ad%x20%s%x20%x28%an%x29\" --date=short #{last_version}.."
  puts "Please update your CHANGELOG.md. Press ENTER when you are done"
  $stdin.gets

  puts "What version are you releasing? (x.x.x format)"
  version = $stdin.gets.chomp

  increment_version_code
  update_version(version)

  sh "./gradlew clean :BraintreeData:uploadArchives :BraintreeApi:uploadArchives :Drop-In:uploadArchives"
  sh "./gradlew :BraintreeApi:closeRepository"
  puts "Sleeping for one minute to allow closing to finish"
  sleep 60
  sh "./gradlew :BraintreeApi:promoteRepository"

  puts "\nArchives are uploaded! Committing and tagging #{version} and preparing for the next development iteration"
  sh "git commit -am 'Release #{version}'"
  sh "git tag #{version} -am '#{version}'"

  update_version("#{version}-SNAPSHOT")
  sh "git commit -am 'Prepare for development'"

  puts "\nDone. Commits and tags have been created. If everything appears to be in order, hit ENTER to push."
  $stdin.gets

  sh "git push origin 1.x #{version}"

  puts "\nPushed to GHE! Press ENTER to push to public Github."
  $stdin.gets

  sh "git push github 1.x #{version}"

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
