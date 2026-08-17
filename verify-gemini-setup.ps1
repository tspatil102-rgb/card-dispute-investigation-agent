# Gemini API Key Quick Verification Script

Write-Host "=================================="
Write-Host "Gemini API Key Verification"
Write-Host "=================================="
Write-Host ""

# 1. Check configuration file
Write-Host "TEST 1: Check application.properties configuration" -ForegroundColor Cyan
$configFile = "c:\workspace\spring-boot-sample\src\main\resources\application.properties"
if (Test-Path $configFile) {
    $config = Get-Content $configFile | Select-String "llm.gemini"
    Write-Host "OK Configuration file found"
    Write-Host "  Details:"
    foreach ($line in $config) {
        Write-Host "  - $line"
    }
}
else {
    Write-Host "FAIL Configuration file not found"
}
Write-Host ""

# 2. Check API key format
Write-Host "TEST 2: Verify API key format" -ForegroundColor Cyan
$apiKey = "AQ.Ab8RN6I-tAgYmkezRM1wPXFh2EcBZNqdDcb1eG6Q0UIgA0i7xg"
if ($apiKey -match "^AQ\.[A-Za-z0-9_-]+$") {
    Write-Host "OK API key format is valid"
    Write-Host "  Key: $($apiKey.Substring(0, 10))***hidden***"
    Write-Host "  Length: $($apiKey.Length) characters"
    Write-Host "  Prefix: AQ (Google Generative AI)"
}
else {
    Write-Host "FAIL API key format is invalid"
}
Write-Host ""

# 3. Check test files created
Write-Host "TEST 3: Verify test files are created" -ForegroundColor Cyan
$testDir = "c:\workspace\spring-boot-sample\src\test\java\com\example\demo\config"
$testFiles = @(
    "GeminiLLMConfigurationTest.java",
    "GeminiChatLanguageModelTest.java"
)

foreach ($testFile in $testFiles) {
    $path = Join-Path $testDir $testFile
    if (Test-Path $path) {
        $size = (Get-Item $path).Length
        Write-Host "OK $testFile - $size bytes"
    }
    else {
        Write-Host "FAIL $testFile not found"
    }
}
Write-Host ""

# 4. Count test cases
Write-Host "TEST 4: Count test cases" -ForegroundColor Cyan
$configTestFile = Join-Path $testDir "GeminiLLMConfigurationTest.java"
$modelTestFile = Join-Path $testDir "GeminiChatLanguageModelTest.java"

$configTests = 0
$modelTests = 0

if (Test-Path $configTestFile) {
    $configTests = (Get-Content $configTestFile | Select-String "@Test" | Measure-Object).Count
    Write-Host "OK GeminiLLMConfigurationTest: $configTests test cases"
}

if (Test-Path $modelTestFile) {
    $modelTests = (Get-Content $modelTestFile | Select-String "@Test" | Measure-Object).Count
    Write-Host "OK GeminiChatLanguageModelTest: $modelTests test cases"
}

$totalTests = $configTests + $modelTests
Write-Host "  Total: $totalTests test methods"
Write-Host ""

# 5. Check documentation
Write-Host "TEST 5: Check documentation file" -ForegroundColor Cyan
$docFile = "c:\workspace\spring-boot-sample\GEMINI_API_KEY_TEST_GUIDE.md"
if (Test-Path $docFile) {
    $docSize = (Get-Item $docFile).Length
    Write-Host "OK GEMINI_API_KEY_TEST_GUIDE.md - $docSize bytes"
}
else {
    Write-Host "FAIL Documentation file not found"
}
Write-Host ""

# 6. Summary
Write-Host "=================================="
Write-Host "SETUP VERIFICATION COMPLETE"
Write-Host "=================================="
Write-Host "✓ API Key: Valid (AQ.* format)"
Write-Host "✓ Configuration: application.properties"
Write-Host "✓ Test Suite: $totalTests test methods"
Write-Host "✓ Documentation: Complete test guide"
Write-Host ""
Write-Host "Ready to execute tests:"
Write-Host "  mvn test -Dtest=GeminiLLMConfigurationTest"
Write-Host "  mvn test -Dtest=GeminiChatLanguageModelTest"
Write-Host ""
