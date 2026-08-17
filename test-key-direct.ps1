# Direct Gemini API test with new key
Write-Host "=== Testing New Gemini API Key ===" -ForegroundColor Cyan

$apiKey = $env:GEMINI_API_KEY
if (-not $apiKey) {
  $apiKey = 'REPLACE_WITH_GEMINI_API_KEY'
}
$body = '{"contents":[{"parts":[{"text":"Analyze this fraud: Customer was charged $500 for a restaurant meal when the receipt was $50. What type of fraud is this?"}]}]}'
$uri = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent'
$headers = @{ 'X-goog-api-key' = $apiKey; 'Content-Type' = 'application/json' }

Write-Host "Testing Gemini API..." -ForegroundColor Yellow
Write-Host "Model: gemini-flash-latest" -ForegroundColor Gray

try {
  $response = Invoke-WebRequest -Uri $uri -Method POST -Headers $headers -Body $body -UseBasicParsing -TimeoutSec 30
  Write-Host "✅ Status: $($response.StatusCode)" -ForegroundColor Green
  Write-Host "`n✅ KEY IS VALID AND WORKING!" -ForegroundColor Green
  Write-Host "`nGemini Response:" -ForegroundColor Cyan
  $parsed = $response.Content | ConvertFrom-Json
  Write-Host $parsed | ConvertTo-Json -Depth 5
} catch {
  Write-Host "❌ Error: $($_.Exception.Message)" -ForegroundColor Red
  if ($_.Exception.Response) {
    Write-Host "Status Code: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
    $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
    Write-Host "Response: $($sr.ReadToEnd())" -ForegroundColor Red
  }
  exit 1
}
