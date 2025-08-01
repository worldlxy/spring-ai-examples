# Spring AI Examples — Integration‑Testing Plan (v3.1)

> **Light‑weight per‑sample folders + Python CI runner**  
> Each example can keep an optional `integration-tests/` directory that contains  
> * a JBang launcher (`Run*.java`) and  
> * a minimal `ExampleInfo.json` metadata file.  
> No extra Maven modules or `pom.xml` files are introduced.

---

## 1. Test Strategy & Location

| Case | Test location | Runs with | Purpose |
|------|---------------|-----------|---------|
| **Simple** example (single JVM, trivial Spring assert) | `src/test/java/*SmokeTests.java` | `mvn test` | Unit tests, basic functionality |
| **Complex** example (needs client/server, Docker, long timeout, env‑vars) | `integration-tests/` folder with JBang script + JSON metadata | `python scripts/run_integration_tests.py` | End-to-end integration |

### Key Benefits of This Approach
- **Minimal overhead**: No additional Maven modules or complex pom.xml configurations
- **Flexible**: Simple examples use standard Maven test lifecycle, complex ones get dedicated integration testing
- **Maintainable**: JSON metadata keeps configuration separate from code
- **Developer-friendly**: Clear commands for different testing scenarios

---

## 2. Folder Structure (Per Example)

```
module/
 ├─ pom.xml
 ├─ src/main/java/…              # production code
 ├─ src/test/java/…              # unit tests or simple smoke tests
 └─ integration-tests/           # **present only if complex testing needed**
      ├─ ExampleInfo.json        # test configuration metadata
      └─ Run<Module>.java        # JBang launcher script
```

### Example Classifications

#### Simple Examples (~15 modules)
- `kotlin/kotlin-hello-world`
- `models/chat/helloworld`
- `misc/openai-streaming-response`
- `misc/spring-ai-java-function-callback`
- `kotlin/kotlin-function-callback`
- `prompt-engineering/prompt-engineering-patterns`

**Testing approach**: Standard JUnit tests in `src/test/java/`

#### Complex Examples (~18 modules)
- All `model-context-protocol/*` modules (requires MCP servers, databases, external deps)
- `agentic-patterns/*` modules (multi-step workflows, complex output validation)
- `kotlin/rag-with-kotlin` (requires database setup)
- `agents/reflection` (complex AI interactions)

**Testing approach**: Integration tests in `integration-tests/` folder

---

## 3. Configuration Schema

### `ExampleInfo.json` Structure
```jsonc
{
  "timeoutSec": 300,                    // Maximum execution time
  "successRegex": [                     // Patterns that must appear in output
    "EVALUATION:\\\\s+PASS",
    "FINAL OUTPUT:",
    "BUILD SUCCESS"
  ],
  "requiredEnv": ["OPENAI_API_KEY"],    // Required environment variables
  "serverPort": 0,                      // Optional: server port (0 = random)
  "setupCommands": [                    // Optional: pre-execution setup
    "./create-database.sh"
  ],
  "cleanupCommands": []                 // Optional: post-execution cleanup
}
```

### Example Configurations by Module Type

#### Basic Chat Example
```json
{
  "timeoutSec": 120,
  "successRegex": ["Joke:", "Setup:", "Punchline:"],
  "requiredEnv": ["OPENAI_API_KEY"]
}
```

#### MCP SQLite Example
```json
{
  "timeoutSec": 300,
  "successRegex": ["Connected to database", "Query results:", "products available"],
  "requiredEnv": ["OPENAI_API_KEY"],
  "setupCommands": ["./create-database.sh"]
}
```

#### Agentic Workflow Example
```json
{
  "timeoutSec": 600,
  "successRegex": ["EVALUATION:\\\\s+PASS", "Chain completed", "Final metrics"],
  "requiredEnv": ["OPENAI_API_KEY"]
}
```

---

## 4. JBang Launcher Template

