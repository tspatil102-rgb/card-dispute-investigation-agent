# LLM API Testing - Sample Complaint Texts

## How to Test LLM Integration

Once deployed to Cloud Run with `GEMINI_API_KEY` set, use these sample complaints to verify the Gemini LLM is responding.

---

## Sample Complaint 1: Simple Unauthorized Transaction

**Use Case**: Test basic text generation capability

**Request**:
```bash
curl -X POST https://card-dispute-investigation-agent-git-589638503857.asia-south1.run.app/api/disputes \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1001,
    "transactionAmount": 85.50,
    "transactionDate": "2026-08-15T14:30:00",
    "transactionDescription": "Unauthorized Online Purchase",
    "merchant": "Unknown Electronics Store",
    "reason": "I did not authorize this purchase. It appeared on my statement without my knowledge.",
    "status": "NEW"
  }'
```

**What to Look For**: 
- HTTP 201 Created response
- `caseId` in response (e.g., "D1786948356001")
- Status "NEW"
- Application logs should show LLM analysis being performed

---

## Sample Complaint 2: Duplicate Charge (Good for LLM Analysis)

**Use Case**: Tests LLM pattern recognition

**Request**:
```bash
curl -X POST https://card-dispute-investigation-agent-git-589638503857.asia-south1.run.app/api/disputes \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1002,
    "transactionAmount": 149.99,
    "transactionDate": "2026-08-16T09:15:00",
    "transactionDescription": "Duplicate Software License Purchase",
    "merchant": "Microsoft Store",
    "reason": "I purchased a software license for $149.99. The charge appeared twice on my card within 5 minutes. I only made one purchase.",
    "status": "NEW"
  }'
```

**LLM Analysis Expected**: 
- Recognition of duplicate/double-charge pattern
- Recommendation for transaction reversal
- Risk assessment as "Low" (legitimate but duplicate error)

---

## Sample Complaint 3: International Fraud (Tests LLM Risk Assessment)

**Use Case**: Complex fraud scenario for LLM analysis

**Request**:
```bash
curl -X POST https://card-dispute-investigation-agent-git-589638503857.asia-south1.run.app/api/disputes \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1003,
    "transactionAmount": 2500.00,
    "transactionDate": "2026-08-14T22:45:00",
    "transactionDescription": "Wire Transfer to Unknown International Account",
    "merchant": "Unknown Bank Transfer Service",
    "reason": "I was traveling in Europe but did not authorize any wire transfers. A $2500 wire transfer to an unknown account in Nigeria was charged to my card while I was sleeping. This is definitely fraud.",
    "status": "NEW"
  }'
```

**LLM Analysis Expected**:
- High-risk fraud detection
- Recognition of international transferfrom unusual location
- Recommendation for card replacement
- Urgent escalation flag

---

## Sample Complaint 4: Subscription Scam (Tests LLM Context Understanding)

**Use Case**: Sneaky recurring charge

**Request**:
```bash
curl -X POST https://card-dispute-investigation-agent-git-589638503857.asia-south1.run.app/api/disputes \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1004,
    "transactionAmount": 9.99,
    "transactionDate": "2026-08-10T00:00:00",
    "transactionDescription": "Unauthorized Monthly Subscription Charge",
    "merchant": "Premium Services Inc",
    "reason": "Free trial charge appeared on my card. I never agreed to monthly charges after the free trial ended. They claim I accepted terms but I never saw any confirmation email. This has been recurring for 3 months: $9.99 each month.",
    "status": "NEW"
  }'
```

**LLM Analysis Expected**:
- Recognition of dark pattern / subscription trap
- Analysis of free-to-paid conversion
- Recommendation for missing consent verification
- Suggestion to report to FTC

---

## Sample Complaint 5: Restaurant Tip Manipulation (Data Integrity Test)

**Use Case**: Tests LLM attention to detail

