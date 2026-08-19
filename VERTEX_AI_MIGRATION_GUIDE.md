# Vertex AI Migration Guide

## ✅ Migration Completed

The application has been successfully migrated from **AI Studio Gemini API** to **Google Cloud Vertex AI**.

---

## Key Changes

### 1. **Removed Dependency**
- ❌ `generativelanguage.googleapis.com` (AI Studio Gemini)

### 2. **New Implementation**
- ✅ `VertexAIChatLanguageModel.java` - Vertex AI REST API client
- ✅ Multi-region fallback support (us-central1, us-west1, us-east1, etc.)
- ✅ OAuth 2.0 authentication via Google Cloud credentials
- ✅ Automatic token caching and refresh

### 3. **Added Dependencies** (pom.xml)
- `com.google.cloud:google-cloud-aiplatform:3.42.0`
- `com.google.auth:google-auth-library-oauth2-http:1.23.0`

### 4. **Configuration** (application.properties)
```properties
# Vertex AI Configuration
llm.vertexai.project-id=${GCP_PROJECT_ID}
llm.vertexai.region=${GCP_REGION:us-central1}
llm.vertexai.model=${VERTEX_AI_MODEL:gemini-2.0-flash}
llm.vertexai.timeout-seconds=30

# Fallback: Gemini (optional)
llm.gemini.api-key=${GEMINI_API_KEY:}
```

---

## Setup Instructions

### Prerequisites
1. Google Cloud Project with Vertex AI API enabled
2. Service Account with `Vertex AI User` and `Vertex AI Editor` roles (or higher)
3. One of the following authentication methods:

### Option A: Service Account Key File (Recommended for Local Development)

1. **Create a service account:**
   ```bash
   gcloud iam service-accounts create dispute-agent \
     --display-name="Dispute Investigation Agent"
   ```

2. **Grant permissions:**
   ```bash
   gcloud projects add-iam-policy-binding <GCP_PROJECT_ID> \
     --member=serviceAccount:dispute-agent@<PROJECT_ID>.iam.gserviceaccount.com \
     --role=roles/aiplatform.user
   ```

3. **Create and download key:**
   ```bash
   gcloud iam service-accounts keys create ~/dispute-agent-key.json \
     --iam-account=dispute-agent@<PROJECT_ID>.iam.gserviceaccount.com
   ```

4. **Set environment variable:**
   ```bash
   # Linux/macOS
   export GOOGLE_APPLICATION_CREDENTIALS=~/dispute-agent-key.json
   export GCP_PROJECT_ID=<your-project-id>
   export GCP_REGION=us-central1  # or any available region
   
   # Windows PowerShell
   $env:GOOGLE_APPLICATION_CREDENTIALS="C:\path\to\dispute-agent-key.json"
   $env:GCP_PROJECT_ID="your-project-id"
   $env:GCP_REGION="us-central1"
   ```

### Option B: Application Default Credentials (Best for Cloud Deployment)

1. **For local development (gcloud CLI):**
   ```bash
   gcloud auth application-default login
   ```

2. **For Cloud Run / App Engine:**
   - Assign the service account directly to the deployment
   - No additional setup needed

3. **Set project configuration:**
   ```bash
   # Linux/macOS
   export GCP_PROJECT_ID=$(gcloud config get-value project)
   export GCP_REGION=us-central1
   
   # Windows PowerShell
   $env:GCP_PROJECT_ID=(gcloud config get-value project)
   $env:GCP_REGION="us-central1"
   ```

---

## Running the Application

### 1. Ensure Vertex AI API is Enabled
```bash
gcloud services enable aiplatform.googleapis.com \
  --project=<GCP_PROJECT_ID>
```

### 2. Start the Application

**With Maven:**
```bash
# Linux/macOS
export GCP_PROJECT_ID=your-project-id
export GCP_REGION=us-central1
mvn spring-boot:run

# Windows PowerShell
$env:GCP_PROJECT_ID="your-project-id"
$env:GCP_REGION="us-central1"
mvn spring-boot:run
```

