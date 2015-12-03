require 'pty'

TAGS = ["request_command"]
FILTER = "'#{TAGS.join('|')}'"

def setup
  `adb logcat -c`
end

def run
  begin
    PTY.spawn("adb logcat -v tag | grep -E #{FILTER}") do |stdin, stdout, pid|
      begin
        stdin.each do |line|
          if line.include?("request_command")
            handle_command_request(line.split(':')[1].strip)
          end
        end
      rescue Errno::EIO
        # adb exited, stopping
      end
    end
  rescue PTY::ChildExited
    # adb exited, stopping
  end
end

def handle_command_request(command)
  if command == 'install paypal wallet'
    `adb install -r #{__dir__}/../vendor/com.paypal.android.p2pmobile.apk > /dev/null`
  elsif command == 'uninstall paypal wallet'
    `adb uninstall com.paypal.android.p2pmobile > /dev/null`
  end
end

setup
run