**Request**:
```bash
curl -X POST https://card-dispute-investigation-agent-git-589638503857.asia-south1.run.app/api/disputes \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1005,
    "transactionAmount": 245.67,
    "transactionDate": "2026-08-12T19:30:00",
    "transactionDescription": "Restaurant Meal with Unauthorized Tip Increase",
    "merchant": "Upscale Restaurant Downtown",
    "reason": "I paid for dinner with my card. The receipt showed $45.50 for the meal. I added $5 tip (total $50.50). But my card was charged $245.67. The merchant added unauthorized high tip or misprocessed the transaction.",
    "status": "NEW"
  }'
```

**LLM Analysis Expected**:
- Detection of amount mismatch (receipt vs charge)
- Recognition of possible point-of-sale fraud
- Recommendation for receipt verification
- Flag for merchant audit

---

## How to Verify LLM is Working

### 1. Check Application Logs
```bash
gcloud logging tail 'resource.labels.service_name="spring-boot-sample"' --follow
```

**Look for lines like:**
```
Generating response for prompt: "Analyze this dispute..."
Gemini API Response: [200 OK]
LLM Analysis complete
```

### 2. Monitor Gemini API Quota
```bash
gcloud compute project-info describe --project=PROJECT-ID | grep -A 5 "generativeai"
```

### 3. Direct LLM Test (Skip API Layer)

Test Gemini directly to ensure API key works (replace with your actual key):

```bash
# PowerShell
$apiKey = 'AQ.YOUR_ACTUAL_GEMINI_API_KEY_HERE'
$body = '{"contents":[{"parts":[{"text":"Analyze this fraud: Customer was charged $500 for a restaurant meal when the receipt was $45. What type of fraud is this?"}]}]}'
$uri = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent'
$headers = @{ 'X-goog-api-key' = $apiKey; 'Content-Type' = 'application/json' }

$response = Invoke-WebRequest -Uri $uri -Method POST -Headers $headers -Body $body -TimeoutSec 30 -UseBasicParsing
$response.Content | ConvertFrom-Json | Select-Object -ExpandProperty candidates | Select-Object -ExpandProperty content
```

### 4. Check Response Patterns

**If LLM is working**, you should see in application logs or API response:
- ✅ Analysis of fraud type
- ✅ Severity assessment (High/Medium/Low Risk)
- ✅ Recommended actions
- ✅ Evidence summary

---

## Common Test Scenarios

| Scenario | Expected LLM Behavior | Status Code |
|----------|----------------------|------------|
| **Simple unauthorized charge** | Risk assessment, reversibility | 201 |
| **Duplicate charge** | Pattern recognition | 201 |
| **International fraud** | Escalation flag | 201 |
| **Subscription scam** | Consent analysis | 201 |
| **Amount mismatch** | POS fraud detection | 201 |
| **Missing API key** | 500 error on LLM call | 500 |
| **Invalid API key** | 400/401 from Gemini API | 500 |

---

## PowerShell Test Script (All-in-One)

