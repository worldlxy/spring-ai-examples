# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository Overview

This is a collection of Spring AI examples demonstrating various AI integration patterns and use cases. The repository is organized as a multi-module Maven project with examples covering:

- **Agentic Patterns**: Chain workflows, orchestrator-workers, parallelization, routing, and evaluator-optimizer patterns
- **Model Context Protocol (MCP)**: Extensive examples for MCP clients and servers including SQLite, filesystem, weather, web search, and annotations
- **Kotlin Examples**: Hello world, function callbacks, and RAG implementations in Kotlin
- **Miscellaneous**: Function callbacks, streaming responses, and prompt engineering patterns

## Build System and Commands

This project uses Maven with the Maven Wrapper (mvnw). Each module can be built and run independently.

### Common Development Commands

**Build entire project:**
```bash
./mvnw clean package
```

**Run a specific example:**
```bash
./run-example.sh <project-directory-name>
# Example: ./run-example.sh agentic-patterns/chain-workflow
```

**Build and run individual module:**
```bash
cd <module-directory>
./mvnw clean package
./mvnw spring-boot:run
```

**Build from root (all modules):**
```bash
mvn clean package
```

## Architecture

### Module Structure
- Each example is a self-contained Spring Boot application
- Modules follow standard Maven directory structure: `src/main/java` and `src/test/java`
- Common Spring AI version: `1.1.0-SNAPSHOT`
- Java version: 17
- Spring Boot parent: `3.4.5`

### Key Module Categories

**Agentic Patterns (`agentic-patterns/`):**
- Demonstrates AI agent workflow patterns
- Each pattern is a separate module with its own Application class

**Model Context Protocol (`model-context-protocol/`):**
- MCP client and server implementations
- Covers various protocols: SQLite, filesystem access, weather APIs, web search
- Includes both manual and starter-based implementations
- Server types: WebMVC, WebFlux, and STDIO

**Kotlin Examples (`kotlin/`):**
- Kotlin-based Spring AI implementations
- Includes RAG (Retrieval-Augmented Generation) examples

### Configuration Patterns
- Application properties typically in `src/main/resources/application.properties` or `application.yaml`
- MCP configurations often use `mcp-servers-config.json`
- Docker Compose files for complex setups (e.g., `compose.yml`)

## Testing Framework

This repository uses a comprehensive integration testing framework with **AI-powered validation** for ensuring all examples work correctly across releases. The framework achieves **97% coverage (32/33 examples)** with intelligent validation for interactive applications.

### Testing Approaches

| Example Type | Test Location | Command | Purpose |
|--------------|---------------|---------|---------|
| **Simple** | `src/test/java/` | `./mvnw test` | Unit tests, basic functionality |
| **Complex** | `integration-tests/` | `jbang integration-tests/Run*.java` | End-to-end integration with AI validation |

### Key Features

- **AI Validation**: Intelligent analysis of application logs for non-deterministic outputs
- **Interactive Application Support**: Testing of chatbots and Scanner-based applications
- **Centralized Architecture**: Single source of truth with 84% code reduction
- **Comprehensive Logging**: Full application output preserved for debugging

### Quick Testing Commands

**Run all integration tests (recommended):**
```bash
./integration-testing/scripts/rit-direct.sh
```

**Run specific integration test:**
```bash
./integration-testing/scripts/rit-direct.sh module-name
```

**Run with clean logs:**
```bash
./integration-testing/scripts/rit-direct.sh --clean-logs
```

**Create integration test for new example:**
```bash
python3 integration-testing/scripts/scaffold_integration_test.py <module-path> [--complexity simple|complex|mcp]
```

**Create integration test with AI validation:**
```bash
# Simple example (hybrid AI validation)
python3 integration-testing/scripts/scaffold_integration_test.py kotlin/kotlin-hello-world --complexity simple

# Complex workflow (primary AI validation)
python3 integration-testing/scripts/scaffold_integration_test.py agentic-patterns/chain-workflow --complexity complex

# Interactive application (fallback AI validation)
python3 integration-testing/scripts/scaffold_integration_test.py agents/reflection --complexity complex

# MCP example (hybrid validation)
python3 integration-testing/scripts/scaffold_integration_test.py model-context-protocol/weather/server --complexity complex
```

**Test specific example:**
```bash
cd <module-directory>
jbang integration-tests/Run*.java  # For complex examples
./mvnw test                         # For simple examples
```

### Integration Test Structure

Complex examples include an `integration-tests/` directory with AI validation:
```
module/
├── integration-tests/
│   ├── ExampleInfo.json      # Test configuration with AI validation support
│   └── RunModule.java        # JBang launcher (uses centralized utilities)
```

### JBang Script Pattern & AI Validation

All integration test scripts use centralized utilities with AI validation support:
- **Centralized Architecture**: Each script is only ~18 lines (84% code reduction)
- **AI Integration**: Automatic AI validation using Claude for intelligent analysis
- **Universal Support**: All test logic in `integration-testing/jbang-lib/IntegrationTestUtils.java`
- **Interactive Apps**: Special handling for Scanner-based applications

**Validation Modes:**
- **Primary**: AI-only validation for unpredictable AI outputs
- **Hybrid**: Regex patterns + AI validation for reliability
- **Fallback**: Regex primary with AI backup for interactive applications

See `integration-testing/docs/README.md` for complete guide and `integration-testing/docs/TROUBLESHOOTING.md` for troubleshooting.

### 🤖 AI Validation (NEW)

The integration testing framework now includes AI-powered validation using Claude to intelligently analyze test outputs. This goes beyond regex pattern matching to understand context, validate unpredictable AI outputs, and assess complex workflows.

**Key Benefits:**
- **Intelligent Assessment** - Understands if examples achieved their intended purpose
- **Handles AI Outputs** - Validates jokes, conversations, creative content  
- **Context-Aware** - Uses README documentation for validation context
- **Multi-Component Support** - Validates distributed systems holistically
- **Cost Efficient** - ~400 tokens per validation with high cache utilization

**When AI Validation Excels:**
- Chat examples (jokes, conversations, creative responses)
- Agentic patterns (multi-step reasoning, workflow completion)
- MCP examples (protocol validation, tool discovery)
- Function calling (realistic tool usage verification)
- Complex workflows with unpredictable outputs

**Configuration Example:**
```json
{
  "timeoutSec": 300,
  "successRegex": ["BUILD SUCCESS", "Started.*Application"],
  "requiredEnv": ["OPENAI_API_KEY"],
  "aiValidation": {
    "enabled": true,
    "validationMode": "hybrid",
    "expectedBehavior": "Application should demonstrate chat functionality with coherent AI responses",
    "promptTemplate": "chat_example_validation"
  }
}
```

**Validation Modes:**
- `primary` - AI validation only (best for unpredictable AI outputs)
- `hybrid` - Both regex and AI must pass (recommended for most cases)  
- `fallback` - AI validation if regex fails (gradual migration)

For complete details, see `integration-testing/docs/AI_VALIDATION.md`.

## Development Notes

- Use the provided `run-example.sh` script for quick testing of individual examples
- Each module has its own Maven wrapper for isolated builds
- Examples demonstrate both OpenAI and other AI model integrations
- MCP examples often include database setup scripts (e.g., `create-database.sh`)
- Complex examples should include integration tests for CI/CD validation