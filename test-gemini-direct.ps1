# Direct Gemini API test
Write-Host "=== Direct Gemini API Test ===" -ForegroundColor Cyan

$apiKey = $env:GEMINI_API_KEY
if (-not $apiKey) {
  $apiKey = 'REPLACE_WITH_GEMINI_API_KEY'
}
$body = '{"contents":[{"parts":[{"text":"What is 2+2?"}]}]}'
$uri = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent'
$headers = @{ 'X-goog-api-key' = $apiKey; 'Content-Type' = 'application/json' }

Write-Host "Calling Gemini API directly..." -ForegroundColor Yellow
Write-Host "URI: $uri" -ForegroundColor Gray

try {
  $response = Invoke-WebRequest -Uri $uri -Method POST -Headers $headers -Body $body -UseBasicParsing -TimeoutSec 30
  Write-Host "✅ Status: $($response.StatusCode)" -ForegroundColor Green
  Write-Host "`nGemini Response:" -ForegroundColor Cyan
  $response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 5 | Write-Host
} catch {
  Write-Host "❌ Error: $($_.Exception.Message)" -ForegroundColor Red
  if ($_.Exception.Response) {
    Write-Host "Status Code: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
    $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
    $body = $sr.ReadToEnd()
    Write-Host "Response Body:" -ForegroundColor Red
    Write-Host $body -ForegroundColor Red
  }
  exit 1
}