```powershell
$baseUrl = 'https://card-dispute-investigation-agent-git-589638503857.asia-south1.run.app'
$headers = @{ 'Content-Type' = 'application/json' }

Write-Host "Testing LLM with Sample Complaints..." -ForegroundColor Cyan
Write-Host ""

# Test 1: Simple Unauthorized
Write-Host "TEST 1: Simple Unauthorized Purchase" -ForegroundColor Yellow
$body1 = @{
    customerId = 1001
    transactionAmount = 85.50
    transactionDate = "2026-08-15T14:30:00"
    transactionDescription = "Unauthorized Online Purchase"
    merchant = "Unknown Electronics Store"
    reason = "I did not authorize this purchase."
    status = "NEW"
} | ConvertTo-Json

try {
    $response = Invoke-WebRequest -Uri "$baseUrl/api/disputes" -Method POST -Headers $headers -Body $body1 -TimeoutSec 30
    $result = $response.Content | ConvertFrom-Json
    Write-Host "✓ Case Created: $($result.caseId)" -ForegroundColor Green
    Write-Host "  Status: $($result.status)" -ForegroundColor Green
} catch {
    Write-Host "✗ Error: $($_.Exception.Message)" -ForegroundColor Red
}

Start-Sleep -Seconds 2

# Test 2: Duplicate Charge
Write-Host ""
Write-Host "TEST 2: Duplicate Charge (Pattern Recognition)" -ForegroundColor Yellow
$body2 = @{
    customerId = 1002
    transactionAmount = 149.99
    transactionDate = "2026-08-16T09:15:00"
    transactionDescription = "Duplicate Software License"
    merchant = "Microsoft Store"
    reason = "Charged twice for same software license within 5 minutes. Need refund for duplicate."
    status = "NEW"
} | ConvertTo-Json

try {
    $response = Invoke-WebRequest -Uri "$baseUrl/api/disputes" -Method POST -Headers $headers -Body $body2 -TimeoutSec 30
    $result = $response.Content | ConvertFrom-Json
    Write-Host "✓ Case Created: $($result.caseId)" -ForegroundColor Green
    Write-Host "  Amount: $($result.transactionAmount)" -ForegroundColor Green
} catch {
    Write-Host "✗ Error: $($_.Exception.Message)" -ForegroundColor Red
}

Start-Sleep -Seconds 2

# Test 3: Fraud Test
Write-Host ""
Write-Host "TEST 3: International Fraud (Risk Assessment)" -ForegroundColor Yellow
$body3 = @{
    customerId = 1003
    transactionAmount = 2500.00
    transactionDate = "2026-08-14T22:45:00"
    transactionDescription = "Wire Transfer to Nigeria"
    merchant = "Unknown Bank Transfer"
    reason = "Wire transfer to unknown Nigeria account while I was sleeping. High fraud risk."
    status = "NEW"
} | ConvertTo-Json

try {
    $response = Invoke-WebRequest -Uri "$baseUrl/api/disputes" -Method POST -Headers $headers -Body $body3 -TimeoutSec 30
    $result = $response.Content | ConvertFrom-Json
    Write-Host "✓ Case Created: $($result.caseId)" -ForegroundColor Green
    Write-Host "  High Amount: $($result.transactionAmount) USD" -ForegroundColor Yellow
} catch {
    Write-Host "✗ Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "Testing Complete!" -ForegroundColor Cyan
Write-Host "Check application logs for LLM analysis output:"
Write-Host "  gcloud logging tail 'resource.labels.service_name=\"spring-boot-sample\"' --follow"
Write-Host ""
```

---

## Expected Success Indicators

✅ **LLM is working if you see:**
1. Case created with 201 status
2. Application logs show "Generating LLM analysis..."
3. Logs contain Gemini API response details
4. No 400/401/403 errors from Gemini API
5. Response completes within 30 seconds (timeout setting)

❌ **LLM is NOT working if you see:**
1. HTTP 500 errors
2. Log messages: "GEMINI_API_KEY not set"
3. "Invalid API Key" errors
4. Request timeout (>30 seconds)
5. "Service unavailable" from Gemini

---

## Debugging checklist

1. **Verify API Key is Set**:
   ```bash
   gcloud run services describe spring-boot-sample --region asia-south1 | grep GEMINI_API_KEY
   ```

2. **Check Service Environment**:
   ```bash
   gcloud run services describe spring-boot-sample --region asia-south1 --format="value(spec.template.spec.containers[0].env)"
   ```

3. **Test LLM Directly**:
   ```bash
   gcloud compute project-info describe --project=YOUR-PROJECT | grep -i "api"
   ```

4. **Review Recent Logs**:
   ```bash
   gcloud logging read 'resource.labels.service_name="spring-boot-sample" AND severity="ERROR"' --limit=20 --order=desc
   ```

---

## Quick Copy-Paste Complaint Texts

### For Testing in Postman/curl:
```json
{
  "customerId": 1001,
  "transactionAmount": 85.50,
  "transactionDate": "2026-08-15T14:30:00",
  "transactionDescription": "Unauthorized Online Purchase",
  "merchant": "Unknown Electronics Store",
  "reason": "I did not authorize this purchase. It appeared on my statement without my knowledge.",
  "status": "NEW"
}
```

Use the endpoint: `POST /api/disputes`

---

Good luck testing! Report back with:
- Case IDs created
- Application log snippets
- Any error messages

This will confirm the LLM integration is fully operational.
