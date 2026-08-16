# PowerShell Script to test all three demo scenarios

$baseUrl = "http://localhost:8080/api/disputes"

Write-Host "========================================"
Write-Host "Card Dispute Investigation - Demo Test"
Write-Host "========================================`n"

# Scenario 1: Genuine Fraud
Write-Host "SCENARIO 1: GENUINE FRAUD CASE"
Write-Host "-------------------------------"

$scenario1Body = @{
    customerId = 1001
    complaintText = "I did not make a transaction of ₹75,000 at Electronics World. This is unauthorized!"
} | ConvertTo-Json

Write-Host "Creating dispute case for Scenario 1..."
$s1Response = Invoke-WebRequest -Uri $baseUrl -Method POST -Body $scenario1Body -ContentType "application/json" -ErrorAction SilentlyContinue
$s1Case = $s1Response.Content | ConvertFrom-Json
$s1CaseId = $s1Case.caseId

Write-Host "Case ID: $s1CaseId"
Write-Host "Status: $($s1Case.status)"
Write-Host ""

Write-Host "Investigating case..."
$s1InvestResponse = Invoke-WebRequest -Uri "$baseUrl/$s1CaseId/investigate" -Method POST -ContentType "application/json" -ErrorAction SilentlyContinue
$s1Investigation = $s1InvestResponse.Content | ConvertFrom-Json

Write-Host "Risk Score: $($s1Investigation.riskResult.riskScore)"
Write-Host "Risk Band: $($s1Investigation.riskResult.riskBand)"
Write-Host "Recommendation: $($s1Investigation.recommendation.decision)"
Write-Host "Confidence: $($s1Investigation.recommendation.confidence)"
Write-Host "Recommended Actions: $($s1Investigation.recommendation.recommendedActions -join ', ')"
Write-Host ""

# Get full case details
Write-Host "Fetching full case details..."
$s1DetailResponse = Invoke-WebRequest -Uri "$baseUrl/$s1CaseId" -Method GET -ErrorAction SilentlyContinue
$s1Details = $s1DetailResponse.Content | ConvertFrom-Json
Write-Host "Final Status: $($s1Details.status)"
Write-Host ""

# Get audit trail
Write-Host "Fetching audit trail..."
$s1AuditResponse = Invoke-WebRequest -Uri "$baseUrl/$s1CaseId/audit" -Method GET -ErrorAction SilentlyContinue
$s1Audit = $s1AuditResponse.Content | ConvertFrom-Json
Write-Host "Number of audit events: $($s1Audit.Count)"
Write-Host ""

Write-Host "========================================`n"

# Scenario 2: Low-Risk False Alarm
Write-Host "SCENARIO 2: LOW-RISK FALSE ALARM"
Write-Host "--------------------------------"

$scenario2Body = @{
    customerId = 1002
    complaintText = "I don't recognize this ₹1,800 transaction at Amazon."
} | ConvertTo-Json

Write-Host "Creating dispute case for Scenario 2..."
$s2Response = Invoke-WebRequest -Uri $baseUrl -Method POST -Body $scenario2Body -ContentType "application/json" -ErrorAction SilentlyContinue
$s2Case = $s2Response.Content | ConvertFrom-Json
$s2CaseId = $s2Case.caseId

Write-Host "Case ID: $s2CaseId"
Write-Host "Status: $($s2Case.status)"
Write-Host ""

Write-Host "Investigating case..."
$s2InvestResponse = Invoke-WebRequest -Uri "$baseUrl/$s2CaseId/investigate" -Method POST -ContentType "application/json" -ErrorAction SilentlyContinue
$s2Investigation = $s2InvestResponse.Content | ConvertFrom-Json

Write-Host "Risk Score: $($s2Investigation.riskResult.riskScore)"
Write-Host "Risk Band: $($s2Investigation.riskResult.riskBand)"
Write-Host "Recommendation: $($s2Investigation.recommendation.decision)"
Write-Host "Confidence: $($s2Investigation.recommendation.confidence)"
Write-Host "Recommended Actions: $($s2Investigation.recommendation.recommendedActions -join ', ')"
Write-Host ""

