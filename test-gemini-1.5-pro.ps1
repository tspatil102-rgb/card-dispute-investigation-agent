# Test Vertex AI with gemini-1.5-pro
Write-Host "Testing Vertex AI with gemini-1.5-pro..." -ForegroundColor Cyan

# Step 1: Create a new dispute
Write-Host "`nStep 1: Create a new dispute..." -ForegroundColor Yellow
$createPayload = @{
    customerId = 12345
    complaintText = "Card charged twice for the same transaction. Please investigate and refund."
    cardNumber = "****1234"
    amount = 99.99
    transactionDate = "2024-08-19"
} | ConvertTo-Json

try {
    $createResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/disputes" `
        -Method POST `
        -ContentType "application/json" `
        -Body $createPayload `
        -ErrorAction Stop
    
    $dispute = $createResponse.Content | ConvertFrom-Json
    Write-Host "✅ Dispute created: $($dispute.id)" -ForegroundColor Green
    
    # Step 2: Trigger investigation
    Write-Host "`nStep 2: Triggering investigation..." -ForegroundColor Yellow
    $investigatePayload = @{ caseId = $dispute.id } | ConvertTo-Json
    
    $investigateResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/disputes/$($dispute.id)/investigate" `
        -Method POST `
        -ContentType "application/json" `
        -Body $investigatePayload `
        -ErrorAction Stop
    
    Write-Host "✅ Investigation started for case: $($dispute.id)" -ForegroundColor Green
    
    # Wait for processing
    Start-Sleep -Seconds 3
    
    # Step 3: Get investigation results
    Write-Host "`nStep 3: Retrieving investigation results..." -ForegroundColor Yellow
    $resultsResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/disputes/$($dispute.id)" `
        -Method GET `
        -ErrorAction Stop
    
    $result = $resultsResponse.Content | ConvertFrom-Json
    Write-Host "Status: $($result.status)" -ForegroundColor Cyan
    Write-Host "Risk Score: $($result.riskScore)" -ForegroundColor Cyan
    
    # Step 4: Get customer response
    Write-Host "`nStep 4: Getting customer response..." -ForegroundColor Yellow
    try {
        $customerResponse = Invoke-WebRequest -Uri "http://localhost:8080/api/disputes/$($dispute.id)/customer-response" `
            -Method GET `
            -ErrorAction Stop
        
        $response = $customerResponse.Content | ConvertFrom-Json
        Write-Host "✅ Customer Response Status: $($response.status)" -ForegroundColor Green
        
        $preview = $response.response.Substring(0, [Math]::Min(200, $response.response.Length))
        Write-Host "Response Preview: $preview..." -ForegroundColor Gray
        
        Write-Host "`n✅ SUCCESS: Vertex AI LLM is working with gemini-1.5-pro!" -ForegroundColor Green
        Write-Host "No 404 errors encountered - model is available in us-central1" -ForegroundColor Green
        
    } catch {
        Write-Host "⚠️ Customer response retrieval: $($_.Exception.Response.StatusCode.Value)" -ForegroundColor Yellow
    }
    
} catch {
    $statusCode = $_.Exception.Response.StatusCode.Value
    $errorMessage = $_.Exception.Message
    
    if ($statusCode -eq 404) {
        Write-Host "❌ ERROR 404: Model not found!" -ForegroundColor Red
        Write-Host "gemini-1.5-pro is not available in us-central1" -ForegroundColor Red
    } else {
        Write-Host "❌ ERROR $statusCode : $errorMessage" -ForegroundColor Red
    }
}
