# Quick Start: Testing Vertex AI Integration (Windows PowerShell)
# This script sets up environment and runs a test case

Write-Output "=========================================="
Write-Output "Vertex AI Migration - Quick Start (Windows)"
Write-Output "=========================================="
Write-Output ""

# Check if gcloud is available
try {
    $gcloud = gcloud --version
} catch {
    Write-Output "❌ gcloud CLI not found. Please install: https://cloud.google.com/sdk/docs/install"
    exit 1
}

Write-Output "📝 Step 1: Determining GCP Project..."
$PROJECT_ID = (gcloud config get-value project 2>$null).Trim()
if ([string]::IsNullOrEmpty($PROJECT_ID)) {
    Write-Output "❌ No GCP project set. Run: gcloud config set project PROJECT_ID"
    exit 1
}
Write-Output "✅ Project: $PROJECT_ID"
Write-Output ""

Write-Output "📝 Step 2: Enabling Vertex AI API..."
gcloud services enable aiplatform.googleapis.com --project=$PROJECT_ID --quiet 2>$null
Write-Output "✅ Vertex AI API enabled"
Write-Output ""

Write-Output "📝 Step 3: Setting environment variables..."
$env:GCP_PROJECT_ID = $PROJECT_ID
$env:GCP_REGION = "us-central1"
$env:VERTEX_AI_MODEL = "gemini-2.0-flash"
Write-Output "✅ Environment configured:"
Write-Output "   - Project: $env:GCP_PROJECT_ID"
Write-Output "   - Region: $env:GCP_REGION"
Write-Output "   - Model: $env:VERTEX_AI_MODEL"
Write-Output ""

Write-Output "📝 Step 4: Building project..."
mvn clean compile -q
Write-Output "✅ Build completed"
Write-Output ""

Write-Output "📝 Step 5: Starting application..."
Write-Output "🚀 App will be available at: http://localhost:8080"
Write-Output "📊 Swagger UI: http://localhost:8080/swagger-ui.html"
Write-Output ""
Write-Output "Press Ctrl+C to stop"
Write-Output ""

# Check for Application Default Credentials
if ([string]::IsNullOrEmpty($env:GOOGLE_APPLICATION_CREDENTIALS)) {
    Write-Output "ℹ️  Using Application Default Credentials"
    Write-Output "   (Make sure you've run: gcloud auth application-default login)"
}

Write-Output ""
mvn spring-boot:run
