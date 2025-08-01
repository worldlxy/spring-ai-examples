# AI-Based Validation Plan for Spring AI Examples

## Overview and Objectives

This plan outlines the integration of AI-powered validation into the Spring AI integration testing framework. The validation will use Claude Code (via the battle-tested `claude_code_wrapper.py`) to intelligently analyze application logs and determine if example applications successfully demonstrated their intended functionality.

### Key Insights

- **These are example applications, not tests** - They demonstrate Spring AI functionality
- **Success means**:
  1. The application ran without exceptions
  2. It demonstrated the functionality described in its README
  3. For multi-component examples, all parts worked together correctly
- **AI validation excels at**:
  - Understanding context from logs and documentation
  - Validating unpredictable AI-generated responses
  - Determining if complex workflows completed successfully
  - Analyzing multi-component interactions

### Benefits

1. **Intelligent Assessment**: Understands if examples achieved their purpose beyond pattern matching
2. **Handles AI Outputs**: Validates unpredictable AI-generated content (jokes, conversations, etc.)
3. **Context-Aware**: Uses README documentation to understand intended behavior
4. **Multi-Component Support**: Validates distributed examples (client/server) holistically
5. **Clear Reasoning**: Provides detailed explanations for pass/fail decisions
6. **Flexible Adoption**: Can be enabled selectively per example

## Design Approach

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    JBang Integration Test                    │
│  ┌─────────────────────────────────────────────────────┐   │
│  │           IntegrationTestUtils.java                  │   │
│  │  ┌─────────────────┐      ┌────────────────────┐   │   │
│  │  │ Run Spring Boot │      │ Capture Log Output │   │   │
│  │  └────────┬────────┘      └──────────┬─────────┘   │   │
│  │           │                           │              │   │
│  │           ▼                           ▼              │   │
│  │  ┌─────────────────┐      ┌────────────────────┐   │   │
│  │  │ Regex Validation│      │  AI Validation     │   │   │
│  │  │   (optional)    │      │  performAI...()    │   │   │
│  │  └─────────────────┘      └──────────┬─────────┘   │   │
│  └───────────────────────────────────────┼─────────────┘   │
│                                          │                   │
│                    Process Boundary      │                   │
│  ┌───────────────────────────────────────▼─────────────┐   │
│  │              validate_example.py                     │   │
│  │  ┌─────────────────┐      ┌────────────────────┐   │   │
│  │  │ Load Log File   │      │ Read README.md     │   │   │
│  │  └────────┬────────┘      └──────────┬─────────┘   │   │
│  │           │                           │              │   │
│  │           ▼                           ▼              │   │
│  │  ┌───────────────────────────────────────────────┐ │   │
│  │  │          claude_code_wrapper.py               │ │   │
│  │  │    Analyze logs + context → Pass/Fail        │ │   │
│  │  └───────────────────────────────────────────────┘ │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### Configuration Schema

The `ExampleInfo.json` will be extended with an optional `aiValidation` section:

```json
{
  "timeoutSec": 300,
  "successRegex": ["BUILD SUCCESS"],  // Optional, kept for basic checks
  "requiredEnv": ["OPENAI_API_KEY"],
  "aiValidation": {
    "enabled": true,
    "validationMode": "primary|hybrid|fallback",
    "readmeFile": "../README.md",  // Optional, for additional context
    "expectedBehavior": "Description of what the example should demonstrate",
    "components": ["server", "client"],  // For multi-component examples
    "promptTemplate": "example_validation",  // Template to use
    "successCriteria": {
      "requiresUserInteraction": true,
      "expectedOutputTypes": ["conversation", "data_transformation", "api_response"]
    }
  }
}
```

### Validation Modes

1. **primary**: Use only AI validation (recommended for AI-response examples)
2. **hybrid**: Both regex and AI must pass (for examples with deterministic + AI components)
3. **fallback**: Use AI validation if regex patterns fail

## Implementation Plan - ✅ COMPLETED PHASES 1-3

### Phase 1: Infrastructure Setup ✅ COMPLETED
- ✅ Create directory structure: `integration-testing/ai-validator/`
- ✅ Copy `claude_code_wrapper.py` to `integration-testing/ai-validator/lib/`
- ✅ Create `requirements.txt` with dependencies:
  ```
  pathlib
  subprocess
  json
  typing
  dataclasses
  ```
- ✅ Create `animated_progress.py` (copy from pr-review or create minimal version)
- ✅ Set up basic directory structure:
  ```
  integration-testing/
  └── ai-validator/
      ├── lib/
      │   ├── claude_code_wrapper.py (✅ with quiet mode)
      │   └── animated_progress.py
      ├── templates/
      │   ├── example_validation.md
      │   ├── chat_example_validation.md
      │   ├── workflow_validation.md
      │   └── client_server_validation.md
      ├── validate_example.py (✅ full implementation)
      └── requirements.txt
  ```

