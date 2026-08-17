@echo off
REM Quick test of the Card Dispute Investigation API using curl

setlocal enabledelayedexpansion

set baseUrl=http://localhost:8080/api/disputes

echo.
echo ========================================
echo Card Dispute Investigation API - Test
echo ========================================
echo.

echo TEST 1: Creating Dispute Case...
curl -s -X POST %baseUrl% -H "Content-Type: application/json" -d "{\"customerId\": 1001, \"complaintText\": \"I did not make a transaction of 75000 at Electronics World.\"}" > case1.json
set /p case1=<case1.json
echo Case Created: %case1%
echo.

echo TEST 2: Getting Metrics...
curl -s -X GET http://localhost:8080/api/metrics/disputes > metrics.json
set /p metrics=<metrics.json
echo Metrics Retrieved: %metrics%
echo.

echo TEST 3: Checking Application Health...
curl -s -X GET http://localhost:8080/ | find "Dashboard" > nul
if %ERRORLEVEL% == 0 (
    echo Application is running and responding correctly.
) else (
    echo Application is running but may have issues.
)
echo.

echo ========================================
echo Tests Completed!
echo ========================================
echo.

del case1.json > nul 2>&1
del metrics.json > nul 2>&1
