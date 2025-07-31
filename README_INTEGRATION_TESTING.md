# Integration Testing Guide for Spring AI Examples

This guide explains how to create, run, and maintain integration tests for Spring AI examples using our lightweight testing framework.

## Quick Start

### Prerequisites
- Java 17+
- JBang (install with: `curl -Ls https://sh.jbang.dev | bash -s - app setup`)
- Python 3.8+
- Required API keys (e.g., `OPENAI_API_KEY`)

### Running Tests

```bash
# Run all integration tests
python3 scripts/run_integration_tests.py

# Run with verbose output
python3 scripts/run_integration_tests.py --verbose

# Run tests for specific modules
python3 scripts/run_integration_tests.py --filter="kotlin"

# Generate test report
python3 scripts/run_integration_tests.py --report=test-report.md
```

## Test Architecture

### Simple vs Complex Examples

| Type | Location | When to Use | Example |
|------|----------|-------------|---------|
| **Simple** | `src/test/java/` | Single JVM, basic assertions | `kotlin-hello-world` |
| **Complex** | `integration-tests/` | External deps, long timeouts | `sqlite/simple` |

### Folder Structure

```
your-example/
├── pom.xml
├── src/test/java/...          # Standard unit tests
└── integration-tests/         # Complex integration tests only
    ├── ExampleInfo.json       # Test configuration
    └── RunYourExample.java    # JBang launcher
```

## Creating Integration Tests

### Using the Scaffolding Tool

```bash
# Simple example
python3 scripts/scaffold_integration_test.py kotlin/kotlin-hello-world

# Complex example with longer timeout
python3 scripts/scaffold_integration_test.py agentic-patterns/chain-workflow --complexity complex

# MCP example with database setup
python3 scripts/scaffold_integration_test.py model-context-protocol/sqlite/simple --complexity mcp
```

### Manual Creation

1. **Create integration-tests directory**
   ```bash
   mkdir your-example/integration-tests
   ```

2. **Create ExampleInfo.json**
   ```json
   {
     "timeoutSec": 300,
     "successRegex": [
       "BUILD SUCCESS",
       "Started.*Application"
     ],
     "requiredEnv": ["OPENAI_API_KEY"]
   }
   ```

3. **Create JBang launcher**
   ```bash
   # Use scaffolding tool or copy from existing example
   cp model-context-protocol/sqlite/simple/integration-tests/RunSqliteSimple.java \
      your-example/integration-tests/RunYourExample.java
   ```

## Configuration Guide

### ExampleInfo.json Schema

```json
{
  "timeoutSec": 300,                    // Maximum execution time
  "successRegex": [                     // Patterns that must appear in output
    "BUILD SUCCESS",
    "Started.*Application",
    "Custom success pattern"
  ],
  "requiredEnv": ["OPENAI_API_KEY"],    // Required environment variables
  "setupCommands": [                    // Optional: pre-execution setup
    "./create-database.sh"
  ],
  "cleanupCommands": []                 // Optional: post-execution cleanup
}
```

### Success Pattern Examples

| Example Type | Success Patterns |
|--------------|------------------|
| **Basic Chat** | `["Joke:", "Setup:", "Punchline:"]` |
| **MCP SQLite** | `["Connected to database", "Query results:", "products available"]` |
| **Agentic Workflow** | `["EVALUATION:\\\\s+PASS", "Chain completed", "Final metrics"]` |

### Timeout Guidelines

| Complexity | Timeout | Reasoning |
|------------|---------|-----------|
| Simple Chat | 120s | Basic API calls |
| Complex Workflow | 300s | Multi-step processing |
| MCP + Database | 300-600s | External service setup |

## Local Development Workflow

### Testing a Single Example

```bash
# Navigate to example directory  
cd kotlin/kotlin-hello-world

# Run integration test directly
jbang integration-tests/RunKotlinHelloWorld.java

# Or run unit tests
./mvnw test
```

### Testing Multiple Examples

