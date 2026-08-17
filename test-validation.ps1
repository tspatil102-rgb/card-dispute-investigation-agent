# Test 1: Missing complaintText (should return 400)
Write-Host "=== TEST 1: Missing complaintText (expect 400) ===" -ForegroundColor Yellow
$badBody = @{ customerId = 1001 } | ConvertTo-Json -Depth 5
try {
  $resp = Invoke-WebRequest -Uri 'http://localhost:8080/api/disputes' -Method Post -Body $badBody -ContentType 'application/json' -ErrorAction Stop
  Write-Host "Status: $($resp.StatusCode)" -ForegroundColor Green
  Write-Host $resp.Content
} catch {
  Write-Host "Status: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
  $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
  $body = $sr.ReadToEnd()
  Write-Host "Response Body:"
  Write-Host $body
}

Start-Sleep -Seconds 2

# Test 2: Valid payload with complaintText (should return 201)
Write-Host "`n=== TEST 2: Valid payload with complaintText (expect 201) ===" -ForegroundColor Yellow
$goodBody = @{
  customerId = 1001
  complaintText = "Unauthorized Online Purchase - I did not authorize this purchase."
} | ConvertTo-Json -Depth 5

try {
  $resp = Invoke-WebRequest -Uri 'http://localhost:8080/api/disputes' -Method Post -Body $goodBody -ContentType 'application/json' -ErrorAction Stop
  Write-Host "Status: $($resp.StatusCode)" -ForegroundColor Green
  Write-Host "Response Body:"
  Write-Host $resp.Content
} catch {
  Write-Host "Status: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
  $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
  $body = $sr.ReadToEnd()
  Write-Host "Response Body:"
  Write-Host $body
}