### Standard Template (`integration-tests/Run*.java`)
```java
///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.zeroturnaround:zt-exec:1.12
//DEPS com.fasterxml.jackson.core:jackson-databind:2.17.1
//JAVA 17
//FILES ExampleInfo.json

import com.fasterxml.jackson.databind.*;
import org.zeroturnaround.exec.*;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import static java.lang.System.*;

record ExampleInfo(
    int timeoutSec, 
    String[] successRegex, 
    String[] requiredEnv,
    String[] setupCommands,
    String[] cleanupCommands
) {}

public class RunSample {
    public static void main(String... args) throws Exception {
        Path configPath = Path.of("integration-tests/ExampleInfo.json");
        ExampleInfo cfg = new ObjectMapper().readValue(configPath.toFile(), ExampleInfo.class);

        // Verify required environment variables
        for (String envVar : cfg.requiredEnv()) {
            if (getenv(envVar) == null) {
                err.println("❌ Missing required environment variable: " + envVar);
                exit(1);
            }
        }

        try {
            // Run setup commands if specified
            if (cfg.setupCommands() != null) {
                for (String setupCmd : cfg.setupCommands()) {
                    out.println("🔧 Running setup: " + setupCmd);
                    runCommand(setupCmd.split("\\s+"), 60); // 1 minute timeout for setup
                }
            }

            // Build and run the main application
            out.println("🏗️  Building application...");
            runCommand(new String[]{"./mvnw", "clean", "package", "-q"}, 300);

            out.println("🚀 Running application...");
            Path logFile = Files.createTempFile("integration-test", ".log");
            
            int exitCode = new ProcessExecutor()
                .command("./mvnw", "spring-boot:run", "-q")
                .timeout(cfg.timeoutSec(), TimeUnit.SECONDS)
                .redirectOutput(logFile.toFile())
                .redirectErrorStream(true)
                .execute()
                .getExitValue();

            // Verify output patterns
            String output = Files.readString(logFile);
            out.println("✅ Verifying output patterns...");
            
            int failedPatterns = 0;
            for (String pattern : cfg.successRegex()) {
                if (output.matches("(?s).*" + pattern + ".*")) {
                    out.println("  ✓ Found: " + pattern);
                } else {
                    err.println("  ❌ Missing: " + pattern);
                    failedPatterns++;
                }
            }

            Files.deleteIfExists(logFile);

            if (exitCode != 0) {
                err.println("❌ Application exited with code: " + exitCode);
                exit(exitCode);
            }

            if (failedPatterns > 0) {
                err.println("❌ Failed pattern verification: " + failedPatterns + " patterns missing");
                exit(1);
            }

            out.println("🎉 Integration test completed successfully!");

        } finally {
            // Run cleanup commands if specified
            if (cfg.cleanupCommands() != null) {
                for (String cleanupCmd : cfg.cleanupCommands()) {
                    out.println("🧹 Running cleanup: " + cleanupCmd);
                    try {
                        runCommand(cleanupCmd.split("\\s+"), 30);
                    } catch (Exception e) {
                        err.println("⚠️  Cleanup failed: " + e.getMessage());
                    }
                }
            }
        }
    }

    private static void runCommand(String[] cmd, int timeoutSec) throws Exception {
        int exit = new ProcessExecutor()
            .command(cmd)
            .timeout(timeoutSec, TimeUnit.SECONDS)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .execute()
            .getExitValue();
        
        if (exit != 0) {
            throw new RuntimeException("Command failed with exit code " + exit + ": " + String.join(" ", cmd));
        }
    }
}
```

---

## 5. Python CI Runner

### `scripts/run_integration_tests.py`
```python
#!/usr/bin/env python3
"""
Integration Test Runner for Spring AI Examples

Discovers all integration-tests/Run*.java launchers,
executes them via JBang, and verifies success patterns
declared in ExampleInfo.json files.
"""

import json
import subprocess
import re
import sys
import pathlib
import shutil
import tempfile
import concurrent.futures
import time
from typing import List, Tuple, Dict

# Configuration
JBANG = shutil.which("jbang")
MAX_WORKERS = 4  # Parallel test execution
VERBOSE = "--verbose" in sys.argv

def log(message: str, level: str = "INFO"):
    """Simple logging with timestamps"""
    timestamp = time.strftime("%H:%M:%S")
    print(f"[{timestamp}] {level}: {message}")

def find_integration_tests() -> List[pathlib.Path]:
    """Find all JBang integration test launchers"""
    launchers = list(pathlib.Path(".").rglob("integration-tests/Run*.java"))
    log(f"Found {len(launchers)} integration test launchers")
    return launchers

def run_integration_test(launcher: pathlib.Path) -> Tuple[pathlib.Path, bool, str]:
    """
    Run a single integration test
    Returns: (launcher_path, success, error_message)
    """
    info_file = launcher.parent / "ExampleInfo.json"
    
    if not info_file.exists():
        return launcher, False, f"Missing {info_file}"
    
    try:
        with info_file.open() as f:
            config = json.load(f)
    except json.JSONDecodeError as e:
        return launcher, False, f"Invalid JSON in {info_file}: {e}"
    
    log(f"Running {launcher.parent.parent.name}/{launcher.parent.name}")
    
    # Change to the module directory
    module_dir = launcher.parent.parent
    original_cwd = pathlib.Path.cwd()
    
    try:
        pathlib.Path.chdir(module_dir)
        
        # Create temporary file for output capture
        with tempfile.NamedTemporaryFile(mode="w+", delete=False, suffix=".log") as tmp:
            try:
                # Run the JBang script
                result = subprocess.run(
                    [JBANG, str(launcher.name)],
                    cwd=launcher.parent,
                    stdout=tmp,
                    stderr=subprocess.STDOUT,
                    text=True,
                    timeout=config.get("timeoutSec", 300)
                )
                
                # Read output for pattern verification
                tmp.seek(0)
                output = tmp.read()
                
                if VERBOSE:
                    log(f"Output from {launcher.name}:\\n{output}")
                
                # Check exit code
                if result.returncode != 0:
                    return launcher, False, f"Exit code {result.returncode}"
                
                # Verify success patterns
                success_patterns = config.get("successRegex", [])
                failed_patterns = []
                
                for pattern in success_patterns:
                    if not re.search(pattern, output, re.DOTALL):
                        failed_patterns.append(pattern)
                
                if failed_patterns:
                    return launcher, False, f"Missing patterns: {failed_patterns}"
                
                return launcher, True, "Success"
                
            except subprocess.TimeoutExpired:
                return launcher, False, f"Timeout after {config.get('timeoutSec', 300)}s"
            except Exception as e:
                return launcher, False, f"Execution error: {e}"
            finally:
                # Cleanup temp file
                pathlib.Path(tmp.name).unlink(missing_ok=True)
                
    finally:
        pathlib.Path.chdir(original_cwd)

def main():
    """Main execution function"""
    if not JBANG:
        log("JBang not found in PATH. Please install JBang.", "ERROR")
        sys.exit(1)
    
    log("Starting Spring AI Examples Integration Tests")
    
    # Find all integration tests
    launchers = find_integration_tests()
    if not launchers:
        log("No integration tests found", "WARNING")
        return
    
    # Run tests in parallel
    results = []
    failed_tests = []
    
    with concurrent.futures.ThreadPoolExecutor(max_workers=MAX_WORKERS) as executor:
        # Submit all tests
        future_to_launcher = {
            executor.submit(run_integration_test, launcher): launcher 
            for launcher in launchers
        }
        
        # Collect results
        for future in concurrent.futures.as_completed(future_to_launcher):
            launcher, success, message = future.result()
            results.append((launcher, success, message))
            
            if success:
                log(f"✅ {launcher.parent.parent.name}/{launcher.parent.name}")
            else:
                log(f"❌ {launcher.parent.parent.name}/{launcher.parent.name}: {message}", "ERROR")
                failed_tests.append((launcher, message))
    
    # Summary
    total_tests = len(results)
    passed_tests = sum(1 for _, success, _ in results if success)
    
    log(f"\\nIntegration Test Results:")
    log(f"  Total: {total_tests}")
    log(f"  Passed: {passed_tests}")
    log(f"  Failed: {len(failed_tests)}")
    
    if failed_tests:
        log("\\nFailed Tests:")
        for launcher, error in failed_tests:
            log(f"  - {launcher.parent.parent.name}/{launcher.parent.name}: {error}")
        sys.exit(1)
    else:
        log("🎉 All integration tests passed!")

if __name__ == "__main__":
    main()
```

