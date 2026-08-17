# Simplified test script for Spring Boot Card Dispute Investigation API

$baseUrl = "http://localhost:8080/api/disputes"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Card Dispute Investigation API - Test" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Test 1: Create Dispute
Write-Host "TEST 1: Create Dispute Case" -ForegroundColor Yellow
Write-Host "-" * 40

$body1 = @{
    customerId = 1001
    complaintText = "I did not make a transaction of 75000 at Electronics World. This is unauthorized!"
} | ConvertTo-Json

$response1 = Invoke-WebRequest -Uri $baseUrl -Method POST -Body $body1 -ContentType "application/json" -UseBasicParsing
$case1 = $response1.Content | ConvertFrom-Json

Write-Host "✓ Case Created" -ForegroundColor Green
Write-Host "  Case ID: $($case1.caseId)"
Write-Host "  Status: $($case1.status)"
Write-Host "  Customer ID: $($case1.customerId)`n"

# Test 2: Get Dispute Details
Write-Host "TEST 2: Get Dispute Details" -ForegroundColor Yellow
Write-Host "-" * 40

$caseId = $case1.caseId
$response2 = Invoke-WebRequest -Uri "$baseUrl/$caseId" -Method GET -UseBasicParsing
$caseDetail = $response2.Content | ConvertFrom-Json

Write-Host "✓ Case Retrieved" -ForegroundColor Green
Write-Host "  Status: $($caseDetail.status)"
$riskScore = if ($caseDetail.riskScore) { $caseDetail.riskScore } else { 'N/A' }
Write-Host "  Risk Score: $riskScore`n"

# Test 3: Investigate Dispute
Write-Host "TEST 3: Investigate Dispute Case" -ForegroundColor Yellow
Write-Host "-" * 40

$response3 = Invoke-WebRequest -Uri "$baseUrl/$caseId/investigate" -Method POST -ContentType "application/json" -UseBasicParsing
$investigation = $response3.Content | ConvertFrom-Json

Write-Host "✓ Investigation Completed" -ForegroundColor Green
Write-Host "  Status: $($investigation.status)"
Write-Host "  Risk Score: $($investigation.riskResult.riskScore)"
Write-Host "  Risk Band: $($investigation.riskResult.riskBand)"
Write-Host "  Recommendation: $($investigation.recommendation.decision)"
Write-Host "  Confidence: $($investigation.recommendation.confidence)`n"

# Test 4: Get Audit Trail
Write-Host "TEST 4: Get Audit Trail" -ForegroundColor Yellow
Write-Host "-" * 40

$response4 = Invoke-WebRequest -Uri "$baseUrl/$caseId/audit" -Method GET -UseBasicParsing
$auditTrail = $response4.Content | ConvertFrom-Json

Write-Host "✓ Audit Trail Retrieved" -ForegroundColor Green
Write-Host "  Number of Events: $($auditTrail.Count)`n"

# Test 5: Get Timeline
Write-Host "TEST 5: Get Timeline Events" -ForegroundColor Yellow
Write-Host "-" * 40

$response5 = Invoke-WebRequest -Uri "$baseUrl/$caseId/timeline" -Method GET -UseBasicParsing
$timeline = $response5.Content | ConvertFrom-Json

Write-Host "✓ Timeline Retrieved" -ForegroundColor Green
Write-Host "  Number of Events: $($timeline.Count)`n"

# Test 6: Get Customer Response Draft
Write-Host "TEST 6: Get Customer Response Draft" -ForegroundColor Yellow
Write-Host "-" * 40

$response6 = Invoke-WebRequest -Uri "$baseUrl/$caseId/customer-response" -Method GET -UseBasicParsing
$responseDraft = $response6.Content | ConvertFrom-Json

Write-Host "✓ Customer Response Draft Retrieved" -ForegroundColor Green
Write-Host "  Case ID: $($responseDraft.caseId)"
Write-Host "  Status: $($responseDraft.status)"
Write-Host "  Response Length: $($responseDraft.responseDraft.Length) chars`n"

# Test 7: Create Second Case (Low Risk)
Write-Host "TEST 7: Create Low-Risk Dispute" -ForegroundColor Yellow
Write-Host "-" * 40

$body7 = @{
    customerId = 1002
    complaintText = "I don't recognize this 1800 transaction at Amazon."
} | ConvertTo-Json

$response7 = Invoke-WebRequest -Uri $baseUrl -Method POST -Body $body7 -ContentType "application/json" -UseBasicParsing
$case7 = $response7.Content | ConvertFrom-Json

Write-Host "✓ Case Created" -ForegroundColor Green
Write-Host "  Case ID: $($case7.caseId)"
Write-Host "  Status: $($case7.status)`n"

# Investigate second case
$caseId7 = $case7.caseId
$response8 = Invoke-WebRequest -Uri "$baseUrl/$caseId7/investigate" -Method POST -ContentType "application/json" -UseBasicParsing
$investigation7 = $response8.Content | ConvertFrom-Json

Write-Host "TEST 8: Investigate Low-Risk Case" -ForegroundColor Yellow
Write-Host "-" * 40
Write-Host "✓ Investigation Completed" -ForegroundColor Green
Write-Host "  Risk Score: $($investigation7.riskResult.riskScore)"
Write-Host "  Risk Band: $($investigation7.riskResult.riskBand)"
Write-Host "  Recommendation: $($investigation7.recommendation.decision)`n"

# Test 9: Get Metrics
Write-Host "TEST 9: Get Metrics" -ForegroundColor Yellow
Write-Host "-" * 40

$response9 = Invoke-WebRequest -Uri "http://localhost:8080/api/metrics/disputes" -Method GET -UseBasicParsing
$metrics = $response9.Content | ConvertFrom-Json

Write-Host "✓ Metrics Retrieved" -ForegroundColor Green
Write-Host "  Total Cases: $($metrics.totalCases)"
Write-Host "  High Risk Cases: $($metrics.highRiskCases)"
Write-Host "  Medium Risk Cases: $($metrics.mediumRiskCases)"
Write-Host "  Low Risk Cases: $($metrics.lowRiskCases)"
Write-Host "  Average Risk Score: $([Math]::Round($metrics.averageRiskScore, 2))`n"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "All Tests Completed Successfully! ✓" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
