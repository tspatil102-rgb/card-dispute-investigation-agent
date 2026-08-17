# Quick test of the Card Dispute Investigation API

$baseUrl = "http://localhost:8080/api/disputes"

Write-Host "========================================"
Write-Host "Card Dispute Investigation API - Test"
Write-Host "========================================"; Write-Host ""

# Test 1: Create Dispute
Write-Host "TEST 1: Creating Dispute Case..." -ForegroundColor Cyan
$body = @{ customerId = 1001; complaintText = "I did not make a transaction of 75000 at Electronics World. This is unauthorized!" } | ConvertTo-Json
$response = Invoke-WebRequest -Uri $baseUrl -Method POST -Body $body -ContentType "application/json" -UseBasicParsing
$case = $response.Content | ConvertFrom-Json
Write-Host "✓ Case Created: $($case.caseId)" -ForegroundColor Green
Write-Host "  Status: $($case.status)"
Write-Host "  Customer ID: $($case.customerId)`n"

# Test 2: Get Case Details
$caseId = $case.caseId
Write-Host "TEST 2: Getting Case Details for $caseId..." -ForegroundColor Cyan
$response = Invoke-WebRequest -Uri "$baseUrl/$caseId" -Method GET -UseBasicParsing
$details = $response.Content | ConvertFrom-Json
Write-Host "✓ Case Retrieved" -ForegroundColor Green
Write-Host "  Status: $($details.status)`n"

# Test 3: Investigate Case
Write-Host "TEST 3: Investigating Case..." -ForegroundColor Cyan
$response = Invoke-WebRequest -Uri "$baseUrl/$caseId/investigate" -Method POST -ContentType "application/json" -UseBasicParsing
$investigation = $response.Content | ConvertFrom-Json
Write-Host "✓ Investigation Completed" -ForegroundColor Green
Write-Host "  Risk Score: $($investigation.riskResult.riskScore)"
Write-Host "  Risk Band: $($investigation.riskResult.riskBand)"
Write-Host "  Recommendation: $($investigation.recommendation.decision)"
Write-Host "  Confidence: $($investigation.recommendation.confidence)`n"

# Test 4: Get Metrics
Write-Host "TEST 4: Retrieving System Metrics..." -ForegroundColor Cyan
$response = Invoke-WebRequest -Uri "http://localhost:8080/api/metrics/disputes" -Method GET -UseBasicParsing
$metrics = $response.Content | ConvertFrom-Json
Write-Host "✓ Metrics Retrieved" -ForegroundColor Green
Write-Host "  Total Cases: $($metrics.totalCases)"
Write-Host "  High Risk Cases: $($metrics.highRiskCases)"
Write-Host "  Medium Risk Cases: $($metrics.mediumRiskCases)"
Write-Host "  Low Risk Cases: $($metrics.lowRiskCases)"
Write-Host "  Average Risk Score: $([Math]::Round($metrics.averageRiskScore, 2))`n"

# Test 5: Create Low-Risk Case
Write-Host "TEST 5: Creating Low-Risk Case..." -ForegroundColor Cyan
$body2 = @{ customerId = 1002; complaintText = "I don't recognize this 1800 transaction at Amazon." } | ConvertTo-Json
$response2 = Invoke-WebRequest -Uri $baseUrl -Method POST -Body $body2 -ContentType "application/json" -UseBasicParsing
$case2 = $response2.Content | ConvertFrom-Json
Write-Host "✓ Case Created: $($case2.caseId)" -ForegroundColor Green

# Investigate second case
Write-Host "TEST 6: Investigating Low-Risk Case..." -ForegroundColor Cyan
$response3 = Invoke-WebRequest -Uri "$baseUrl/$($case2.caseId)/investigate" -Method POST -ContentType "application/json" -UseBasicParsing
$investigation2 = $response3.Content | ConvertFrom-Json
Write-Host "✓ Investigation Completed" -ForegroundColor Green
Write-Host "  Risk Score: $($investigation2.riskResult.riskScore)"
Write-Host "  Risk Band: $($investigation2.riskResult.riskBand)"
Write-Host "  Recommendation: $($investigation2.recommendation.decision)`n"

Write-Host "========================================"
Write-Host "✓ All Tests Completed Successfully!"
Write-Host "========================================"