---

## 6. GitHub Actions Workflow

### `.github/workflows/integration-tests.yml`
```yaml
name: Spring AI Examples Integration Tests

on:
  pull_request:
  push:
    branches: [main]
  schedule:
    - cron: '0 6 * * *'  # Daily at 6 AM UTC

jobs:
  unit-tests:
    name: Unit & Simple Tests
    runs-on: ubuntu-22.04
    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
          cache: 'maven'

      - name: Run unit tests
        run: ./mvnw -q test --batch-mode

  integration-tests:
    name: Integration Tests
    runs-on: ubuntu-22.04
    timeout-minutes: 45
    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
          cache: 'maven'

      - name: Install JBang
        run: |
          curl -Ls https://sh.jbang.dev | bash -s - app setup
          echo "$HOME/.jbang/bin" >> $GITHUB_PATH

      - name: Install Python dependencies
        run: |
          python3 -m pip install --upgrade pip

      - name: Run integration tests
        env:
          OPENAI_API_KEY: ${{ secrets.OPENAI_API_KEY }}
          BRAVE_API_KEY: ${{ secrets.BRAVE_API_KEY }}
        run: |
          python3 scripts/run_integration_tests.py --verbose

      - name: Upload test logs on failure
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: integration-test-logs
          path: |
            **/integration-tests/*.log
            **/target/spring-boot-run.log
          retention-days: 3

  cross-platform:
    name: Cross-Platform Tests
    strategy:
      matrix:
        os: [ubuntu-22.04, windows-2022, macos-13]
        java: [17, 21]
    runs-on: ${{ matrix.os }}
    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up JDK ${{ matrix.java }}
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: ${{ matrix.java }}
          cache: 'maven'

      - name: Install JBang (Unix)
        if: runner.os != 'Windows'
        run: |
          curl -Ls https://sh.jbang.dev | bash -s - app setup
          echo "$HOME/.jbang/bin" >> $GITHUB_PATH

      - name: Install JBang (Windows)
        if: runner.os == 'Windows'
        run: |
          choco install jbang

      - name: Run sample integration test
        env:
          OPENAI_API_KEY: ${{ secrets.OPENAI_API_KEY }}
        run: |
          # Run just one simple integration test for cross-platform verification
          python3 scripts/run_integration_tests.py --filter="kotlin-hello-world"
```

---

## 7. Developer Workflow & Commands

### Daily Development Commands

