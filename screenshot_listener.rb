require 'pty'

FILTER = "request_screenshot"
cmd = "adb logcat | grep #{FILTER}"
begin
  PTY.spawn(cmd) do |stdin, stdout, pid|
    `mkdir failed_test_screenshots`
    puts "Listening..."
    begin
      stdin.each do |line|
        screenshot_name = line.split(':')[1].strip
        puts "Taking screenshot #{screenshot_name}"
        `adb shell screencap -p | perl -pe 's/\\x0D\\x0A/\\x0A/g' > failed_test_screenshots/#{screenshot_name}.png`
      end
    rescue Errno::EIO
      "adb exited, stopping"
    end
  end
rescue PTY::ChildExited
  puts "adb exited, stopping"
end
