#!/bin/bash

# Script to refactor all JBang integration test scripts to use centralized utilities

set -e

echo "🔄 Refactoring JBang integration test scripts..."

# Define the scripts to update with their module names
declare -A SCRIPTS=(
    ["/home/mark/projects/spring-ai-examples/agentic-patterns/chain-workflow/integration-tests/RunChainWorkflow.java"]="chain-workflow"
    ["/home/mark/projects/spring-ai-examples/agentic-patterns/evaluator-optimizer/integration-tests/RunEvaluatorOptimizer.java"]="evaluator-optimizer"
    ["/home/mark/projects/spring-ai-examples/agentic-patterns/orchestrator-workers/integration-tests/RunOrchestratorWorkers.java"]="orchestrator-workers"
    ["/home/mark/projects/spring-ai-examples/agentic-patterns/parallelization-workflow/integration-tests/RunParallelizationWorkflow.java"]="parallelization-workflow"
    ["/home/mark/projects/spring-ai-examples/agentic-patterns/routing-workflow/integration-tests/RunRoutingWorkflow.java"]="routing-workflow"
    ["/home/mark/projects/spring-ai-examples/kotlin/kotlin-function-callback/integration-tests/RunKotlinFunctionCallback.java"]="kotlin-function-callback"
    ["/home/mark/projects/spring-ai-examples/kotlin/kotlin-hello-world/integration-tests/RunKotlinHelloWorld.java"]="kotlin-hello-world"
    ["/home/mark/projects/spring-ai-examples/misc/openai-streaming-response/integration-tests/RunOpenaiStreamingResponse.java"]="openai-streaming-response"
    ["/home/mark/projects/spring-ai-examples/misc/spring-ai-java-function-callback/integration-tests/RunSpringAiJavaFunctionCallback.java"]="spring-ai-java-function-callback"
    ["/home/mark/projects/spring-ai-examples/model-context-protocol/brave/integration-tests/RunBrave.java"]="brave"
    ["/home/mark/projects/spring-ai-examples/model-context-protocol/client-starter/starter-default-client/integration-tests/RunStarterDefaultClient.java"]="starter-default-client"
    ["/home/mark/projects/spring-ai-examples/model-context-protocol/client-starter/starter-webflux-client/integration-tests/RunStarterWebfluxClient.java"]="starter-webflux-client"
    ["/home/mark/projects/spring-ai-examples/model-context-protocol/filesystem/integration-tests/RunFilesystem.java"]="filesystem"
    ["/home/mark/projects/spring-ai-examples/model-context-protocol/sqlite/simple/integration-tests/RunSimple.java"]="sqlite-simple"
    ["/home/mark/projects/spring-ai-examples/model-context-protocol/weather/starter-webmvc-server/integration-tests/RunStarterWebmvcServer.java"]="starter-webmvc-server"
    ["/home/mark/projects/spring-ai-examples/model-context-protocol/web-search/brave-starter/integration-tests/RunBraveStarter.java"]="brave-starter"
    ["/home/mark/projects/spring-ai-examples/prompt-engineering/prompt-engineering-patterns/integration-tests/RunPromptEngineeringPatterns.java"]="prompt-engineering-patterns"
)

# Calculate relative path depth for each script
for SCRIPT_PATH in "${!SCRIPTS[@]}"; do
    MODULE_NAME="${SCRIPTS[$SCRIPT_PATH]}"
    CLASS_NAME=$(basename "$SCRIPT_PATH" .java)
    
    # Calculate the relative path to integration-testing/jbang-lib
    DEPTH=$(echo "$SCRIPT_PATH" | tr '/' '\n' | grep -c .)
    # From spring-ai-examples root, count depth minus 1 for the script file itself
    RELATIVE_PATH=""
    
    # Determine the correct relative path based on location
    if [[ $SCRIPT_PATH == */model-context-protocol/client-starter/* ]]; then
        # 5 levels up for client-starter submodules
        RELATIVE_PATH="../../../../../integration-testing/jbang-lib/IntegrationTestUtils.java"
    elif [[ $SCRIPT_PATH == */model-context-protocol/weather/* ]]; then
        # 5 levels up for weather submodules
        RELATIVE_PATH="../../../../../integration-testing/jbang-lib/IntegrationTestUtils.java"
    elif [[ $SCRIPT_PATH == */model-context-protocol/web-search/* ]]; then
        # 5 levels up for web-search submodules
        RELATIVE_PATH="../../../../../integration-testing/jbang-lib/IntegrationTestUtils.java"
    elif [[ $SCRIPT_PATH == */model-context-protocol/sqlite/simple/* ]]; then
        # 5 levels up for sqlite/simple
        RELATIVE_PATH="../../../../../integration-testing/jbang-lib/IntegrationTestUtils.java"
    elif [[ $SCRIPT_PATH == */kotlin/* ]] || [[ $SCRIPT_PATH == */misc/* ]] || [[ $SCRIPT_PATH == */agentic-patterns/* ]]; then
        # 3 levels up for kotlin, misc, and agentic-patterns modules
        RELATIVE_PATH="../../../integration-testing/jbang-lib/IntegrationTestUtils.java"
    else
        # 4 levels up for model-context-protocol top level modules
        RELATIVE_PATH="../../../../integration-testing/jbang-lib/IntegrationTestUtils.java"
    fi
    
    echo "📝 Refactoring $CLASS_NAME for module: $MODULE_NAME"
    
    # Create the refactored script
    cat > "$SCRIPT_PATH" << EOF
///usr/bin/env jbang "\$0" "\$@" ; exit \$?
//DEPS org.zeroturnaround:zt-exec:1.12
//DEPS com.fasterxml.jackson.core:jackson-databind:2.17.1
//JAVA 17
//FILES ExampleInfo.json
//SOURCES $RELATIVE_PATH

/*
 * Integration test launcher for $MODULE_NAME
 * Refactored to use centralized utilities
 */

public class $CLASS_NAME {
    
    public static void main(String... args) throws Exception {
        IntegrationTestUtils.runIntegrationTest("$MODULE_NAME");
    }
}
EOF
    
    echo "  ✓ Refactored $CLASS_NAME"
done

echo "✅ Refactored ${#SCRIPTS[@]} JBang scripts successfully!"