| Task | Command | Description |
|------|---------|-------------|
| Run unit tests for specific module | `cd module && ./mvnw test` | Fast feedback for development |
| Run integration test for specific module | `cd module && jbang integration-tests/Run*.java` | Manual integration testing |
| Run all integration tests | `python3 scripts/run_integration_tests.py` | Full integration test suite |
| Run integration tests with verbose output | `python3 scripts/run_integration_tests.py --verbose` | Debugging failed tests |
| Scaffold new integration test | `python3 scripts/scaffold_integration_test.py module-name` | Create boilerplate files |

### Development Workflow
1. **Create new example**: Follow standard Spring Boot conventions
2. **Add unit tests**: Place in `src/test/java/` for simple functionality testing
3. **Determine complexity**: Does it need external services, long timeouts, or complex verification?
4. **Add integration test** (if complex):
   - Create `integration-tests/` directory
   - Add `ExampleInfo.json` with appropriate configuration
   - Create `Run*.java` JBang launcher
   - Test locally: `jbang integration-tests/Run*.java`
5. **Verify CI integration**: Push and check GitHub Actions results

---

## 8. Learning-Based Implementation Plan

Each phase documents learnings and insights in `learnings/phase-N-insights.md` files to guide subsequent phases and ensure continuous improvement.

### 🚨 **Critical Implementation Principle**
**ALWAYS review ALL existing learnings documents before starting any implementation work**, including:
- All previous phase learnings (`phase-1-insights.md`, `phase-2-insights.md`, etc.)
- Current phase learnings (if any `phase-N-insights.md` exists for the current phase)
- Implementation summary and templates

This ensures that recent discoveries, architectural changes, and proven patterns are incorporated into all implementation decisions.

### Phase 1: Infrastructure Setup & Foundation Learning (Week 1)
**Before Starting**: Review existing bash scripts and testing patterns in the repository.

#### Tasks:
- [x] **Setup Python CI Runner**
  - [x] Create `scripts/run_integration_tests.py` runner
  - [x] Add parallel execution capability
  - [x] Include verbose logging and error reporting
  - [x] Document in `learnings/phase-1-insights.md`: Python script effectiveness, performance characteristics

- [x] **GitHub Actions Configuration**
  - [x] Set up basic GitHub Actions workflow with JBang installation
  - [x] Configure environment variable handling for API keys
  - [x] Test cross-platform compatibility (Ubuntu, Windows, macOS)
  - [x] Document in `learnings/phase-1-insights.md`: CI setup challenges, platform-specific issues

- [x] **Scaffolding Tools Development**
  - [x] Create `scripts/scaffold_integration_test.py` for rapid test creation
  - [x] Design template system for common test patterns
  - [x] Test scaffolding tool on sample modules
  - [x] Document in `learnings/phase-1-insights.md`: Template effectiveness, developer UX feedback

- [x] **Documentation & Developer Workflow**
  - [x] Create developer onboarding guide
  - [x] Document local testing procedures
  - [x] Create troubleshooting guide for common issues
  - [x] Document in `learnings/phase-1-insights.md`: Documentation gaps, developer pain points

- [x] **Phase 1 Learning Capture**
  - [x] Create `learnings/phase-1-insights.md` with findings
  - [x] Include: setup time, tool effectiveness, discovered edge cases
  - [x] Note: patterns that work well, patterns to avoid
  - [x] Recommendations for Phase 2 based on learnings

### Phase 2: Pilot Conversion & Pattern Validation (Week 2)
**Before Starting**: Review `learnings/phase-1-insights.md` for infrastructure insights and tool effectiveness.

#### Tasks:
- [x] **Simple Example Conversion** (`kotlin/kotlin-hello-world`)
  - [x] Add standard JUnit tests in `src/test/java/`
  - [x] Test with local Maven execution
  - [x] Validate CI integration
  - [x] Document timing, complexity, issues encountered

- [x] **Complex Example Conversion** (`agentic-patterns/chain-workflow`)
  - [x] Create `integration-tests/` directory
  - [x] Configure `ExampleInfo.json` with appropriate timeouts and success patterns
  - [x] Create JBang `RunChainWorkflow.java` launcher
  - [x] Test locally and in CI
  - [x] Document pattern verification challenges, timeout tuning

- [x] **Very Complex Example Conversion** (`model-context-protocol/sqlite/simple`)
  - [x] Handle database setup requirements
  - [x] Configure environment dependencies (uvx, SQLite MCP server)
  - [x] Create comprehensive success pattern matching
  - [x] Test with external service dependencies
  - [x] Document dependency management strategies, reliability issues

- [x] **Pattern Analysis & Refinement**
  - [x] Compare conversion effort across complexity levels
  - [x] Identify common configuration patterns
  - [x] Refine templates based on real implementation
  - [x] Test different success pattern strategies

- [x] **Phase 2 Learning Capture**
  - [x] Create `learnings/phase-2-insights.md` with conversion patterns
  - [x] Include: time estimates per complexity level, effective success patterns
  - [x] Note: template improvements needed, configuration edge cases
  - [x] Document dependency management best practices
  - [x] Recommendations for batch conversion approach

