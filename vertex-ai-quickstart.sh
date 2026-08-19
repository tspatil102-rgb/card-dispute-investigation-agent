#!/bin/bash
# Quick Start: Testing Vertex AI Integration
# This script sets up environment and runs a test case

set -e

echo "=========================================="
echo "Vertex AI Migration - Quick Start"
echo "=========================================="
echo ""

# Check if gcloud is available
if ! command -v gcloud &> /dev/null; then
    echo "❌ gcloud CLI not found. Please install: https://cloud.google.com/sdk/docs/install"
    exit 1
fi

echo "📝 Step 1: Determining GCP Project..."
PROJECT_ID=$(gcloud config get-value project)
if [ -z "$PROJECT_ID" ]; then
    echo "❌ No GCP project set. Run: gcloud config set project PROJECT_ID"
    exit 1
fi
echo "✅ Project: $PROJECT_ID"
echo ""

echo "📝 Step 2: Enabling Vertex AI API..."
gcloud services enable aiplatform.googleapis.com --project=$PROJECT_ID --quiet
echo "✅ Vertex AI API enabled"
echo ""

echo "📝 Step 3: Setting environment variables..."
export GCP_PROJECT_ID=$PROJECT_ID
export GCP_REGION=us-central1
export VERTEX_AI_MODEL=gemini-2.0-flash
echo "✅ Environment configured:"
echo "   - Project: $GCP_PROJECT_ID"
echo "   - Region: $GCP_REGION"
echo "   - Model: $VERTEX_AI_MODEL"
echo ""

echo "📝 Step 4: Building project..."
mvn clean compile -q
echo "✅ Build completed"
echo ""

echo "📝 Step 5: Starting application..."
echo "🚀 App will be available at: http://localhost:8080"
echo "📊 Swagger UI: http://localhost:8080/swagger-ui.html"
echo ""
echo "Press Ctrl+C to stop"
echo ""

# Check for Application Default Credentials
if [ -z "$GOOGLE_APPLICATION_CREDENTIALS" ]; then
    echo "ℹ️  Using Application Default Credentials"
    echo "   (Make sure you've run: gcloud auth application-default login)"
fi

mvn spring-boot:run
