# Cloud Run Deployment Guide - Security Best Practices

## Overview
This guide explains how to securely deploy the Card Dispute Investigation Agent to Google Cloud Run with proper Gemini API key management.

## Security Model
- **No hardcoded secrets** in source code or Docker image
- **Environment variable configuration** at deployment time
- **Secret Manager integration** (recommended for production)
- **Application fails safely** if GEMINI_API_KEY is not provided

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
  --set-env-vars GEMINI_API_KEY='AQ.Ab8RN6J-JYP9CFi2f0DbVVH0O7l9r387Qf9I0418sxSA9OW_bA'
```

**Risks**: API key visible in deployment history and Cloud Run UI configuration.

## Deployment Option 2: Secret Manager (Recommended for Production)

### Step 1: Create Secret in Secret Manager
```bash
# Store the Gemini API key as a secret
gcloud secrets create gemini-api-key \
  --data-file=- <<< 'AQ.Ab8RN6J-JYP9CFi2f0DbVVH0O7l9r387Qf9I0418sxSA9OW_bA'

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

# Run locally with environment variable
docker run \
  -e GEMINI_API_KEY='AQ.Ab8RN6J-JYP9CFi2f0DbVVH0O7l9r387Qf9I0418sxSA9OW_bA' \
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

# Update secret
echo -n 'NEW_KEY_VALUE' | gcloud secrets versions add gemini-api-key --data-file=-

# Revoke old secret version (after updating)
gcloud secrets versions destroy VERSION_NUMBER --secret=gemini-api-key
```

## References
- [Cloud Run Documentation](https://cloud.google.com/run/docs)
- [Using Secrets in Cloud Run](https://cloud.google.com/run/docs/configuring/secrets)
- [Secret Manager](https://cloud.google.com/secret-manager/docs)
- [Google AI Studio - Get API Key](https://aistudio.google.com/app/apikey)
- [Spring Boot Environment Properties](https://docs.spring.io/spring-boot/reference/features/external-config.html)