### Phase 3a: Critical UX & Test Validation (Week 3 - Immediate Priority) 🚨
**Before Starting**: Review ALL learnings documents (`learnings/phase-1-insights.md`, `learnings/phase-2-insights.md`, and any existing `learnings/phase-3-insights.md`) for tool improvements, proven patterns, and recent discoveries.

**⚠️ CRITICAL PHASE**: This phase addresses fundamental UX and validation issues discovered during initial Phase 3 testing that prevent effective framework usage.

#### Tasks:
- [x] **Update Integration Testing Plan** ✅ **COMPLETED**
  - [x] Split Phase 3 into sub-phases to address critical streaming UX issue 
  - [x] Restructure plan to prioritize systematic test validation
  - [x] Document the need for real-time Spring Boot output visibility

- [x] **Simple Examples Batch Conversion** ✅ **COMPLETED**
  - [x] Apply learnings from Phase 2 to remaining simple examples
  - [x] Use proven templates and patterns from `learnings/phase-2-insights.md`
  - [x] Convert: `models/chat/helloworld`, `misc/openai-streaming-response`, `misc/spring-ai-java-function-callback`
  - [x] Convert: `kotlin/kotlin-function-callback`, `prompt-engineering/prompt-engineering-patterns`
  - [x] Track conversion time and success rate

- [x] **Live Progress Indication Implementation** (Priority: Critical UX Issue) ✅ **COMPLETED**
  - [x] Add `--stream` flag to show real-time test output
  - [x] Implement file-based logging with timestamped logs in `logs/` directory
  - [x] Add progress indicators and structured output display during test execution
  - [x] Show timeout countdown for long-running tests
  - [x] Test with long-running examples like prompt-engineering-patterns

- [x] **Enhanced Log Management & Developer Experience** ✅ **COMPLETED**
  - [x] Add Python cache and log files to .gitignore (completed)
  - [x] Implement structured log directories with `logs/run-<timestamp>/` organization for iteration debugging
  - [x] Add `--clean-logs` command to Python test runner for log cleanup
  - [x] Add test report management guidance (test reports added to .gitignore as they are generated artifacts)
  - [x] Create log directory structure for debugging multiple test iterations

- [x] **Critical JBang Output Capture Fix** ✅ **COMPLETED**
  - [x] Discovered and fixed critical bug: 10 out of 12 JBang scripts missing "📋 Captured Application Output" display sections
  - [x] Fixed 8/12 scripts (helloworld, kotlin-hello-world, chain-workflow, sqlite/simple, prompt-engineering-patterns, kotlin-function-callback, filesystem, evaluator-optimizer)
  - [x] Enhanced output capture with multi-line AI response handling
  - [x] Added module-specific content filters for better debugging visibility

- [ ] **Complete Remaining JBang Output Fixes** (Critical for debugging)
  - [ ] Fix remaining 4 scripts: brave, openai-streaming-response, spring-ai-java-function-callback, parallelization-workflow
  - [ ] Apply same output display pattern used in working scripts
  - [ ] Test each fix individually to ensure proper output capture

- [ ] **Real-time Spring Boot Output Streaming** (Critical UX Issue)
  - [ ] **Problem**: Current `--stream` only shows JBang overhead, not actual Spring Boot application output
  - [ ] **Solution**: Implement Python log tailing while JBang runs for real-time Spring Boot visibility
  - [ ] Replace current dual output strategy with simpler log tailing approach
  - [ ] Test with long-running examples to ensure developers can see live Spring Boot execution

- [ ] **Systematic Test Validation & Pattern Repair**
  - [ ] Run all 12 existing integration tests individually to identify failing patterns
  - [ ] Fix incorrect success patterns in ExampleInfo.json files (e.g., prompt-engineering-patterns patterns)
  - [ ] Validate that each test passes individually before proceeding with batch conversions
  - [ ] Document pattern validation methodology for future tests

- [ ] **Phase 3a Learning Capture**
  - [ ] Update `learnings/phase-3-insights.md` with critical UX and validation discoveries
  - [ ] Document streaming architecture issues and solutions
  - [ ] Note systematic testing approach for pattern validation
  - [ ] Record JBang output capture bug patterns to prevent regression

### Phase 3b: Continue Batch Conversion (Week 3-4)
**Prerequisites**: Phase 3a must be completed first - all existing tests must pass and real-time output streaming must work.

#### Tasks:
- [ ] **MCP Examples Conversion** (Week 3-4)
  - [ ] Apply complex example patterns to all MCP modules
  - [ ] Handle varied dependency requirements (databases, external APIs)
  - [ ] Create reusable configuration templates for MCP patterns
  - [ ] Document resource contention issues, timing conflicts (Note: parallel testing disabled due to port conflicts)

- [ ] **Agentic Pattern Examples** (Week 4)
  - [ ] Convert remaining agentic pattern examples
  - [ ] Handle long-running workflow validation
  - [ ] Create specialized success patterns for multi-step processes
  - [ ] Test timeout configurations for complex workflows

- [ ] **Special Cases** (Week 4)
  - [ ] Convert `kotlin/rag-with-kotlin` (database setup complexity)
  - [ ] Convert `agents/reflection` (complex AI interactions)
  - [ ] Handle any edge cases discovered during batch conversion

