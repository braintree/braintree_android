require 'pty'

TAGS = ["request_screenshot", "request_command"]

def get_filter
  "'#{TAGS.join('|')}'"
end

def setup
  puts `mkdir failed_test_screenshots`
  puts `rm -rf failed_test_screenshots/*`
  puts `adb logcat -c`
end

def handle_screenshot_request(screenshot_name)
  puts `adb shell screencap -p | perl -pe 's/\\x0D\\x0A/\\x0A/g' > failed_test_screenshots/#{screenshot_name}.png`
end

def handle_command_request(command)
  if command == 'install fakewallet'
    `./gradlew :FakeWallet:installDebug > /dev/null`
  elsif command == 'uninstall fakewallet'
    `adb uninstall com.braintreepayments.fake.wallet > /dev/null`
  end
end

def run
  puts "Listening..."
  puts "adb logcat | grep -E #{get_filter}"
  begin
    PTY.spawn("adb logcat | grep -E #{get_filter}") do |stdin, stdout, pid|
      begin
        stdin.each do |line|
          puts line
          if line.include?("request_screenshot")
            handle_screenshot_request(line.split(':').last.strip)
          elsif line.include?("request_command")
            handle_command_request(line.split(':').last.strip)
          end
        end
      rescue Errno::EIO
        puts "adb exited, stopping"
      end
    end
  rescue PTY::ChildExited
    puts "adb exited, stopping"
  end
end

setup
run