### 3. Verify It's Working
```bash
# Create a dispute
curl -X POST http://localhost:8080/api/disputes \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1001,
    "complaintText": "Unauthorized $500 transaction"
  }'

# Investigate (triggers Vertex AI)
curl -X POST http://localhost:8080/api/disputes/<CASE_ID>/investigate

# Check results
curl http://localhost:8080/api/disputes/<CASE_ID>
```

---

## Available Regions

Vertex AI supports these regions for the Generative API:

- `us-central1` (Iowa) - Default
- `us-west1` (Oregon)
- `us-east1` (South Carolina)
- `europe-west1` (Belgium)
- `europe-west4` (Netherlands)
- `asia-northeast1` (Tokyo)
- `asia-southeast1` (Singapore)

The application automatically falls back to other regions if the primary region is unavailable.

---

## Model Configuration

The application supports all Vertex AI Generative Models:

- `gemini-2.0-flash` (Latest, recommended)
- `gemini-2.0-pro`
- `gemini-1.5-flash`
- `gemini-1.5-pro`

Set via environment variable:
```bash
export VERTEX_AI_MODEL=gemini-2.0-flash
```

---

## Fallback to Gemini

If Vertex AI is not configured, the application automatically falls back to Gemini API:

```bash
export GEMINI_API_KEY=your-api-key
```

Both can coexist - Vertex AI takes precedence.

---

## Troubleshooting

### Error: "Google Cloud credentials not initialized"
**Solution:** Set `GOOGLE_APPLICATION_CREDENTIALS` to your service account key file

```bash
export GOOGLE_APPLICATION_CREDENTIALS=~/dispute-agent-key.json
```

### Error: "Project ID not found"
**Solution:** Set `GCP_PROJECT_ID` environment variable

```bash
export GCP_PROJECT_ID=your-actual-project-id
```

### Error: "Permission denied" (403)
**Solution:** Ensure service account has `Vertex AI User` role:

```bash
gcloud projects add-iam-policy-binding <PROJECT_ID> \
  --member=serviceAccount:<SERVICE_ACCOUNT> \
  --role=roles/aiplatform.user
```

### Error: "Region not supported"
**Solution:** Try a different region from the [supported regions list](#available-regions)

```bash
export GCP_REGION=us-west1
```

---

## Monitoring & Logging

### View Vertex AI API Usage
```bash
gcloud logging read "resource.type=api" \
  --filter='protoPayload.serviceName="aiplatform.googleapis.com"' \
  --limit 50
```

### Monitor Quota
```bash
gcloud compute project-info describe <PROJECT_ID> \
  --format="value(quotas[name='AIPLATFORM_API_CALLS_PER_PROJECT_PER_DAY'].usage)"
```

---

## Cost Optimization

### Pricing Structure
- Vertex AI: Pay-per-use with free tier limits
- Request-based pricing (per 1000 tokens)
- Regional pricing varies

### Cost Control
1. Set spending limits in GCP Billing
2. Monitor usage in Cloud Console
3. Implement rate limiting in IntakeAgent, DecisionRecommendationAgent, etc.
4. Use model fallback for cost optimization

---

## Next Steps

1. ✅ **Verify build:**
   ```bash
   mvn clean compile
   ```

2. ✅ **Set environment variables** (from Setup section above)

3. ✅ **Start the app:**
   ```bash
   mvn spring-boot:run
   ```

4. ✅ **Test webhook integration** with your dispute investigation workflow

5. 📊 **Monitor performance** in Cloud Console

---

## Reference

- [Vertex AI Documentation](https://cloud.google.com/vertex-ai/docs)
- [Vertex AI Generative API](https://cloud.google.com/vertex-ai/docs/generative-ai/start/quickstarts/api-quickstart)
- [Google Cloud Authentication](https://cloud.google.com/docs/authentication)
- [Service Account Setup Guide](https://cloud.google.com/docs/authentication/provide-credentials-adc)