### Phase 3c: Scale Testing & Optimization (Week 4)
**Prerequisites**: Phase 3a and 3b must be completed - all integration tests must exist and pass individually.

#### Tasks:
- [ ] **Scale Testing & Optimization**
  - [ ] Run full test suite multiple times to identify patterns
  - [ ] Test parallel execution limits and resource usage (after implementing dynamic port assignment)
  - [ ] Optimize CI pipeline performance
  - [ ] Identify and fix flaky test patterns

- [ ] **Phase 3 Learning Capture**
  - [ ] Update `learnings/phase-3-insights.md` with complete batch conversion insights
  - [ ] Include: efficiency patterns, resource usage insights, flakiness patterns
  - [ ] Note: successful template variations, problematic configurations
  - [ ] Document scaling challenges and solutions
  - [ ] Performance benchmarks and optimization opportunities

### Phase 4: Validation, Cleanup & Knowledge Synthesis (Week 5)
**Before Starting**: Review ALL learnings documents (`learnings/phase-1-insights.md`, `learnings/phase-2-insights.md`, `learnings/phase-3-insights.md`, and any `learnings/phase-4-insights.md` created during this phase) for comprehensive improvement opportunities and latest discoveries.

#### Tasks:
- [ ] **Comprehensive Testing & Flaky Test Resolution**
  - [ ] Run full test suite 10+ times to establish baseline reliability
  - [ ] Identify and fix flaky tests using patterns from `learnings/phase-3-insights.md`
  - [ ] Optimize timeout configurations based on collected performance data
  - [ ] Refine success patterns to reduce false positives/negatives

- [ ] **Configuration Optimization**
  - [ ] Apply all learnings to optimize `ExampleInfo.json` configurations
  - [ ] Standardize patterns for similar example types
  - [ ] Create configuration validation tools
  - [ ] Document configuration best practices

- [ ] **Legacy System Removal**
  - [ ] Remove bash scripts only after validating complete parity
  - [ ] Update all README files with new testing commands
  - [ ] Update CLAUDE.md with refined development workflow
  - [ ] Archive old documentation while preserving historical context

- [ ] **Documentation Enhancement**
  - [ ] Create comprehensive troubleshooting guide based on all phase learnings
  - [ ] Update developer onboarding with proven workflows
  - [ ] Create maintainer guide for future example additions
  - [ ] Document performance expectations and scaling limits

- [ ] **Knowledge Synthesis & Future Planning**
  - [ ] Create `learnings/implementation-summary.md` synthesizing all phase insights
  - [ ] Document proven patterns and anti-patterns
  - [ ] Create template library for future examples
  - [ ] Plan monitoring and maintenance procedures

- [ ] **Phase 4 Learning Capture**
  - [ ] Create `learnings/phase-4-insights.md` with validation and cleanup learnings
  - [ ] Include: reliability improvements, optimization results, maintenance insights
  - [ ] Document final performance metrics and success rates
  - [ ] Create recommendations for ongoing maintenance
  - [ ] Plan future enhancement priorities based on accumulated knowledge

---

## 9. Success Metrics & Monitoring

### Quantitative Metrics

| Metric | Target | Current | Measurement Method |
|--------|--------|---------|-------------------|
| Complex examples with integration tests | 100% (18/18) | 0% | Count of `integration-tests/` directories |
| Simple examples with unit tests | 100% (15/15) | ~60% | Count of modules with tests in `src/test/` |
| CI pipeline duration (full suite) | ≤ 20 minutes | N/A | GitHub Actions workflow time |
| Test flakiness rate | < 2% | N/A | Failed runs / total runs over 10 runs |
| Cross-platform compatibility | 100% | N/A | Matrix job success rate |

### Qualitative Metrics
- **Developer Experience**: Time from "clone repo" to "run tests locally"
- **Maintenance Overhead**: Effort required to add new examples
- **Debugging Capability**: Ability to isolate and fix test failures
- **Documentation Quality**: Clarity of setup and troubleshooting guides

---

## 10. Risk Assessment & Mitigation

### High Impact Risks

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|-------------------|
| **API Rate Limits** | Medium | High | Use test doubles/mocks for CI; reserve real API calls for critical paths |
| **Flaky Network Tests** | High | Medium | Implement retry logic; use local services where possible |
| **Environment Setup Complexity** | Medium | High | Containerize dependencies; provide clear setup documentation |
| **CI Resource Constraints** | Low | High | Implement intelligent test selection; parallel execution |

### Medium Impact Risks

| Risk | Probability | Impact | Mitigation Strategy |
|------|-------------|--------|-------------------|
| **JBang Installation Issues** | Low | Medium | Pre-install in CI; provide alternative installation methods |
| **Cross-Platform Path Issues** | Medium | Low | Use Path API consistently; test on Windows/Mac/Linux |
| **Test Timeout Variations** | Medium | Medium | Environment-specific timeout configurations |

---

## 11. Troubleshooting Guide