```bash
# From repository root
python3 scripts/run_integration_tests.py --filter="kotlin"
python3 scripts/run_integration_tests.py --filter="agentic-patterns"
```

### Debugging Failed Tests

1. **Run with verbose output**
   ```bash
   python3 scripts/run_integration_tests.py --verbose --filter="your-example"
   ```

2. **Check test logs**
   - Integration test output is captured in temporary files
   - Use `--verbose` to see full output
   - Check for specific error patterns

3. **Test patterns locally**
   ```bash
   cd your-example
   ./mvnw clean package spring-boot:run > test-output.log 2>&1
   grep -E "your-success-pattern" test-output.log
   ```

## CI/CD Integration

### GitHub Actions

The integration tests run automatically on:
- Pull requests
- Pushes to main branch
- Daily schedule (6 AM UTC)
- Manual triggers

### Environment Variables

Required secrets in GitHub repository:
- `OPENAI_API_KEY` - OpenAI API access
- `BRAVE_API_KEY` - Brave search API (for web search examples)

### Test Reports

- Integration test reports are uploaded as artifacts
- Failed test logs are preserved for debugging
- Cross-platform compatibility is tested on Ubuntu, Windows, macOS

## Troubleshooting

### Common Issues

#### "JBang not found"
```bash
# Install JBang
curl -Ls https://sh.jbang.dev | bash -s - app setup
# Add to PATH
echo 'export PATH="$HOME/.jbang/bin:$PATH"' >> ~/.bashrc
```

#### "Missing environment variable"
```bash
# Set required API keys
export OPENAI_API_KEY="your-key-here"
export BRAVE_API_KEY="your-brave-key"  # if using Brave examples
```

#### "Integration test timeout"
- Check `ExampleInfo.json` timeout settings
- Verify external services are accessible
- Run with `--verbose` flag for detailed output

#### "Pattern verification failed"
- Update `successRegex` patterns in `ExampleInfo.json`
- Test patterns locally with sample output
- Consider environment-specific output variations

### Performance Issues

#### Slow Test Execution
- Reduce parallel workers: `--workers 2`
- Filter to specific examples: `--filter="simple"`
- Check network connectivity for API-dependent tests

#### Flaky Tests
- Increase timeout values in `ExampleInfo.json`
- Add retry logic for network-dependent operations
- Use more specific success patterns

## Best Practices

### Configuration
- **Start simple**: Use basic patterns, increase complexity as needed
- **Be specific**: Prefer specific success patterns over generic ones
- **Test locally**: Always verify integration tests work locally before CI
- **Document patterns**: Add comments explaining complex regex patterns

### Maintenance
- **Regular updates**: Keep success patterns current with application changes
- **Monitor CI**: Watch for flaky tests and address promptly
- **Version control**: Track changes to test configurations
- **Learning docs**: Document issues and solutions in `learnings/` directory

### Development
- **Incremental**: Add integration tests as you add new examples
- **Consistent**: Follow naming conventions and folder structure
- **Efficient**: Reuse proven patterns from similar examples
- **Collaborative**: Share learnings and improvements with the team

## Examples by Complexity

### Simple Examples
- `kotlin/kotlin-hello-world` - Basic chat interaction
- `models/chat/helloworld` - Simple OpenAI integration
- `misc/openai-streaming-response` - Streaming responses

### Complex Examples
- `agentic-patterns/chain-workflow` - Multi-step AI workflows
- `agentic-patterns/evaluator-optimizer` - Complex evaluation chains
- `agents/reflection` - Advanced AI interactions

### MCP Examples
- `model-context-protocol/sqlite/simple` - Database integration
- `model-context-protocol/weather/starter-webmvc-server` - External APIs
- `model-context-protocol/brave/` - Web search integration

## Getting Help

- Check existing examples for patterns
- Review `learnings/` directory for documented insights
- Run tests with `--verbose` for detailed output
- Create issues for persistent problems
- Consult the troubleshooting section above