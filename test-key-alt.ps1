Write-Host "=== Testing with gemini-1.5-flash (alternative model) ===" -ForegroundColor Cyan

$apiKey = 'AQ.Ab8RN6Kv7cuAd7h9FYSxrWDp0hX37r1Vn250NkSHagpABwyzcg'
$body = '{"contents":[{"parts":[{"text":"Analyze this fraud dispute: Customer charged $500 for meal, receipt shows $50. What type of fraud?"}]}]}'
$uri = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent'
$headers = @{ 'X-goog-api-key' = $apiKey; 'Content-Type' = 'application/json' }

Write-Host "Testing with gemini-1.5-flash model..." -ForegroundColor Yellow

try {
  $response = Invoke-WebRequest -Uri $uri -Method POST -Headers $headers -Body $body -UseBasicParsing -TimeoutSec 30
  Write-Host "✅ SUCCESS! Key is valid and working!" -ForegroundColor Green
  Write-Host "Status Code: $($response.StatusCode)" -ForegroundColor Green
  Write-Host "`nGemini LLM Response:" -ForegroundColor Cyan
  $parsed = $response.Content | ConvertFrom-Json
  Write-Host $parsed.candidates[0].content.parts[0].text
  Write-Host ""
} catch {
  Write-Host "❌ Error: $($_.Exception.Message)" -ForegroundColor Red
  if ($_.Exception.Response) {
    Write-Host "Status Code: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
  }
}