### Common Issues & Solutions

#### "JBang not found"
```bash
# Install JBang
curl -Ls https://sh.jbang.dev | bash -s - app setup
# Or use package manager
brew install jbang  # macOS
choco install jbang  # Windows
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

---

## 12. Future Enhancements

### Short-term (Next 6 months)
- [x] **Real-time Test Output Window**: Stream live test output for long-running tests with progress indicators ✅ **PARTIALLY COMPLETED** - `--stream` flag implemented, but needs real-time Spring Boot output streaming via log tailing
- [ ] **Parallel Execution with Port Management**: Dynamic port assignment or test categorization to enable safe parallel execution (CRITICAL: currently disabled due to port conflicts)
- [ ] **Intelligent Test Selection**: Run only tests affected by code changes
- [ ] **Performance Benchmarking**: Track execution time trends
- [ ] **Test Result Caching**: Skip unchanged tests when possible
- [ ] **Local Development Dashboard**: Web UI for test results

### Medium-term (6-12 months)
- [ ] **AI-Powered Test Generation**: Auto-generate integration tests from README files
- [ ] **Visual Regression Testing**: Screenshot comparisons for UI-based examples
- [ ] **Load Testing Integration**: Performance testing for high-throughput examples
- [ ] **Multi-Environment Testing**: Test against different AI model providers

### Long-term (12+ months)
- [ ] **Self-Healing Tests**: Automatically update success patterns based on output changes
- [ ] **Distributed Testing**: Run tests across multiple cloud providers
- [ ] **Integration with Spring AI Framework**: Contribute testing patterns back to core framework

---

## 13. Learning Documentation Structure

### Learning File Template (`learnings/phase-N-insights.md`)

Each phase creates a structured learning document following this template:

```markdown
# Phase N Implementation Insights

**Implementation Period**: [Start Date] - [End Date]  
**Phase Focus**: [Brief description of phase objectives]

## Summary
Brief overview of what was accomplished and key learnings.

## What Worked Well ✅

### [Category 1 - e.g., Tool Effectiveness]
- **Finding**: [Specific observation]
- **Evidence**: [Data/examples supporting this]
- **Impact**: [Why this matters for future phases]

### [Category 2 - e.g., Process Efficiency]
- **Finding**: [Specific observation]
- **Evidence**: [Data/examples supporting this]
- **Impact**: [Why this matters for future phases]

## Challenges & Issues ❌

### [Challenge 1 - e.g., Configuration Complexity]
- **Problem**: [Description of the issue]
- **Root Cause**: [Why this happened]
- **Solution Applied**: [How it was resolved]
- **Prevention**: [How to avoid in future phases]

### [Challenge 2 - e.g., Performance Issues]
- **Problem**: [Description of the issue]
- **Root Cause**: [Why this happened]
- **Solution Applied**: [How it was resolved]
- **Prevention**: [How to avoid in future phases]

## Key Metrics & Data 📊

| Metric | Target | Actual | Notes |
|--------|--------|--------|-------|
| [Metric 1] | [Target] | [Actual] | [Context] |
| [Metric 2] | [Target] | [Actual] | [Context] |

## Patterns & Anti-Patterns 🔄

### Effective Patterns ✨
1. **[Pattern Name]**: [Description and when to use]
2. **[Pattern Name]**: [Description and when to use]

### Anti-Patterns to Avoid ⚠️
1. **[Anti-Pattern Name]**: [Description and why to avoid]
2. **[Anti-Pattern Name]**: [Description and why to avoid]

## Technical Insights 🔧

### Configuration Discoveries
- **[Configuration Type]**: [What was learned about optimal settings]
- **[Configuration Type]**: [What was learned about optimal settings]

### Tool & Technology Insights
- **[Tool Name]**: [Effectiveness, limitations, best practices]
- **[Tool Name]**: [Effectiveness, limitations, best practices]

## Recommendations for Next Phase 🎯

### Immediate Actions
- [ ] **[Action 1]**: [Description and rationale]
- [ ] **[Action 2]**: [Description and rationale]

### Process Improvements
- [ ] **[Improvement 1]**: [Description and expected benefit]
- [ ] **[Improvement 2]**: [Description and expected benefit]

### Template/Tool Updates
- [ ] **[Update 1]**: [What needs to be changed and why]
- [ ] **[Update 2]**: [What needs to be changed and why]

## Cross-Phase Dependencies

### Prerequisites for Next Phase
- [ ] **[Requirement 1]**: [What must be completed before next phase]
- [ ] **[Requirement 2]**: [What must be completed before next phase]

### Information to Transfer
- **[Data Type 1]**: [What information needs to carry forward]  
- **[Data Type 2]**: [What information needs to carry forward]

## Appendix

### Detailed Examples
[Include specific examples, code snippets, or detailed analysis that supports the insights above]

