#!/bin/bash

# Script to fix duplicate output sections in JBang files

echo "🔧 Fixing duplicate output sections in JBang scripts..."

# List of files that need fixing
files=(
    "agentic-patterns/evaluator-optimizer/integration-tests/RunEvaluatorOptimizer.java"
    "agentic-patterns/parallelization-workflow/integration-tests/RunParallelizationWorkflow.java"
    "kotlin/kotlin-function-callback/integration-tests/RunKotlinFunctionCallback.java"
    "kotlin/kotlin-hello-world/integration-tests/RunKotlinHelloWorld.java"
    "misc/spring-ai-java-function-callback/integration-tests/RunSpringAiJavaFunctionCallback.java"
    "model-context-protocol/brave/integration-tests/RunBrave.java"
    "model-context-protocol/filesystem/integration-tests/RunFilesystem.java"
    "model-context-protocol/sqlite/simple/integration-tests/RunSimple.java"
    "prompt-engineering/prompt-engineering-patterns/integration-tests/RunPromptEngineeringPatterns.java"
)

for file in "${files[@]}"; do
    echo "📝 Fixing: $file"
    
    # Create a temporary file
    temp_file=$(mktemp)
    
    # Process the file to remove duplicate sections and clean up
    awk '
    BEGIN { 
        in_dup = 0
        skip_lines = 0
    }
    
    # Skip the duplicate "Show full raw output" section
    /String\[\] lines = output\.split/ {
        skip_lines = 1
        next
    }
    
    # When we hit the second "Show full raw output", skip it
    skip_lines && /Show full raw output/ {
        skip_lines = 0
        next
    }
    
    # Skip lines in the duplicate section
    skip_lines && !/int failedPatterns = 0/ {
        next
    }
    
    # Resume normal processing when we hit failedPatterns
    /int failedPatterns = 0/ {
        skip_lines = 0
    }
    
    # Remove the extra comment at the end
    /\/\/ File updated to show full raw output/ {
        next
    }
    
    # Print all other lines
    !skip_lines { print }
    ' "$file" > "$temp_file"
    
    # Replace the original file
    mv "$temp_file" "$file"
    echo "  ✅ Fixed successfully"
done

echo "✨ All duplicate sections removed!"