require 'rake'

task :default => :tests

task :tests do
  output = `adb devices`
  if output.match(/device$/)
    begin
      sh "ruby script/httpsd.rb /tmp/httpsd.pid"
      sh "./gradlew --info clean runAllTests connectedAndroidTest"
    ensure
      `kill -9 \`cat /tmp/httpsd.pid\``
    end
  else
    puts "Please connect a device or start an emulator and try again"
    exit 1
  end
end