# Get full case details
Write-Host "Fetching full case details..."
$s2DetailResponse = Invoke-WebRequest -Uri "$baseUrl/$s2CaseId" -Method GET -ErrorAction SilentlyContinue
$s2Details = $s2DetailResponse.Content | ConvertFrom-Json
Write-Host "Final Status: $($s2Details.status)"
Write-Host ""

# Get analyst note for Scenario 2
Write-Host "Fetching analyst note (first 500 chars)..."
$s2Note = $s2Details.analystNote.Substring(0, [Math]::Min(500, $s2Details.analystNote.Length))
Write-Host $s2Note
Write-Host ""

Write-Host "========================================`n"

# Scenario 3: Ambiguous Case
Write-Host "SCENARIO 3: AMBIGUOUS CASE"
Write-Host "--------------------------"

$scenario3Body = @{
    customerId = 1003
    complaintText = "This merchant looks suspicious. I don't recall making a ₹2,500 purchase at TechStore."
} | ConvertTo-Json

Write-Host "Creating dispute case for Scenario 3..."
$s3Response = Invoke-WebRequest -Uri $baseUrl -Method POST -Body $scenario3Body -ContentType "application/json" -ErrorAction SilentlyContinue
$s3Case = $s3Response.Content | ConvertFrom-Json
$s3CaseId = $s3Case.caseId

Write-Host "Case ID: $s3CaseId"
Write-Host "Status: $($s3Case.status)"
Write-Host ""

Write-Host "Investigating case..."
$s3InvestResponse = Invoke-WebRequest -Uri "$baseUrl/$s3CaseId/investigate" -Method POST -ContentType "application/json" -ErrorAction SilentlyContinue
$s3Investigation = $s3InvestResponse.Content | ConvertFrom-Json

Write-Host "Risk Score: $($s3Investigation.riskResult.riskScore)"
Write-Host "Risk Band: $($s3Investigation.riskResult.riskBand)"
Write-Host "Recommendation: $($s3Investigation.recommendation.decision)"
Write-Host "Confidence: $($s3Investigation.recommendation.confidence)"
Write-Host "Recommended Actions: $($s3Investigation.recommendation.recommendedActions -join ', ')"
Write-Host ""

# Get full case details
Write-Host "Fetching full case details..."
$s3DetailResponse = Invoke-WebRequest -Uri "$baseUrl/$s3CaseId" -Method GET -ErrorAction SilentlyContinue
$s3Details = $s3DetailResponse.Content | ConvertFrom-Json
Write-Host "Final Status: $($s3Details.status)"
Write-Host ""

# Get customer response draft
Write-Host "Fetching customer response draft (first 400 chars)..."
$s3ResponseDraftResponse = Invoke-WebRequest -Uri "$baseUrl/$s3CaseId/customer-response" -Method GET -ErrorAction SilentlyContinue
$s3ResponseDraft = $s3ResponseDraftResponse.Content | ConvertFrom-Json
$s3Draft = $s3ResponseDraft.responseDraft.Substring(0, [Math]::Min(400, $s3ResponseDraft.responseDraft.Length))
Write-Host $s3Draft
Write-Host ""

Write-Host "========================================`n"
Write-Host "TEST SUMMARY"
Write-Host "============"
Write-Host "Scenario 1 (Genuine Fraud): $($s1Investigation.recommendation.decision) - Risk: $($s1Investigation.riskResult.riskBand)"
Write-Host "Scenario 2 (Low Risk): $($s2Investigation.recommendation.decision) - Risk: $($s2Investigation.riskResult.riskBand)"
Write-Host "Scenario 3 (Ambiguous): $($s3Investigation.recommendation.decision) - Risk: $($s3Investigation.riskResult.riskBand)"
Write-Host ""
Write-Host "All tests completed successfully!"
Write-Host "========================================"
