package com.braintreepayments.api.internal;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import javax.net.ssl.SSLException;

class BraintreeApiCertificate {

    private static final String CERTIFICATE =
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIFezCCBGOgAwIBAgIQCDUbS1vOwdqlZvpIYct7NTANBgkqhkiG9w0BAQsFADBw\n" +
        "MQswCQYDVQQGEwJVUzEVMBMGA1UEChMMRGlnaUNlcnQgSW5jMRkwFwYDVQQLExB3\n" +
        "d3cuZGlnaWNlcnQuY29tMS8wLQYDVQQDEyZEaWdpQ2VydCBTSEEyIEhpZ2ggQXNz\n" +
        "dXJhbmNlIFNlcnZlciBDQTAeFw0xNjEwMDYwMDAwMDBaFw0xODEwMTExMjAwMDBa\n" +
        "MIGOMQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTERMA8GA1UEBxMI\n" +
        "U2FuIEpvc2UxFTATBgNVBAoTDFBheVBhbCwgSW5jLjEeMBwGA1UECxMVQnJhaW50\n" +
        "cmVlIERldmVsb3BtZW50MSAwHgYDVQQDDBcqLmRldi5icmFpbnRyZWUtYXBpLmNv\n" +
        "bTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANVcV5YEonx6IorBXy7D\n" +
        "otbZ9avNyHRaCcEj810Q80be7usfmDenGbQYUJmH2XD354K/PR8ZwZz0BHZ3FwuW\n" +
        "hG0ZPOvns8XjV3oFr6dgk6emZpohnhJeys2L1ouWiXsYzhZGEMkeeEFkLp6oSaS5\n" +
        "3mka7d9i+rED2M8uKyjMoSFvwu7u8Pht9T7kXJiPH89X83dLQIPFaGdwqBQAnDpL\n" +
        "wt1s5eLTq6bsVf0cQ/GzG7tQMkqXxmTgzPIFdFwSuUYPGNF4A3Emgggc2XrYwPDR\n" +
        "4nObx00fINL7SS/92adLXu3gRqrygBK1w7OwyGsiLw7prmVPhM5vRV22ciWnfMFB\n" +
        "MbUCAwEAAaOCAfAwggHsMB8GA1UdIwQYMBaAFFFo/5CvAgd1PMzZZWRiohK4WXI7\n" +
        "MB0GA1UdDgQWBBRfOORkCeTaMCOHcPJMjqgDK1S5rDAiBgNVHREEGzAZghcqLmRl\n" +
        "di5icmFpbnRyZWUtYXBpLmNvbTAOBgNVHQ8BAf8EBAMCBaAwHQYDVR0lBBYwFAYI\n" +
        "KwYBBQUHAwEGCCsGAQUFBwMCMHUGA1UdHwRuMGwwNKAyoDCGLmh0dHA6Ly9jcmwz\n" +
        "LmRpZ2ljZXJ0LmNvbS9zaGEyLWhhLXNlcnZlci1nNS5jcmwwNKAyoDCGLmh0dHA6\n" +
        "Ly9jcmw0LmRpZ2ljZXJ0LmNvbS9zaGEyLWhhLXNlcnZlci1nNS5jcmwwTAYDVR0g\n" +
        "BEUwQzA3BglghkgBhv1sAQEwKjAoBggrBgEFBQcCARYcaHR0cHM6Ly93d3cuZGln\n" +
        "aWNlcnQuY29tL0NQUzAIBgZngQwBAgIwgYMGCCsGAQUFBwEBBHcwdTAkBggrBgEF\n" +
        "BQcwAYYYaHR0cDovL29jc3AuZGlnaWNlcnQuY29tME0GCCsGAQUFBzAChkFodHRw\n" +
        "Oi8vY2FjZXJ0cy5kaWdpY2VydC5jb20vRGlnaUNlcnRTSEEySGlnaEFzc3VyYW5j\n" +
        "ZVNlcnZlckNBLmNydDAMBgNVHRMBAf8EAjAAMA0GCSqGSIb3DQEBCwUAA4IBAQBi\n" +
        "i/MYTZPKx5tbUHlMb+cw4qW/Q5SkR1t7zzS6NRT4TqureFFjyP7m4yvF+FQ5y1L4\n" +
        "DTAc+4tAt2TyWA9ZWW9mxxCJ0v9Cw8CO6FNjs0cTdM1H6tOSdwB1B5WFmxg9vAeG\n" +
        "+F9a7fDt9PXsM7efuESlKrcFcsjJ6o4CcjZmPA7rfVTWzs5NhPjFYQ6s58jyKcIr\n" +
        "okdokuhTdg81BiBbH8Sy31A1kzwWamBN/GdepsD5PUmMtz9ioJ6Bsi/K46AyVv7F\n" +
        "FG9hgsjvGWyi4TZvEV/KCa00N64j929kusb9b6phAUsZkW5YgDgcfqRiIYI3M5Vz\n" +
        "exIyRhyqX/1t/PkOluih\n" +
        "-----END CERTIFICATE-----\n" +
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIEtjCCA56gAwIBAgIQDHmpRLCMEZUgkmFf4msdgzANBgkqhkiG9w0BAQsFADBs\n" +
        "MQswCQYDVQQGEwJVUzEVMBMGA1UEChMMRGlnaUNlcnQgSW5jMRkwFwYDVQQLExB3\n" +
        "d3cuZGlnaWNlcnQuY29tMSswKQYDVQQDEyJEaWdpQ2VydCBIaWdoIEFzc3VyYW5j\n" +
        "ZSBFViBSb290IENBMB4XDTEzMTAyMjEyMDAwMFoXDTI4MTAyMjEyMDAwMFowdTEL\n" +
        "MAkGA1UEBhMCVVMxFTATBgNVBAoTDERpZ2lDZXJ0IEluYzEZMBcGA1UECxMQd3d3\n" +
        "LmRpZ2ljZXJ0LmNvbTE0MDIGA1UEAxMrRGlnaUNlcnQgU0hBMiBFeHRlbmRlZCBW\n" +
        "YWxpZGF0aW9uIFNlcnZlciBDQTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoC\n" +
        "ggEBANdTpARR+JmmFkhLZyeqk0nQOe0MsLAAh/FnKIaFjI5j2ryxQDji0/XspQUY\n" +
        "uD0+xZkXMuwYjPrxDKZkIYXLBxA0sFKIKx9om9KxjxKws9LniB8f7zh3VFNfgHk/\n" +
        "LhqqqB5LKw2rt2O5Nbd9FLxZS99RStKh4gzikIKHaq7q12TWmFXo/a8aUGxUvBHy\n" +
        "/Urynbt/DvTVvo4WiRJV2MBxNO723C3sxIclho3YIeSwTQyJ3DkmF93215SF2AQh\n" +
        "cJ1vb/9cuhnhRctWVyh+HA1BV6q3uCe7seT6Ku8hI3UarS2bhjWMnHe1c63YlC3k\n" +
        "8wyd7sFOYn4XwHGeLN7x+RAoGTMCAwEAAaOCAUkwggFFMBIGA1UdEwEB/wQIMAYB\n" +
        "Af8CAQAwDgYDVR0PAQH/BAQDAgGGMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEF\n" +
        "BQcDAjA0BggrBgEFBQcBAQQoMCYwJAYIKwYBBQUHMAGGGGh0dHA6Ly9vY3NwLmRp\n" +
        "Z2ljZXJ0LmNvbTBLBgNVHR8ERDBCMECgPqA8hjpodHRwOi8vY3JsNC5kaWdpY2Vy\n" +
        "dC5jb20vRGlnaUNlcnRIaWdoQXNzdXJhbmNlRVZSb290Q0EuY3JsMD0GA1UdIAQ2\n" +
        "MDQwMgYEVR0gADAqMCgGCCsGAQUFBwIBFhxodHRwczovL3d3dy5kaWdpY2VydC5j\n" +
        "b20vQ1BTMB0GA1UdDgQWBBQ901Cl1qCt7vNKYApl0yHU+PjWDzAfBgNVHSMEGDAW\n" +
        "gBSxPsNpA/i/RwHUmCYaCALvY2QrwzANBgkqhkiG9w0BAQsFAAOCAQEAnbbQkIbh\n" +
        "hgLtxaDwNBx0wY12zIYKqPBKikLWP8ipTa18CK3mtlC4ohpNiAexKSHc59rGPCHg\n" +
        "4xFJcKx6HQGkyhE6V6t9VypAdP3THYUYUN9XR3WhfVUgLkc3UHKMf4Ib0mKPLQNa\n" +
        "2sPIoc4sUqIAY+tzunHISScjl2SFnjgOrWNoPLpSgVh5oywM395t6zHyuqB8bPEs\n" +
        "1OG9d4Q3A84ytciagRpKkk47RpqF/oOi+Z6Mo8wNXrM9zwR4jxQUezKcxwCmXMS1\n" +
        "oVWNWlZopCJwqjyBcdmdqEU79OX2olHdx3ti6G8MdOu42vi/hw15UJGQmxg7kVkn\n" +
        "8TUoE6smftX3eg==\n" +
        "-----END CERTIFICATE-----\n" +
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIKQzCCCSugAwIBAgIQAjmU3LHSxBNVhFWLiaR4uTANBgkqhkiG9w0BAQsFADB1\n" +
        "MQswCQYDVQQGEwJVUzEVMBMGA1UEChMMRGlnaUNlcnQgSW5jMRkwFwYDVQQLExB3\n" +
        "d3cuZGlnaWNlcnQuY29tMTQwMgYDVQQDEytEaWdpQ2VydCBTSEEyIEV4dGVuZGVk\n" +
        "IFZhbGlkYXRpb24gU2VydmVyIENBMB4XDTE2MTAwNzAwMDAwMFoXDTE4MTAxMjEy\n" +
        "MDAwMFowggEhMR0wGwYDVQQPDBRQcml2YXRlIE9yZ2FuaXphdGlvbjETMBEGCysG\n" +
        "AQQBgjc8AgEDEwJVUzEZMBcGCysGAQQBgjc8AgECEwhEZWxhd2FyZTEQMA4GA1UE\n" +
        "BRMHMzAxNDI2NzEWMBQGA1UECRMNMjIxMSBOIDFzdCBTdDEOMAwGA1UEERMFOTUx\n" +
        "MzExCzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpDYWxpZm9ybmlhMREwDwYDVQQHEwhT\n" +
        "YW4gSm9zZTEVMBMGA1UEChMMUGF5UGFsLCBJbmMuMR0wGwYDVQQLExRCcmFpbnRy\n" +
        "ZWUgUHJvZHVjdGlvbjErMCkGA1UEAxMicGF5bWVudHMuc2FuZGJveC5icmFpbnRy\n" +
        "ZWUtYXBpLmNvbTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAK0jlmYC\n" +
        "/VbsVKr1jt0/vTzw0tsVjVSV3YDgmTfp+aJIb7js6FWVwOYGhSvYXQ7dBxBFoM+R\n" +
        "F6JE8DMOfG/usHkxJ9gFxgzlBLQHtY8oOfCpI42VYZDvSP87JH16nSDOQ1xocQPU\n" +
        "VmzO0XgmRWvMaQQGcr/bvbjUYzOUHhc8oBukDXH/Cz3DpYPVJHITRVRndsoQv+5i\n" +
        "ikTOuJVfBjosnlfbaJQFFzL/md5sVt97mN4tLn4dC9koW07Ahy7D8SEUHtONm/hZ\n" +
        "yXIGFpWWphU4OLSWw2xJP4yujcBobi0iL5/zm2VyBoC2JZ8w8tFNS5M1jQ/tGl1a\n" +
        "eXHHApTZglctkT0CAwEAAaOCBh8wggYbMB8GA1UdIwQYMBaAFD3TUKXWoK3u80pg\n" +
        "CmXTIdT4+NYPMB0GA1UdDgQWBBSaxVQI6hV3sYLg4uUjrCX7O0f/ATCCAlQGA1Ud\n" +
        "EQSCAkswggJHgiJwYXltZW50cy5zYW5kYm94LmJyYWludHJlZS1hcGkuY29tgj5h\n" +
        "dG1vc3BoZXJlLXNhbmQtdXMtd2VzdC0yLnNhbmRib3guY29zbW9zLmJyYWludHJl\n" +
        "ZXBheW1lbnRzLmNvbYI+YXRtb3NwaGVyZS1zYW5kLXVzLWVhc3QtMS5zYW5kYm94\n" +
        "LmNvc21vcy5icmFpbnRyZWVwYXltZW50cy5jb22CPmF0bW9zcGhlcmUtc2FuZC11\n" +
        "cy13ZXN0LTEuc2FuZGJveC5jb3Ntb3MuYnJhaW50cmVlcGF5bWVudHMuY29tgihh\n" +
        "cGkuc2FuZGJveC5jb3Ntb3MuYnJhaW50cmVlcGF5bWVudHMuY29tgjtvcmlnaW4t\n" +
        "YXRtb3NwaGVyZS1zYW5kLnNhbmRib3guY29zbW9zLmJyYWludHJlZXBheW1lbnRz\n" +
        "LmNvbYIpYXRtb3NwaGVyZS1zYW5kLnNhbmRib3guYnJhaW50cmVlLWFwaS5jb22C\n" +
        "MG9yaWdpbi1hdG1vc3BoZXJlLXNhbmQuc2FuZGJveC5icmFpbnRyZWUtYXBpLmNv\n" +
        "bYIzYXRtb3NwaGVyZS1zYW5kLXVzLXdlc3QtMi5zYW5kYm94LmJyYWludHJlZS1h\n" +
        "cGkuY29tgjNhdG1vc3BoZXJlLXNhbmQtdXMtZWFzdC0xLnNhbmRib3guYnJhaW50\n" +
        "cmVlLWFwaS5jb22CM2F0bW9zcGhlcmUtc2FuZC11cy13ZXN0LTEuc2FuZGJveC5i\n" +
        "cmFpbnRyZWUtYXBpLmNvbTAOBgNVHQ8BAf8EBAMCBaAwHQYDVR0lBBYwFAYIKwYB\n" +
        "BQUHAwEGCCsGAQUFBwMCMHUGA1UdHwRuMGwwNKAyoDCGLmh0dHA6Ly9jcmwzLmRp\n" +
        "Z2ljZXJ0LmNvbS9zaGEyLWV2LXNlcnZlci1nMS5jcmwwNKAyoDCGLmh0dHA6Ly9j\n" +
        "cmw0LmRpZ2ljZXJ0LmNvbS9zaGEyLWV2LXNlcnZlci1nMS5jcmwwSwYDVR0gBEQw\n" +
        "QjA3BglghkgBhv1sAgEwKjAoBggrBgEFBQcCARYcaHR0cHM6Ly93d3cuZGlnaWNl\n" +
        "cnQuY29tL0NQUzAHBgVngQwBATCBiAYIKwYBBQUHAQEEfDB6MCQGCCsGAQUFBzAB\n" +
        "hhhodHRwOi8vb2NzcC5kaWdpY2VydC5jb20wUgYIKwYBBQUHMAKGRmh0dHA6Ly9j\n" +
        "YWNlcnRzLmRpZ2ljZXJ0LmNvbS9EaWdpQ2VydFNIQTJFeHRlbmRlZFZhbGlkYXRp\n" +
        "b25TZXJ2ZXJDQS5jcnQwDAYDVR0TAQH/BAIwADCCAfMGCisGAQQB1nkCBAIEggHj\n" +
        "BIIB3wHdAHUApLkJkLQYWBSHuxOizGdwCjw1mAT5G9+443fNDsgN3BAAAAFYLIN0\n" +
        "MAAABAMARjBEAiBJVV61qeX434qnGwGgUQlwdj+ZbAKoYZU3utXEY5MurQIgTQ3q\n" +
        "IpBF+A20Z0I7mAKQZ7EvypJ86hwN8b02lflytO0AdQBo9pj4H2SCvjqM7rkoHUz8\n" +
        "cVFdZ5PURNEKZ6y7T0/7xAAAAVgsg3QyAAAEAwBGMEQCID8ofQdfZLZFpglYdV7J\n" +
        "ekkkuQZ5FFtQ+sh3ahXC0G3zAiAZLEbY7CWJBknIo33KWx2nIUNzJf8X7qac2ArK\n" +
        "0al1QgB2AFYUBpov18Ls0/XhvUSyPsdGdrm8mRFcwO+UmFXWidDdAAABWCyDdLIA\n" +
        "AAQDAEcwRQIhAOA4AlPexM+U7DxZNQOXeT3V+HQNYQMZlo6RCOLEN9/aAiAJt26r\n" +
        "DnSCMu5s7FynEPRTdaxm5XsO4pVPkpo3kYv5cQB1AO5Lvbd1zmC64UJpH6vhnmaj\n" +
        "D35fsHLYgwDEe4l6qP3LAAABWCyDdkoAAAQDAEYwRAIgTvG8I5Sr8hrGe/YT4wcl\n" +
        "l88RMv5MD2ARokTASfXydsYCIARDqyYuagO+ff/1JD25V2dvvC40l2DfXRxuiIxO\n" +
        "MJjDMA0GCSqGSIb3DQEBCwUAA4IBAQB6WQbhlHGqN5bIzl4+9ZbqjPqzHZCSpjis\n" +
        "0Ot1rVN5Lc4RKL4RY69cXLERQiRXkg7Z8AynPbACVk7eR0DgtYWBKvFGCVjHII9d\n" +
        "1Rw8ipHPfkVWeamfSRguSRtOZhsb2M8U2vcSpN9J3cyweFiBW1xdUqD/oXAYV8D6\n" +
        "CkPa1mnI2g7vVq2d66WZmpsDtY1EwUya7vmZTxw7BFlntV7lrVxPLhUbq1wJGIbT\n" +
        "/++w4xnhHveMA5McQqgld1uyrc4R8j8aKBCMbiKh5cYkUDp7xWOkEL1WPHEYx2DH\n" +
        "uEnXgUGZxnLYZfCNsC/UAQxvKKpxBLWus/n1InM/97HfA31WJsvQ\n" +
        "-----END CERTIFICATE-----\n" +
        "-----BEGIN CERTIFICATE-----\n" +
        "MIIKGjCCCQKgAwIBAgIQAVW8CEsvQA7LE5GWnT6icDANBgkqhkiG9w0BAQsFADB1\n" +
        "MQswCQYDVQQGEwJVUzEVMBMGA1UEChMMRGlnaUNlcnQgSW5jMRkwFwYDVQQLExB3\n" +
        "d3cuZGlnaWNlcnQuY29tMTQwMgYDVQQDEytEaWdpQ2VydCBTSEEyIEV4dGVuZGVk\n" +
        "IFZhbGlkYXRpb24gU2VydmVyIENBMB4XDTE2MTAwNTAwMDAwMFoXDTE4MTAxMDEy\n" +
        "MDAwMFowggEZMR0wGwYDVQQPDBRQcml2YXRlIE9yZ2FuaXphdGlvbjETMBEGCysG\n" +
        "AQQBgjc8AgEDEwJVUzEZMBcGCysGAQQBgjc8AgECEwhEZWxhd2FyZTEQMA4GA1UE\n" +
        "BRMHMzAxNDI2NzEWMBQGA1UECRMNMjIxMSBOIDFzdCBTdDEOMAwGA1UEERMFOTUx\n" +
        "MzExCzAJBgNVBAYTAlVTMRMwEQYDVQQIEwpDYWxpZm9ybmlhMREwDwYDVQQHEwhT\n" +
        "YW4gSm9zZTEVMBMGA1UEChMMUGF5UGFsLCBJbmMuMR0wGwYDVQQLExRCcmFpbnRy\n" +
        "ZWUgUHJvZHVjdGlvbjEjMCEGA1UEAxMacGF5bWVudHMuYnJhaW50cmVlLWFwaS5j\n" +
        "b20wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQD6e33w3lDT7mnkB2MA\n" +
        "iaUF4/FEtWeftDpozH1jj9g44qctRkCV2NUEhche5X35oIpZ9R+grkOrE9sVfTU/\n" +
        "R62jtqJ+TjhsPDSCahaXZHO81WAhagxKbsC7QrYJ9KDKdOXh/k8xLrP4UV7QSlUk\n" +
        "lJ7cMUMeNiI8nMxI7QzMLwd4CAMSU4K2zRDw8fBugbN98Yq1pqzCRnwzd/4aaw7b\n" +
        "uQj0ecDG3NAFatRXwW9iYL1s8HazURSI7dY9GoQKVNXksMdIbMVTOjK/c0+cUTpu\n" +
        "CNCBQlBjo/AixAzyFjpeeSN47wfscRH/HaR/mi5M3gxGqUFc/GwgexFyFiCJKB/G\n" +
        "iMLrAgMBAAGjggX+MIIF+jAfBgNVHSMEGDAWgBQ901Cl1qCt7vNKYApl0yHU+PjW\n" +
        "DzAdBgNVHQ4EFgQUm2lgFJIkeBL2ToCw0A1deaMXtHEwggIxBgNVHREEggIoMIIC\n" +
        "JIIacGF5bWVudHMuYnJhaW50cmVlLWFwaS5jb22CM29yaWdpbi1hdG1vc3BoZXJl\n" +
        "LXByb2QucHJvZHVjdGlvbi5icmFpbnRyZWUtYXBpLmNvbYI2YXRtb3NwaGVyZS1w\n" +
        "cm9kLXVzLXdlc3QtMi5wcm9kdWN0aW9uLmJyYWludHJlZS1hcGkuY29tgjZhdG1v\n" +
        "c3BoZXJlLXByb2QtdXMtZWFzdC0xLnByb2R1Y3Rpb24uYnJhaW50cmVlLWFwaS5j\n" +
        "b22CNmF0bW9zcGhlcmUtcHJvZC11cy13ZXN0LTEucHJvZHVjdGlvbi5icmFpbnRy\n" +
        "ZWUtYXBpLmNvbYIgYXBpLmNvc21vcy5icmFpbnRyZWVwYXltZW50cy5jb22CPm9y\n" +
        "aWdpbi1hdG1vc3BoZXJlLXByb2QucHJvZHVjdGlvbi5jb3Ntb3MuYnJhaW50cmVl\n" +
        "cGF5bWVudHMuY29tgkFhdG1vc3BoZXJlLXByb2QtdXMtd2VzdC0yLnByb2R1Y3Rp\n" +
        "b24uY29zbW9zLmJyYWludHJlZXBheW1lbnRzLmNvbYJBYXRtb3NwaGVyZS1wcm9k\n" +
        "LXVzLWVhc3QtMS5wcm9kdWN0aW9uLmNvc21vcy5icmFpbnRyZWVwYXltZW50cy5j\n" +
        "b22CQWF0bW9zcGhlcmUtcHJvZC11cy13ZXN0LTEucHJvZHVjdGlvbi5jb3Ntb3Mu\n" +
        "YnJhaW50cmVlcGF5bWVudHMuY29tMA4GA1UdDwEB/wQEAwIFoDAdBgNVHSUEFjAU\n" +
        "BggrBgEFBQcDAQYIKwYBBQUHAwIwdQYDVR0fBG4wbDA0oDKgMIYuaHR0cDovL2Ny\n" +
        "bDMuZGlnaWNlcnQuY29tL3NoYTItZXYtc2VydmVyLWcxLmNybDA0oDKgMIYuaHR0\n" +
        "cDovL2NybDQuZGlnaWNlcnQuY29tL3NoYTItZXYtc2VydmVyLWcxLmNybDBLBgNV\n" +
        "HSAERDBCMDcGCWCGSAGG/WwCATAqMCgGCCsGAQUFBwIBFhxodHRwczovL3d3dy5k\n" +
        "aWdpY2VydC5jb20vQ1BTMAcGBWeBDAEBMIGIBggrBgEFBQcBAQR8MHowJAYIKwYB\n" +
        "BQUHMAGGGGh0dHA6Ly9vY3NwLmRpZ2ljZXJ0LmNvbTBSBggrBgEFBQcwAoZGaHR0\n" +
        "cDovL2NhY2VydHMuZGlnaWNlcnQuY29tL0RpZ2lDZXJ0U0hBMkV4dGVuZGVkVmFs\n" +
        "aWRhdGlvblNlcnZlckNBLmNydDAMBgNVHRMBAf8EAjAAMIIB9QYKKwYBBAHWeQIE\n" +
        "AgSCAeUEggHhAd8AdgCkuQmQtBhYFIe7E6LMZ3AKPDWYBPkb37jjd80OyA3cEAAA\n" +
        "AVe56WY1AAAEAwBHMEUCIQDszb6745Y7gUjYnqI9rF+5mnn5Nj8Cm7+0kyqO6S2L\n" +
        "IwIgHIWqMuMoa39BWHlOc6SgDkUiIJnANWItm7Rq9EPJp4YAdgBo9pj4H2SCvjqM\n" +
        "7rkoHUz8cVFdZ5PURNEKZ6y7T0/7xAAAAVe56WW1AAAEAwBHMEUCIAcl6FvKvDaM\n" +
        "hlAsEOt1Uw15AgHLh1JlWNyiD0iT0rofAiEA4HyYodql1mSyk6QfiqtCGfebG395\n" +
        "9jk4phxID2VMYLYAdQBWFAaaL9fC7NP14b1Esj7HRna5vJkRXMDvlJhV1onQ3QAA\n" +
        "AVe56WYdAAAEAwBGMEQCIFby3owU/4IhFx6B774LSIW2EDFC/CPE2NQu+TWDFp9U\n" +
        "AiA+dIquHnVefbJTGl6GnPauZQt6cDSgkDBqh8zaZ8GuVAB2AO5Lvbd1zmC64UJp\n" +
        "H6vhnmajD35fsHLYgwDEe4l6qP3LAAABV7npaDgAAAQDAEcwRQIgIdHHnOBp7bBj\n" +
        "795x3UPNKlpx/STPIyMY6q4gdkOoFs8CIQCXk59kGUCiSqv4lHSjxum/hvmsiELc\n" +
        "tE33Q7BREEzc0DANBgkqhkiG9w0BAQsFAAOCAQEAgqNZNoJp6UcCEghEtDiDMtY7\n" +
        "gv1ooxe3VIHfTfYel9bClpBVKmvB8nMSWeuo17eXBYaQFCTyugCKmmgO6OI4t93m\n" +
        "Q3r8FODNMhdVm7Qs2A/tB3zQaaryyesPhAFYfrqN8BsVx0DmAzXzimRaqVImR9GX\n" +
        "QREKMylfZYWNHBUOlt6c8eiqOX4lGG8kOUhbiKhTsiYdWh+w3mDGhLDUlGcHAzOY\n" +
        "T0L45y5RTkAsfJbCQARtv1M6l7DR8c3PnpwOwwLxwyc7zlTV4Z8R3oJKH6np55og\n" +
        "FdRx/54K4cOxblP9SRXsnNwoZjETJgewCyk9F/YER1wpKqR9u1RhORkw0Afzzg==\n" +
        "-----END CERTIFICATE-----\n";

    static InputStream getCertInputStream() throws SSLException {
        try {
            return new ByteArrayInputStream(CERTIFICATE.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new SSLException(e.getMessage());
        }
    }
}