### Phase 2: Core Validation Logic ✅ COMPLETED
- ✅ Implement `validate_example.py` with main validation function:
  ```python
  def validate_example(
      log_path: str,
      example_name: str,
      readme_path: Optional[str] = None,
      expected_behavior: Optional[str] = None,
      components: Optional[List[str]] = None,
      prompt_template: str = "example_validation"
  ) -> Dict[str, Any]
  ```
- ✅ Implement log reading and preprocessing
- ✅ Implement README parsing for context extraction
- ✅ Create prompt building logic using templates
- ✅ Integrate with `claude_code_wrapper.py` with **quiet mode** for clean JSON
- ✅ Implement response parsing and structured output:
  ```json
  {
    "success": true,
    "confidence": 0.95,
    "reasoning": "Detailed explanation of the validation decision...",
    "components_validated": ["server", "client"],
    "functionality_demonstrated": [
      "Application started successfully",
      "Chat interaction worked as expected",
      "AI responses were coherent and appropriate"
    ],
    "issues_found": [],
    "recommendations": []
  }
  ```
- ✅ Add error handling and timeout management
- ✅ Implement multi-component log combination logic

### Phase 3: Java Integration ✅ COMPLETED
- ✅ Extend `ExampleInfo` record to include `AIValidationConfig`:
  ```java
  public record AIValidationConfig(
      boolean enabled,
      String validationMode,
      String readmeFile,
      String expectedBehavior,
      String[] components,
      String promptTemplate,
      Map<String, Object> successCriteria
  ) {}
  ```
- ✅ Add `AIValidationConfig aiValidation` field to `ExampleInfo` record
- ✅ Implement `performAIValidation()` method in `IntegrationTestUtils.java`:
  ```java
  public static boolean performAIValidation(
      String output, 
      Path logFile, 
      ExampleInfo cfg
  ) throws Exception
  ```
- ✅ Add Python script invocation using ProcessExecutor
- ✅ Parse JSON response from Python script (simplified with quiet mode)
- ✅ Integrate validation results into test flow
- ✅ Add appropriate logging and error messages
- ✅ Handle validation mode logic (primary/hybrid/fallback)
- ✅ **Path resolution fixed** - Works from different module depths
- ✅ **End-to-end testing** - Successfully validated helloworld example

### ⚡ **Key Innovation: Quiet Mode**
- ✅ Added `quiet` parameter to `claude_code_wrapper.py` methods
- ✅ Eliminates brittle JSON extraction logic in Java
- ✅ Direct JSON parsing instead of complex text filtering  
- ✅ Much more reliable and maintainable integration

### Phase 4: Prompt Templates ✅ COMPLETED (Basic Set)
- ✅ Create `example_validation.md` base template
- ✅ Create `chat_example_validation.md` for conversational examples
- ✅ Create `workflow_validation.md` for multi-step processes  
- ✅ Create `client_server_validation.md` for distributed examples
- ✅ Add template variable substitution logic with proper JSON escaping
- ✅ Include specific validation criteria per template type

### Phase 5: Cost Reporting & Analysis 🚧 CURRENT PRIORITY
- [ ] **Implement cost reporting in AI validation results**
  - [ ] Extract cost information from claude_code_wrapper JSON output
  - [ ] Add cost summary to validation output logs
  - [ ] Create cost tracking dashboard/summary
- [ ] **Port Kotlin test to AI validation**
  - [ ] Update `kotlin/kotlin-hello-world` ExampleInfo.json 
  - [ ] Test AI validation with Kotlin chat example
  - [ ] Verify cost tracking works correctly
- [ ] **Validate `rit-direct.sh` integration**
  - [ ] Run full integration test suite with AI validation enabled
  - [ ] Verify AI costs are properly reported across all tests
  - [ ] Document cost implications and budgeting considerations

### Phase 6: Testing & Rollout
- ✅ Test with `models/chat/helloworld` (simple chat example) - **SUCCESS**
- [ ] Test with `agentic-patterns/chain-workflow` (complex workflow)
- [ ] Test with `model-context-protocol/weather/starter-webmvc-server` (client/server)
- [ ] Test error scenarios (exceptions, timeouts, missing functionality)
- [ ] Update `scaffold_integration_test.py` to support AI validation config
- [ ] Create documentation:
  - [ ] Add AI validation section to `integration-testing/docs/README.md`
  - [ ] Create `integration-testing/docs/AI_VALIDATION.md` guide
  - [ ] Update `CLAUDE.md` with AI validation information
- [ ] Create example configurations for different scenarios
- [ ] Performance testing and optimization
- [ ] Create migration guide for existing tests

### Phase 7: Test Migration Strategy  
Based on analysis of current 18 tests, prioritize migration for:

