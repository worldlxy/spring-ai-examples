#!/bin/bash

# Script to update all JBang integration test scripts to show full raw output and normalize paths

echo "🔧 Updating JBang integration test scripts..."

# List of files that need updating (excluding already updated ones)
files=(
    "agentic-patterns/evaluator-optimizer/integration-tests/RunEvaluatorOptimizer.java"
    "agentic-patterns/orchestrator-workers/integration-tests/RunOrchestratorWorkers.java"
    "agentic-patterns/parallelization-workflow/integration-tests/RunParallelizationWorkflow.java"
    "agentic-patterns/routing-workflow/integration-tests/RunRoutingWorkflow.java"
    "kotlin/kotlin-function-callback/integration-tests/RunKotlinFunctionCallback.java"
    "kotlin/kotlin-hello-world/integration-tests/RunKotlinHelloWorld.java"
    "misc/spring-ai-java-function-callback/integration-tests/RunSpringAiJavaFunctionCallback.java"
    "model-context-protocol/brave/integration-tests/RunBrave.java"
    "model-context-protocol/client-starter/starter-default-client/integration-tests/RunStarterDefaultClient.java"
    "model-context-protocol/client-starter/starter-webflux-client/integration-tests/RunStarterWebfluxClient.java"
    "model-context-protocol/filesystem/integration-tests/RunFilesystem.java"
    "model-context-protocol/sqlite/simple/integration-tests/RunSimple.java"
    "model-context-protocol/web-search/brave-starter/integration-tests/RunBraveStarter.java"
    "prompt-engineering/prompt-engineering-patterns/integration-tests/RunPromptEngineeringPatterns.java"
)

for file in "${files[@]}"; do
    echo "📝 Updating: $file"
    
    # First, update path normalization
    sed -i 's/logFile\.toAbsolutePath()/logFile.toAbsolutePath().normalize()/g' "$file"
    
    # Create a temporary file for the complex replacement
    temp_file=$(mktemp)
    
    # Read the file and replace the filtering section with full output
    awk '
    BEGIN { in_section = 0; found_section = 0 }
    
    # Detect start of filtering section
    /Show actual captured output|Show key lines|Show relevant output/ {
        in_section = 1
        found_section = 1
        print "            // Show full raw output"
        print "            out.println(\"📋 Full Application Output:\");"
        print "            out.println(\"---\");"
        print "            out.println(output);"
        print "            out.println(\"---\");"
        next
    }
    
    # Skip lines in the filtering section
    in_section && /^[[:space:]]*out\.println\("---"\);[[:space:]]*$/ {
        in_section = 0
        next
    }
    
    # Skip all lines in the filtering section
    in_section { next }
    
    # Print all other lines
    !in_section { print }
    
    END {
        if (found_section) {
            print "    // File updated to show full raw output"
        }
    }
    ' "$file" > "$temp_file"
    
    # Only update if changes were made
    if ! cmp -s "$file" "$temp_file"; then
        mv "$temp_file" "$file"
        echo "  ✅ Updated successfully"
    else
        rm "$temp_file"
        echo "  ⏭️  No changes needed"
    fi
done

echo "✨ All JBang scripts updated!"