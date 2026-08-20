# Cloud Run Deployment Guide - Vertex AI with OAuth 2.0

## Overview
This guide explains how to securely deploy the Card Dispute Investigation Agent to Google Cloud Run with Vertex AI backend using OAuth 2.0 authentication via service accounts.

## Security Model
- **OAuth 2.0 Service Account** authentication (no API keys)
- **Automatic token management** with caching and refresh
- **Environment variable configuration** at deployment time
- **Cloud Run IAM binding** ensures least-privilege access
- **No hardcoded secrets** in source code or Docker image

## Prerequisites
- Google Cloud Project with Cloud Run API enabled
- gcloud CLI installed and authenticated
- Docker image already built and pushed to Container Registry/Artifact Registry
- Gemini API key from Google AI Studio (https://aistudio.google.com/app/apikey)

## Deployment Option 1: Environment Variable (Quick Setup)

### Step 1: Build and Push Docker Image
```bash
# From project root
docker build -t gcr.io/YOUR-PROJECT-ID/spring-boot-sample:latest .

# You need to be authenticated with gcloud
gcloud auth configure-docker

# Push to Google Container Registry
docker push gcr.io/YOUR-PROJECT-ID/spring-boot-sample:latest
```

### Step 2: Deploy to Cloud Run with Environment Variable
```bash
gcloud run deploy spring-boot-sample \
  --image gcr.io/YOUR-PROJECT-ID/spring-boot-sample:latest \
  --region asia-south1 \
  --platform managed \
  --allow-unauthenticated \
  --port 8080 \
  --set-env-vars GEMINI_API_KEY='AQ.YOUR_ACTUAL_GEMINI_API_KEY_HERE'
```

**Risks**: API key visible in deployment history and Cloud Run UI configuration.

## Deployment Option 2: Secret Manager (Recommended for Production)

### Step 1: Create Secret in Secret Manager
```bash
# Store the Gemini API key as a secret (replace with your actual key from aistudio.google.com)
gcloud secrets create gemini-api-key \
  --data-file=- <<< 'AQ.YOUR_ACTUAL_GEMINI_API_KEY_HERE'

# Grant Cloud Run service account access to the secret
gcloud secrets add-iam-policy-binding gemini-api-key \
  --member=serviceAccount:PROJECT-ID@appspot.gserviceaccount.com \
  --role=roles/secretmanager.secretAccessor
```

### Step 2: Deploy with Secret Reference
```bash
gcloud run deploy spring-boot-sample \
  --image gcr.io/YOUR-PROJECT-ID/spring-boot-sample:latest \
  --region asia-south1 \
  --platform managed \
  --allow-unauthenticated \
  --port 8080 \
  --update-secrets GEMINI_API_KEY=projects/PROJECT-ID/secrets/gemini-api-key:latest
```

**Benefits**: 
- Secret never appears in source control or deployment logs
- Centralized secret management
- Audit trail in Cloud Audit Logs
- Easy rotation without redeployment

## Step 3: Verify Deployment

### Check Service Status
```bash
gcloud run services list --region asia-south1
gcloud run services describe spring-boot-sample --region asia-south1
```

### Test Health Endpoint
```bash
curl -v https://card-dispute-investigation-agent-git-589638503857.asia-south1.run.app/
```

### View Logs
```bash
# View recent logs
gcloud logging read 'resource.type="cloud_run_revision" AND resource.labels.service_name="spring-boot-sample"' \
  --limit 50 \
  --order=desc

# Check if GEMINI_API_KEY is properly set
gcloud run services describe spring-boot-sample --region=asia-south1 | grep -i gemini

# Follow logs (streaming)
gcloud logging tail 'resource.type="cloud_run_revision" AND resource.labels.service_name="spring-boot-sample"' \
  --follow
```

### Test API Endpoints
```bash
# Health check
curl https://card-dispute-investigation-agent-git-589638503857.asia-south1.run.app/

# Get metrics
curl https://card-dispute-investigation-agent-git-589638503857.asia-south1.run.app/api/metrics

# Create dispute case
curl -X POST https://card-dispute-investigation-agent-git-589638503857.asia-south1.run.app/api/disputes \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1001,
    "transactionAmount": 150.00,
    "transactionDate": "2026-08-17T10:30:00",
    "transactionDescription": "Online Purchase",
    "merchant": "Test Merchant",
    "reason": "Unauthorized transaction",
    "status": "NEW"
  }'
```

## Application Configuration

### Environment Variables
| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `GEMINI_API_KEY` | YES | None | Google Gemini API key from aistudio.google.com |
| `SERVER_PORT` | No | 8080 | Container port (Cloud Run uses 8080) |
| `SPRING_PROFILES_ACTIVE` | No | default | Spring profile (default/dev/prod) |

### Application Properties (Read at Runtime)
The following are configured in `application.properties` and loaded at runtime:

```properties
# Always requires GEMINI_API_KEY environment variable
llm.gemini.api-key=${GEMINI_API_KEY}
llm.gemini.model=gemini-flash-latest
llm.gemini.timeout-seconds=30
```

## Troubleshooting

### Error: "GEMINI_API_KEY is not set"
**Cause**: Environment variable not provided to Cloud Run service
**Solution**: 
- Online UI: Cloud Run → Services → spring-boot-sample → Edit & Deploy → Add environment variable
- CLI: Re-run deploy command with `--set-env-vars GEMINI_API_KEY='...'` or `--update-secrets`

### Error: "Invalid API Key or Permissions"
**Cause**: API key is invalid or doesn't have access to Gemini
**Solution**:
1. Verify key format: Must start with `AQ.`
2. Check Google AI Studio: https://aistudio.google.com/app/apikey
3. Ensure Gemini API is enabled in Google Cloud Project
4. Update secret: `gcloud secrets versions add gemini-api-key --data-file=-`
5. Redeploy service to pick up new secret

### Error: "Service unreachable" or timeouts
**Cause**: Cloud Run service is not running or has startup issues
**Solution**:
1. Check logs: `gcloud logging tail 'resource.labels.service_name="spring-boot-sample"'`
2. Review revision errors: `gcloud run revisions list --service=spring-boot-sample --region=asia-south1`
3. Check memory/concurrency limits are sufficient
4. Verify Docker image builds and runs locally: `docker run -e GEMINI_API_KEY='...' gcr.io/PROJECT/spring-boot-sample:latest`

## Security Checklist

- ✅ No API keys in `src/main/resources/application.properties`
- ✅ No API keys in Docker image
- ✅ No API keys in Git history (use Secret Manager or CI/CD secrets)
- ✅ GEMINI_API_KEY required at deployment time
- ✅ Private endpoints behind authentication (set `--no-allow-unauthenticated` for production)
- ✅ Regular secret rotation using Secret Manager versions
- ✅ Enable Cloud Audit Logs for secret access tracking
- ✅ Use internal IAM roles, not public service accounts

## Local Testing Before Deployment

```bash
# Build Docker image locally
docker build -t spring-boot-sample:latest .

# Run locally with environment variable (replace with your actual key)
docker run \
  -e GEMINI_API_KEY='AQ.YOUR_ACTUAL_GEMINI_API_KEY_HERE' \
  -p 8080:8080 \
  spring-boot-sample:latest

# Test health check
curl http://localhost:8080/
```

## Useful Commands

```bash
# List all Cloud Run services
gcloud run services list --region=asia-south1

# Get service details
gcloud run services describe spring-boot-sample --region=asia-south1

# Update service (change environment)
gcloud run services update spring-boot-sample \
  --region=asia-south1 \
  --update-env-vars KEY1=value1,KEY2=value2

# Update service (delete environment variable)
gcloud run services update spring-boot-sample \
  --region=asia-south1 \
  --remove-env-vars OLD_KEY

# Delete service
gcloud run services delete spring-boot-sample --region=asia-south1

# Manage secrets
gcloud secrets list
gcloud secrets describe gemini-api-key
gcloud secrets versions list gemini-api-key
gcloud secrets versions access latest --secret=gemini-api-key  # ⚠️ displays secret!

# Update secret (replace with your new key)
echo -n 'AQ.YOUR_NEW_GEMINI_API_KEY_HERE' | gcloud secrets versions add gemini-api-key --data-file=-

# Revoke old secret version (after updating)
gcloud secrets versions destroy VERSION_NUMBER --secret=gemini-api-key
```

---

## ⭐ UPDATED: Vertex AI Deployment (Current Recommended)

### Prerequisites
1. Google Cloud Project: `spring-boot-sample-505807`
2. Service Account with Vertex AI permissions
3. Service account key JSON file (already configured)
4. Cloud Run service in asia-south1 region

### Step 1: Build and Push Docker Image
```bash
# From project root: c:\workspace\spring-boot-sample
docker build -t gcr.io/spring-boot-sample-505807/dispute-agent:latest .
docker push gcr.io/spring-boot-sample-505807/dispute-agent:latest
```

### Step 2: Deploy to Cloud Run with Vertex AI Environment Variables
```bash
gcloud run deploy card-dispute-investigation-agent \
  --image gcr.io/spring-boot-sample-505807/dispute-agent:latest \
  --project spring-boot-sample-505807 \
  --region asia-south1 \
  --allow-unauthenticated \
  --memory 512Mi \
  --cpu 1 \
  --timeout 300 \
  --set-env-vars "GCP_PROJECT_ID=spring-boot-sample-505807,GCP_REGION=us-central1,VERTEX_AI_MODEL=gemini-1.5-pro"
```

**Key Points:**
- `GCP_PROJECT_ID`: Your Google Cloud project ID
- `GCP_REGION`: Region for Vertex AI API calls (us-central1 for gemini-1.5-pro availability)
- `VERTEX_AI_MODEL`: Model to use (gemini-1.5-pro verified working ✅)
- **No GEMINI_API_KEY needed** - Uses Vertex AI OAuth2 service account
- **No GOOGLE_APPLICATION_CREDENTIALS file needed** - Cloud Run's default service account has necessary IAM roles

### Step 3: Configure IAM Permissions
The Cloud Run service account needs these roles:
```bash
# Get your Cloud Run service account email
gcloud iam service-accounts list --filter="project:spring-boot-sample-505807"

# Grant Vertex AI User role (replace SERVICE_ACCOUNT_EMAIL)
gcloud projects add-iam-policy-binding spring-boot-sample-505807 \
  --member=serviceAccount:SERVICE_ACCOUNT_EMAIL \
  --role=roles/aiplatform.user
```

### Step 4: Update Existing Cloud Run Service (If Already Deployed)
```bash
gcloud run services update card-dispute-investigation-agent \
  --update-env-vars GCP_PROJECT_ID=spring-boot-sample-505807,GCP_REGION=us-central1,VERTEX_AI_MODEL=gemini-1.5-pro \
  --region asia-south1 \
  --project spring-boot-sample-505807
```

## Vertex AI Model Availability Reference
| Model | us-central1 | us-west1 | europe-west1 | asia-south1 |
|-------|:-----------:|:--------:|:------------:|:-----------:|
| gemini-1.5-pro | ✅ TESTED | ✅ | ✅ | ❌ |
| gemini-1.5-flash | ✅ | ✅ | ✅ | ❌ |
| gemini-2.0-flash | ❌ | ✅ | ❌ | ❌ |

**Important:** Set `GCP_REGION=us-central1` for Vertex AI API calls, even if Cloud Run service is deployed in asia-south1. The app region and the model region are independent.

## Testing Deployed Cloud Run Service
```bash
# Get the service URL
SERVICE_URL=$(gcloud run services describe card-dispute-investigation-agent \
  --region asia-south1 \
  --project spring-boot-sample-505807 \
  --format 'value(status.url)')

# Test health endpoint
curl $SERVICE_URL/health

# Create a test dispute
curl -X POST $SERVICE_URL/api/disputes \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 12345,
    "complaintText": "Card charged twice. Please investigate.",
    "cardNumber": "****1234",
    "amount": 99.99,
    "transactionDate": "2024-08-19"
  }'
```

## Troubleshooting Cloud Run Vertex AI Errors

### Error: 404 Model not found
**Symptom:** `"Publisher model... was not found"`
**Solution:** Verify model is available in specified region.  Recommended: Use `gemini-1.5-pro` in `us-central1`
```bash
# Check logs
gcloud run logs read card-dispute-investigation-agent --limit 50 --region asia-south1
```

### Error: 403 Permission denied
**Symptom:** `"Caller does not have permission to use resource"`
**Solution:** Add IAM role to Cloud Run service account:
```bash
gcloud projects add-iam-policy-binding spring-boot-sample-505807 \
  --member=serviceAccount:SERVICE_ACCOUNT_EMAIL \
  --role=roles/aiplatform.user
```

### Error: UNAUTHENTICATED Token generation failed
**Symptom:** OAuth2 token generation fails
**Solution:** Ensure Cloud Run service account has `roles/iam.serviceAccountUser` and `roles/aiplatform.user`

## Comparison: Gemini API vs. Vertex AI

| Feature | Gemini API (Legacy) | Vertex AI (Current) |
|---------|---------------------|---------------------|
| Authentication | API Key (in env var) | OAuth2 Service Account |
| Cost Model | Per-request billing | Per-request billing |
| Rate Limits | Limited free tier | Higher enterprise limits |
| Availability | asia-south1 | us-central1 (for gemini-1.5-pro) |
| Model Selection | Limited to API Studio models | Full Vertex AI catalog |
| Reliability | Best effort | SLA-backed |
| Production Ready | ❌ Not recommended | ✅ Recommended |

## Files Modified for Vertex AI Migration
1. **application.properties** - Added Vertex AI configuration section
2. **VertexAIChatLanguageModel.java** - OAuth2 implementation with token caching
3. **LangChain4jConfiguration.java** - Provider prioritization logic
4. **pom.xml** - Added google-cloud-aiplatform and google-auth dependencies

## Local Testing Verification ✅
- Dispute Creation: ✅ Working
- Investigation with LLM: ✅ Working without 404 errors
- Customer Response Generation: ✅ LLM generated successfully
- Risk Assessment: ✅ Scores and recommendations generated
- Database Persistence: ✅ All data stored correctly

## References
- [Cloud Run Documentation](https://cloud.google.com/run/docs)
- [Vertex AI API Documentation](https://cloud.google.com/vertex-ai/docs)
- [Google Auth Library - OAuth2](https://github.com/googleapis/google-auth-library-java)
- [Spring Boot Environment Properties](https://docs.spring.io/spring-boot/reference/features/external-config.html)
- [LangChain4j Documentation](https://docs.langchain4j.dev)

