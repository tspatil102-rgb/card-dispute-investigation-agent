$body = @{
  customerId = 1001
  transactionAmount = 85.50
  transactionDate = '2026-08-15T14:30:00'
  transactionDescription = 'Unauthorized Online Purchase'
  merchant = 'Unknown Electronics Store'
  reason = 'I did not authorize this purchase. It appeared on my statement without my knowledge.'
  status = 'NEW'
} | ConvertTo-Json -Depth 5
try {
  $resp = Invoke-RestMethod -Uri 'http://localhost:8080/api/disputes' -Method Post -Body $body -ContentType 'application/json' -ErrorAction Stop
  Write-Host 'RESPONSE'
  $resp | ConvertTo-Json -Depth 5
} catch {
  Write-Host 'ERROR'
  if ($_.Exception.Response) {
    $r = $_.Exception.Response
    Write-Host "StatusCode: $($r.StatusCode.value__)"
    Write-Host "StatusDescription: $($r.StatusDescription)"
    $sr = New-Object System.IO.StreamReader($r.GetResponseStream())
    Write-Host "Body:`n$($sr.ReadToEnd())"
  } else {
    Write-Host $_.Exception.Message
  }
  exit 1
}