### Raw Data
[Include any performance measurements, timing data, error logs, etc.]
```

### Learning Integration Process

1. **Phase Preparation**: Each phase begins by reviewing ALL previous learning files
2. **Active Documentation**: During implementation, continuously update learning insights
3. **Phase Completion**: Finalize learning document with comprehensive analysis
4. **Knowledge Transfer**: Next phase explicitly references and builds upon previous learnings
5. **Cross-Reference**: Maintain links between related insights across phases

### Learning File Locations

```
learnings/
├── phase-1-insights.md           # Infrastructure setup learnings
├── phase-2-insights.md           # Pilot conversion patterns
├── phase-3-insights.md           # Batch conversion & scaling insights
├── phase-4-insights.md           # Validation & cleanup learnings
├── implementation-summary.md     # Synthesis of all phase insights
└── templates/
    ├── integration-test-configs/ # Proven ExampleInfo.json templates
    ├── jbang-launchers/         # Successful JBang launcher patterns
    └── troubleshooting/         # Common issues and solutions
```

---

## 14. Appendix: Template Files

### A. Simple Unit Test Template
```java
package com.example.kotlin.hello;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.ai.openai.api-key=test-key"
})
class KotlinHelloWorldApplicationTests {

    @Test
    void contextLoads() {
        // Simple smoke test - application context should load
    }
}
```

### B. Integration Test Scaffolding Script
```python
#!/usr/bin/env python3
"""
Scaffold integration test files for a Spring AI example
Usage: python3 scripts/scaffold_integration_test.py <module-path>
"""

import sys
import pathlib
import json

def create_integration_test(module_path: str):
    module_dir = pathlib.Path(module_path)
    if not module_dir.exists():
        print(f"Module directory not found: {module_path}")
        sys.exit(1)
    
    integration_dir = module_dir / "integration-tests"
    integration_dir.mkdir(exist_ok=True)
    
    # Create ExampleInfo.json
    config = {
        "timeoutSec": 300,
        "successRegex": ["BUILD SUCCESS", "Started.*Application"],
        "requiredEnv": ["OPENAI_API_KEY"]
    }
    
    with open(integration_dir / "ExampleInfo.json", "w") as f:
        json.dump(config, f, indent=2)
    
    # Create Run*.java launcher
    module_name = module_dir.name.replace("-", "").title()
    launcher_content = f"""///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.zeroturnaround:zt-exec:1.12
//DEPS com.fasterxml.jackson.core:jackson-databind:2.17.1
//JAVA 17
//FILES ExampleInfo.json

// Integration test launcher for {module_name}
// Generated by scaffold_integration_test.py

import com.fasterxml.jackson.databind.*;
import org.zeroturnaround.exec.*;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import static java.lang.System.*;

record ExampleInfo(int timeoutSec, String[] successRegex, String[] requiredEnv) {{}}

public class Run{module_name} {{
    public static void main(String... args) throws Exception {{
        // Load configuration
        Path configPath = Path.of("integration-tests/ExampleInfo.json");
        ExampleInfo cfg = new ObjectMapper().readValue(configPath.toFile(), ExampleInfo.class);

        // Verify environment variables
        for (String envVar : cfg.requiredEnv()) {{
            if (getenv(envVar) == null) {{
                err.println("❌ Missing required environment variable: " + envVar);
                exit(1);
            }}
        }}

        // Build and run
        out.println("🏗️  Building {module_name}...");
        new ProcessExecutor()
            .command("./mvnw", "clean", "package", "-q")
            .timeout(120, TimeUnit.SECONDS)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .executeNoTimeout();

        out.println("🚀 Running {module_name}...");
        Path logFile = Files.createTempFile("integration-test", ".log");
        
        int exitCode = new ProcessExecutor()
            .command("./mvnw", "spring-boot:run", "-q")
            .timeout(cfg.timeoutSec(), TimeUnit.SECONDS)
            .redirectOutput(logFile.toFile())
            .redirectErrorStream(true)
            .execute()
            .getExitValue();

        // Verify patterns
        String output = Files.readString(logFile);
        int failedPatterns = 0;
        
        for (String pattern : cfg.successRegex()) {{
            if (output.matches("(?s).*" + pattern + ".*")) {{
                out.println("✅ Found pattern: " + pattern);
            }} else {{
                err.println("❌ Missing pattern: " + pattern);
                failedPatterns++;
            }}
        }}

        Files.deleteIfExists(logFile);

        if (exitCode != 0 || failedPatterns > 0) {{
            err.println("❌ Integration test failed");
            exit(1);
        }}

        out.println("🎉 Integration test passed!");
    }}
}}
"""
    
    with open(integration_dir / f"Run{module_name}.java", "w") as f:
        f.write(launcher_content)
    
    print(f"✅ Created integration test for {module_path}")
    print(f"   - {integration_dir}/ExampleInfo.json")
    print(f"   - {integration_dir}/Run{module_name}.java")
    print(f"\\nTo test: cd {module_path} && jbang integration-tests/Run{module_name}.java")

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python3 scripts/scaffold_integration_test.py <module-path>")
        sys.exit(1)
    
    create_integration_test(sys.argv[1])
```

This plan provides a lightweight, practical approach to testing the Spring AI examples while maintaining developer productivity and CI efficiency.