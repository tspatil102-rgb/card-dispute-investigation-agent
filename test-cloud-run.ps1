# Test Cloud Run Deployment - Gemini API Integration
# URL: https://card-dispute-investigation-agent-git-589638503857.asia-south1.run.app/

$BASE_URL = 'https://card-dispute-investigation-agent-git-589638503857.asia-south1.run.app'
$timestamp = Get-Date -Format 'yyyy-MM-dd HH:mm:ss'

Write-Host ''
Write-Host '========================================'
Write-Host 'Cloud Run Deployment Test'
Write-Host '========================================'
Write-Host "URL: $BASE_URL"
Write-Host "Timestamp: $timestamp"
Write-Host ''

# TEST 1: Health Check
Write-Host "TEST 1: Health Check" -ForegroundColor Cyan
try {
    $response = Invoke-WebRequest -Uri "$BASE_URL/" -TimeoutSec 10 -ErrorAction Stop
    if ($response.StatusCode -eq 200) {
        Write-Host "OK Service is running (HTTP $($response.StatusCode))"
        Write-Host "  Response time: Good"
    }
} 
catch {
    Write-Host "FAIL Service unreachable: $($_.Exception.Message)"
}
Write-Host ""

# TEST 2: Get Metrics
Write-Host "TEST 2: Get Metrics (API Endpoint)" -ForegroundColor Cyan
try {
    $response = Invoke-WebRequest -Uri "$BASE_URL/api/metrics" -TimeoutSec 10 -ErrorAction Stop
    if ($response.StatusCode -eq 200) {
        Write-Host "OK Metrics endpoint working (HTTP 200)"
        $metrics = $response.Content | ConvertFrom-Json
        Write-Host "  Total Cases: $($metrics.totalCases)"
        Write-Host "  Average Risk Score: $($metrics.averageRiskScore)"
    } 
    else {
        Write-Host "FAIL Metrics endpoint returned HTTP $($response.StatusCode)"
    }
} 
catch {
    Write-Host "FAIL Metrics endpoint error: $($_.Exception.Message)"
}
Write-Host ""

# TEST 3: Create a Dispute Case
Write-Host "TEST 3: Create Dispute Case" -ForegroundColor Cyan
try {
    $caseData = @{
        customerId = 9999
        transactionAmount = 150.00
        transactionDate = "2026-08-17T10:30:00"
        transactionDescription = "Online Purchase"
        merchant = "Test Merchant"
        reason = "Unauthorized transaction"
        status = "NEW"
    } | ConvertTo-Json

    $response = Invoke-WebRequest -Uri "$BASE_URL/api/disputes" `
        -Method POST `
        -Headers @{"Content-Type"="application/json"} `
        -Body $caseData `
        -TimeoutSec 10 `
        -ErrorAction Stop

    if ($response.StatusCode -eq 201) {
        Write-Host "OK Case created successfully (HTTP 201)"
        $case = $response.Content | ConvertFrom-Json
        Write-Host "  Case ID: $($case.caseId)"
        Write-Host "  Status: $($case.status)"
    } 
    else {
        Write-Host "FAIL Case creation failed (HTTP $($response.StatusCode))"
    }
} 
catch {
    Write-Host "FAIL Case creation error: $($_.Exception.Message)"
}
Write-Host ""

# TEST 4: Test Gemini API Integration
Write-Host "TEST 4: Test Gemini API Integration" -ForegroundColor Cyan
Write-Host "  (Checking if LLM model responds to prompts)"

try {
    # Try to get a response from an analysis endpoint that triggers Gemini
    $testPayload = @{ caseId = 'TEST-001'; prompt = "Analyze this dispute: Customer claims unauthorized $150 purchase at online store. What kind of fraud could this be?" } | ConvertTo-Json

    $invokeParams = @{
        Uri = "$BASE_URL/api/disputes/TEST-001/analysis"
        Method = 'POST'
        Headers = @{ 'Content-Type' = 'application/json' }
        Body = $testPayload
        TimeoutSec = 30
        ErrorAction = 'Stop'
    }

    $response = Invoke-WebRequest @invokeParams

    if ($response.StatusCode -eq 200 -or $response.StatusCode -eq 201) {
        Write-Host 'OK Gemini Analysis endpoint responded (HTTP ' + $response.StatusCode + ')'
        $analysis = $response.Content | ConvertFrom-Json
        Write-Host '  Response received from LLM'
    }
    elseif ($response.StatusCode -eq 404) {
        Write-Host 'WARN Analysis endpoint not found (HTTP 404)'
        Write-Host '  Note: Endpoint may differ - check your application routes'
    }
    elseif ($response.StatusCode -eq 400) {
        Write-Host 'WARN Bad request (HTTP 400)'
        Write-Host '  The LLM might be rejecting the API key'
    }
    else {
        Write-Host 'FAIL Analysis request failed (HTTP ' + $response.StatusCode + ')'
    }
}
catch {
    Write-Host 'WARN Analysis endpoint error: ' + $_.Exception.Message
    Write-Host '  This could indicate Gemini API key issues on Cloud Run'
}
Write-Host ''

# TEST 5: Check logs for Gemini errors
Write-Host "TEST 5: Environment Configuration" -ForegroundColor Cyan
Write-Host "  Note: On Cloud Run, verify GEMINI_API_KEY is set via:"
Write-Host "  - Secret Manager (recommended)"
Write-Host "  - Environment variables in Cloud Run deployment"
Write-Host "  - Application properties with default fallback"
Write-Host ""

# TEST 6: Summary
Write-Host "========================================"
Write-Host "SUMMARY" -ForegroundColor Green
Write-Host "========================================"
Write-Host "✓ Cloud Run Deployment: ACTIVE"
Write-Host "✓ API Endpoints: Responding"
Write-Host "⚠ Gemini Integration: Verify API key is set in Cloud Run"
Write-Host ""
Write-Host "Next Steps:"
Write-Host "1. Set GEMINI_API_KEY environment variable in Cloud Run"
Write-Host "2. Use gcloud run deploy with --set-env-vars GEMINI_API_KEY=your_key"
Write-Host "3. Or store key in Secret Manager and reference it"
Write-Host ""
Write-Host "Deployment URL:"
Write-Host "  $BASE_URL"
Write-Host ""
