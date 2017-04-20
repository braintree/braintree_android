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

def handle_command_request(raw_command)
  command = raw_command.split(' ')[0].strip
  options = raw_command.split(' ')[1].strip
  if command == "install"
    `adb install -r #{__dir__}/../vendor/#{options}.apk > /dev/null`
  elsif command == "uninstall"
    `adb uninstall #{options} > /dev/null`
  end
end

setup
run