#### **High Priority (AI-Generated Content):**
- [ ] `kotlin/kotlin-hello-world` - Kotlin chat example
- [ ] `model-context-protocol/client-starter/starter-default-client` - MCP chat with tools
- [ ] `agentic-patterns/evaluator-optimizer` - AI evaluates and optimizes content
- [ ] `agentic-patterns/chain-workflow` - Multi-step AI workflow 
- [ ] `agentic-patterns/orchestrator-workers` - Complex AI coordination
- [ ] `agentic-patterns/routing-workflow` - AI decision making
- [ ] `misc/spring-ai-java-function-callback` - Weather function calls
- [ ] `kotlin/kotlin-function-callback` - Kotlin function calling

#### **Medium Priority (Structured Output):**
- [ ] `prompt-engineering/prompt-engineering-patterns` - Various AI output patterns
- [ ] `misc/openai-streaming-response` - Streaming AI responses

#### **Migration Benefits:**
- **Unpredictable AI responses** - Chat examples generate different jokes/conversations
- **Complex workflows** - Agentic patterns have multi-step AI reasoning hard to validate with regex  
- **Function calling** - Need to verify proper tool usage and realistic responses
- **Context understanding** - MCP examples need to show proper tool integration

## Technical Implementation Details

### File Structure
```
integration-testing/
├── ai-validator/
│   ├── lib/
│   │   ├── claude_code_wrapper.py
│   │   └── animated_progress.py
│   ├── templates/
│   │   ├── example_validation.md
│   │   ├── chat_example_validation.md
│   │   ├── workflow_validation.md
│   │   └── client_server_validation.md
│   ├── validate_example.py
│   ├── requirements.txt
│   └── README.md
├── jbang-lib/
│   └── IntegrationTestUtils.java  # Enhanced with AI validation
├── scripts/
│   └── scaffold_integration_test.py  # Updated for AI config
└── docs/
    ├── README.md  # Updated with AI validation section
    └── AI_VALIDATION.md  # New comprehensive guide
```

### Example Configurations

#### Simple Chat Example (helloworld)
```json
{
  "timeoutSec": 120,
  "successRegex": ["Spring AI Hello World!"],
  "requiredEnv": ["OPENAI_API_KEY"],
  "aiValidation": {
    "enabled": true,
    "validationMode": "primary",
    "expectedBehavior": "The application should accept user input 'tell me a joke' and return a coherent joke response from the AI assistant",
    "promptTemplate": "chat_example_validation"
  }
}
```

#### Complex Workflow Example (chain-workflow)
```json
{
  "timeoutSec": 300,
  "successRegex": ["Customer Satisfaction", "Revenue Growth"],
  "requiredEnv": ["OPENAI_API_KEY"],
  "aiValidation": {
    "enabled": true,
    "validationMode": "hybrid",
    "readmeFile": "../README.md",
    "expectedBehavior": "Process Q3 performance data through 4 transformation steps: extract values, standardize format, sort, and create markdown table",
    "promptTemplate": "workflow_validation",
    "successCriteria": {
      "expectedSteps": 4,
      "outputFormat": "markdown_table"
    }
  }
}
```

#### Client-Server Example (MCP weather)
```json
{
  "timeoutSec": 300,
  "requiredEnv": ["OPENAI_API_KEY"],
  "aiValidation": {
    "enabled": true,
    "validationMode": "primary",
    "components": ["server", "client"],
    "expectedBehavior": "MCP weather server should start successfully and handle client requests for weather information via getWeatherForecastByLocation and getAlerts tools",
    "promptTemplate": "client_server_validation",
    "successCriteria": {
      "requiresUserInteraction": false,
      "expectedOutputTypes": ["api_response", "weather_data"]
    }
  }
}
```

## Success Criteria and Metrics

### Implementation Success Metrics
- [ ] All example types can be validated (chat, workflow, client/server)
- [ ] AI validation correctly identifies successful runs
- [ ] AI validation correctly identifies failures (exceptions, missing functionality)
- [ ] Validation provides clear, actionable reasoning
- [ ] Performance: AI validation completes within 30 seconds
- [ ] Zero false positives in validation results
- [ ] Minimal false negatives (< 5%)

### Expected Outcomes
1. **Improved Validation Quality**: More accurate assessment of example functionality
2. **Reduced Maintenance**: Less need to update regex patterns for AI outputs
3. **Better Developer Experience**: Clear explanations of why examples passed/failed
4. **Broader Coverage**: Ability to validate previously hard-to-test examples
5. **Future-Proof**: Handles new example types without major changes

## Next Steps

After implementation:
1. Monitor validation accuracy and gather feedback
2. Fine-tune prompt templates based on results
3. Consider caching validation results for identical outputs
4. Explore batch validation for multiple examples
5. Add support for custom validation plugins