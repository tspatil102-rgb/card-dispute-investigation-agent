# Direct Gemini API test
Write-Host "=== Direct Gemini API Test ===" -ForegroundColor Cyan

$apiKey = 'AQ.Ab8RN6J-JYP9CFi2f0DbVVH0O7l9r387Qf9I0418sxSA9OW_bA'
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
