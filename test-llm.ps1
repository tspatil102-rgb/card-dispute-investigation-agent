Write-Host "=== LLM Test: Full Workflow ===" -ForegroundColor Cyan

# Step 1: Create a dispute case
Write-Host "`n[1/3] Creating dispute case..." -ForegroundColor Yellow
$createBody = @{
  customerId = 1001
  complaintText = "I was charged $245.67 for a restaurant meal but my receipt shows only $50.50. The merchant appears to have added an unauthorized high tip or misprocessed the transaction. Please investigate this fraud."
} | ConvertTo-Json -Depth 5

try {
  $createResp = Invoke-WebRequest -Uri 'http://localhost:8080/api/disputes' -Method Post -Body $createBody -ContentType 'application/json' -TimeoutSec 30 -UseBasicParsing
  $createData = $createResp.Content | ConvertFrom-Json
  $caseId = $createData.caseId
  Write-Host "✅ Dispute created with Case ID: $caseId" -ForegroundColor Green
} catch {
  Write-Host "❌ Error creating dispute: $($_.Exception.Message)" -ForegroundColor Red
  exit 1
}

# Step 2: Trigger LLM investigation
Write-Host "`n[2/3] Triggering LLM investigation..." -ForegroundColor Yellow
Write-Host "Calling POST /api/disputes/$caseId/investigate" -ForegroundColor Gray

Start-Sleep -Seconds 2

try {
  $investigateResp = Invoke-WebRequest -Uri "http://localhost:8080/api/disputes/$caseId/investigate" -Method Post -TimeoutSec 40 -UseBasicParsing
  Write-Host "✅ Status: $($investigateResp.StatusCode)" -ForegroundColor Green
  Write-Host "LLM Analysis Response:" -ForegroundColor Cyan
  $investigateResp.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10 | Write-Host
} catch {
  Write-Host "❌ Error: $($_.Exception.Message)" -ForegroundColor Red
  if ($_.Exception.Response) {
    Write-Host "Status: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
    $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
    Write-Host "Response body: $($sr.ReadToEnd())" -ForegroundColor Red
  }
}

# Step 3: Check logs
Write-Host "`n[3/3] App Logs (last 50 lines containing 'Gemini|LLM|investigation'):" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Gray

# Read the app log file and show relevant lines
if (Test-Path 'app.log') {
  Get-Content 'app.log' -Tail 100 | Select-String -Pattern 'Gemini|LLM|investigation|ERROR|Exception' | Tail -30 | ForEach-Object { Write-Host $_ -ForegroundColor Gray }
} else {
  Write-Host "(app.log not found - logs are streaming to console)" -ForegroundColor Gray
}

Write-Host "`nTest Complete!" -ForegroundColor Cyan
