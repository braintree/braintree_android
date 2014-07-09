#!/usr/bin/env ruby
require 'webrick'
require 'webrick/https'
require 'openssl'

private_key_file = "#{__dir__}/privateKey.key"
cert_file = "#{__dir__}/certificate.crt"

pkey = OpenSSL::PKey::RSA.new(File.read(private_key_file))
cert = OpenSSL::X509::Certificate.new(File.read(cert_file))

pid_file = ARGV[0]

s = WEBrick::HTTPServer.new(
  :BindAddress => '0.0.0.0',
  :Port => 9443,
  :Logger => WEBrick::Log::new(nil, WEBrick::Log::ERROR),
  :DocumentRoot => File.join(File.dirname(__FILE__)),
  :ServerType => WEBrick::Daemon,
  :SSLEnable => true,
  :SSLVerifyClient => OpenSSL::SSL::VERIFY_NONE,
  :SSLCertificate => cert,
  :SSLPrivateKey => pkey,
  :SSLCertName => [ [ "CN",WEBrick::Utils::getservername ] ],
  :StartCallback => proc { File.open(pid_file, "w") { |f| f.write $$.to_s }}
)
trap("INT"){ s.shutdown }
s.start
