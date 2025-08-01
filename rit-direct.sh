#!/bin/bash

# Direct RIT - Run Integration Tests directly via JBang
# Complete bypass of Python script to avoid hanging issues

# Don't exit on command failure (let tests fail individually)
set -uo pipefail

LOGS_DIR="logs/background-runs"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
LOG_FILE="$(pwd)/${LOGS_DIR}/rit-direct_${TIMESTAMP}.log"

mkdir -p "${LOGS_DIR}"

# Create the log file first
touch "${LOG_FILE}"

echo "🚀 Starting Spring AI Examples Integration Tests (Direct Mode)"
echo "📝 Log file: ${LOG_FILE}"
echo "==============================================="

# Clean up old logs (but not the current log file)
echo "🧹 Cleaning up old logs..." | tee -a "${LOG_FILE}"
find logs/ -name "*.log" -not -name "$(basename "${LOG_FILE}")" -delete 2>/dev/null || true

# Find all JBang integration test scripts
jbang_scripts=($(find . -name "Run*.java" -path "*/integration-tests/*" | sort))

echo "Found ${#jbang_scripts[@]} JBang integration test scripts" | tee -a "${LOG_FILE}"
echo "" | tee -a "${LOG_FILE}"

passed=0
failed=0
failed_tests=()

for script in "${jbang_scripts[@]}"; do
    test_dir=$(dirname "${script}")
    module_dir=$(dirname "${test_dir}")
    module_name=$(basename "${module_dir}")
    parent_name=$(basename $(dirname "${module_dir}"))
    full_name="${parent_name}/${module_name}"
    
    echo "🔄 Testing ${full_name}..." | tee -a "${LOG_FILE}"
    echo "   Script: ${script}" | tee -a "${LOG_FILE}"
    
    # Run JBang script with full path (avoid cd issues)
    if (cd "${module_dir}" && timeout 300s jbang "integration-tests/$(basename "${script}")") >> "${LOG_FILE}" 2>&1; then
        echo "✅ ${full_name} - PASSED" | tee -a "${LOG_FILE}"
        ((passed++))
    else
        exit_code=$?
        if [ ${exit_code} -eq 124 ]; then
            echo "⏰ ${full_name} - TIMEOUT" | tee -a "${LOG_FILE}"
        else
            echo "❌ ${full_name} - FAILED (exit code: ${exit_code})" | tee -a "${LOG_FILE}"
        fi
        ((failed++))
        failed_tests+=("${full_name}")
    fi
    
    echo "" | tee -a "${LOG_FILE}"
done

echo "===============================================" | tee -a "${LOG_FILE}"
echo "📊 Results:" | tee -a "${LOG_FILE}"
echo "  Total: $((passed + failed))" | tee -a "${LOG_FILE}"
echo "  Passed: ${passed}" | tee -a "${LOG_FILE}"
echo "  Failed: ${failed}" | tee -a "${LOG_FILE}"

if [ ${failed} -gt 0 ]; then
    echo "" | tee -a "${LOG_FILE}"
    echo "💥 Failed tests:" | tee -a "${LOG_FILE}"
    for test in "${failed_tests[@]}"; do
        echo "  - ${test}" | tee -a "${LOG_FILE}"
    done
fi

echo "" | tee -a "${LOG_FILE}"
echo "📝 Full log: ${LOG_FILE}"
echo "✅ Testing completed!"