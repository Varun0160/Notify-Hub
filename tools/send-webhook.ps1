param(
  [Parameter(Mandatory=$true)][string]$EventId,
  [Parameter(Mandatory=$true)][string]$MessageId,
  [string]$Type = "delivered",
  [int]$AgeSeconds = 0,
  [switch]$Tamper,
  [string]$Secret = "dev-webhook-secret-change-me-at-least-32-bytes!!",
  [string]$Url = "http://localhost:8080/api/v1/webhooks/provider"
)

$body = '{"eventId":"' + $EventId + '","providerMessageId":"' + $MessageId + '","type":"' + $Type + '"}'
$ts = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds() - $AgeSeconds

$hmac = New-Object System.Security.Cryptography.HMACSHA256
$hmac.Key = [Text.Encoding]::UTF8.GetBytes($Secret)
$sig = ($hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes("$ts.$body")) |
        ForEach-Object { $_.ToString("x2") }) -join ""

# -Tamper: sign the original body, then change it before sending (use it with -Type delivered)
if ($Tamper) { $body = $body.Replace($Type, "bounced") }

# Write the body to a file so curl sends the exact bytes (PowerShell mangles quotes otherwise)
$file = Join-Path $env:TEMP "webhook-body.json"
[IO.File]::WriteAllText($file, $body)

curl.exe -s -i -X POST $Url -H "Content-Type: application/json" -H "X-Timestamp: $ts" -H "X-Signature: $sig" --data-binary "@$file"