# GitHub Actions CI/CD Implementation Plan

## Overview
Implement GitHub Actions integration testing with multi-version Spring AI support through a systematic, phased approach. All state is tracked via checkboxes in this plan and learnings documents.

**Priority Focus**: Version management (1.0.1 ↔ 1.1.0-SNAPSHOT) and multi-version test matrix validation are higher risk than full coverage, so we tackle these first.

## Reference Architecture
This plan leverages proven patterns from the Spring AI repository's GitHub Actions workflow (see `integration-testing/plans/spring-ai-github-build-workflow.yml`), including:
- JDK 17 setup with Temurin distribution and Maven caching
- Environment variable management for API keys (`OPENAI_API_KEY`, `SPRING_AI_OPENAI_API_KEY`)
- Branch-based triggering (`main`, `*.*.x` pattern)
- Organization-specific conditions (`github.repository_owner`)
- Maven batch mode execution with update snapshots

Note: Our implementation will use simpler patterns suitable for examples repository (no Artifactory, docs deployment, or SSH keys).

## Goals (Reprioritized)
1. **Priority 1**: Version management scripts (1.0.1 ↔ 1.1.0-SNAPSHOT)
2. **Priority 2**: Multi-version test matrix validation with 3 examples
3. **Priority 3**: Full coverage rollout (97% local reliability gives confidence)
4. Enable automated integration testing via GitHub Actions
5. Provide manual and automatic workflow triggers
6. Create comprehensive documentation for maintenance

---

## Phase 1: Foundation & Script Renaming

*Same priority as before - needed for clear naming*

### Phase 1 Pre-Conditions
- [ ] Review existing learnings in `integration-testing/learnings/` directory
- [ ] Understand current integration testing setup
- [ ] Verify `rit-direct.sh` exists and functions

### Phase 1 Implementation
- [x] Check current file exists: `integration-testing/scripts/rit-direct.sh`
- [x] Execute rename: `mv integration-testing/scripts/rit-direct.sh integration-testing/scripts/run-integration-tests.sh`
- [x] Update references in `CLAUDE.md`
- [x] Update references in `integration-testing/README.md`
- [x] Update references in `integration-testing/TODO.txt` (no references found)
- [x] Search for other references: `grep -r "rit-direct.sh" . --exclude-dir=.git`
- [x] Update any additional references found

### Phase 1 Validation
- [x] Test locally: `./integration-testing/scripts/run-integration-tests.sh helloworld`
- [x] Verify exit code is 0
- [x] Test locally: `./integration-testing/scripts/run-integration-tests.sh kotlin-hello-world`
- [x] Verify exit code is 0
- [x] Check logs created in `integration-testing/logs/`

### Phase 1 Completion & Commit Point
- [ ] Commit: `git commit -m "refactor: rename integration test runner to run-integration-tests.sh"`
- [ ] Create learnings document: `integration-testing/learnings/phase-1-script-renaming.md`
- [ ] Document any issues encountered
- [ ] Review Phase 2 below and adjust if needed based on learnings

---

## Phase 2: Version Management System (HIGH PRIORITY)

*Moved up from Phase 3 - this is the highest risk component*

### Phase 2 Pre-Conditions
- [ ] Read `integration-testing/learnings/phase-1-script-renaming.md`
- [ ] Reflect on Phase 1 learnings and assess if Phase 2 plan needs adjustment
- [ ] Review if any blockers or concerns were identified
- [ ] Confirm renamed script is working locally
- [ ] Understand current Spring AI version structure

### Phase 2 Implementation
- [x] Analyze current version distribution: `grep -r "spring-ai.version" . --include="pom.xml" | wc -l`
- [x] Document which modules use version property vs. which don't (17 use property, 17 don't)
- [x] Create script: `scripts/update-spring-ai-version.sh`
- [x] Add shebang and parameter handling:
  ```bash
  #!/bin/bash
  VERSION="${1:-1.1.0-SNAPSHOT}"
  echo "Updating Spring AI version to: $VERSION"
  ```
- [x] Add find command to locate all pom.xml files
- [x] Add sed command to update `<spring-ai.version>` property
- [x] Add backup functionality before modifications
- [x] Add validation for version format (handle both release and SNAPSHOT)
- [x] Add restore capability from backups (separate script)
- [x] Make script executable: `chmod +x scripts/*.sh`

### Phase 2 Local Testing
- [x] Test with release version: `./scripts/update-spring-ai-version.sh 1.0.1`
- [x] Verify: `grep "spring-ai.version" misc/openai-streaming-response/pom.xml`
- [x] Test with SNAPSHOT: `./scripts/update-spring-ai-version.sh 1.1.0-SNAPSHOT`
- [x] Verify SNAPSHOT format preserved
- [x] Test restore functionality
- [x] Verify original versions restored
- [x] Run integration test to ensure changed version works: `./integration-testing/scripts/run-integration-tests.sh kotlin-hello-world`

### Phase 2 Version Compatibility Testing
- [x] Test examples with 1.0.1: `./integration-testing/scripts/run-integration-tests.sh kotlin-hello-world`
- [x] Test examples with 1.1.0-SNAPSHOT: `./integration-testing/scripts/run-integration-tests.sh kotlin-hello-world`
- [x] Document any version-specific issues (none found)
- [x] Create compatibility matrix for tested versions

### Phase 2 Completion & Commit Point
- [x] Create learnings document: `integration-testing/learnings/phase-2-version-management.md`
- [x] Document version compatibility findings
- [x] Note any version-specific behavior or limitations (none found)
- [x] List tested version combinations
- [x] Commit version management system: `git commit -m "feat: add Spring AI version management system"`
- [x] Review Phase 3 and adjust based on learnings

---

## Phase 3: Minimal GitHub Actions Workflow

*Basic workflow to validate version management works in CI*

### Phase 3 Pre-Conditions
- [x] Read ALL previous learnings documents (phases 1-2)
- [x] Reflect on learnings and assess if Phase 3 plan needs significant adjustment
- [x] Identify any blockers or technical debt from previous phases (none found)
- [x] Confirm version management script works locally
- [ ] Ensure GitHub repository write access
- [ ] Have OPENAI_API_KEY available for secrets

### Phase 3 Implementation
- [x] Create directory: `.github/workflows/`
- [x] Create file: `.github/workflows/integration-tests-manual.yml`
- [ ] Add workflow based on `spring-ai-github-build-workflow.yml` patterns:
  ```yaml
  name: Integration Tests (Manual)
  on:
    workflow_dispatch:
      inputs:
        spring_ai_version:
          description: 'Spring AI Version'
          required: false
          default: '1.1.0-SNAPSHOT'
  
  jobs:
    test:
      runs-on: ubuntu-latest
      timeout-minutes: 30
  ```
- [x] Add JDK 17 setup step (following reference workflow pattern)
- [x] Add Maven cache configuration
- [x] Add version update step using Phase 2 script
- [x] Add test execution for exactly 3 examples:
  - `models/chat/helloworld`
  - `kotlin/kotlin-hello-world`
  - `misc/spring-ai-java-function-callback`
- [x] Configure environment variables (OPENAI_API_KEY, SPRING_AI_OPENAI_API_KEY)
- [x] Add artifact upload for logs

### Phase 3 GitHub Secrets Configuration
- [ ] Navigate to repository Settings > Secrets and variables > Actions
- [ ] Add repository secret: OPENAI_API_KEY
- [ ] Document secret configuration process

### Phase 3 CI Validation
- [ ] Commit workflow: `git commit -m "feat: add GitHub Actions workflow with version management"`
- [ ] Push to GitHub: `git push`
- [ ] Navigate to Actions tab and select workflow
- [ ] Trigger workflow with default version (1.1.0-SNAPSHOT)
- [ ] Monitor execution and verify 3 examples pass
- [ ] Trigger workflow with version 1.0.1
- [ ] Download artifacts and verify version was applied correctly
- [ ] Trigger workflow with version 1.1.0-SNAPSHOT
- [ ] Verify both SNAPSHOT and release versions work

### Phase 3 Troubleshooting (if needed)
- [ ] If failures occur, analyze logs
- [ ] Fix GitHub Actions specific issues
- [ ] Re-run workflow to verify fixes

### Phase 3 Completion & Commit Point
- [ ] Commit workflow updates: `git commit -m "feat: add manual GitHub Actions workflow for integration tests"`
- [ ] Create learnings document: `integration-testing/learnings/phase-3-minimal-workflow.md`
- [ ] Document GitHub Actions setup process
- [ ] Note any CI-specific issues or limitations
- [ ] Document version management integration success
- [ ] Review Phase 4 and adjust based on learnings

---

## Phase 3b: Workflow Optimization & AI Validation Support

*Critical optimization to support AI validation and improve CI performance*

### Phase 3b Pre-Conditions
- [ ] Read learnings from Phase 3 (especially regarding AI validation failures)
- [ ] Understand Python requirements for AI validation
- [ ] Review JBang installation best practices
- [ ] Confirm basic workflow is functional

### Phase 3b Implementation - Setup Optimization
- [ ] Replace manual JBang installation with official action:
  ```yaml
  - name: Set up JBang
    uses: jbangdev/setup-jbang@v0.119.0
  ```
- [ ] Add Python setup for AI validation:
  ```yaml
  - name: Set up Python
    uses: actions/setup-python@v5
    with:
      python-version: '3.11'
      cache: 'pip'
  ```
- [ ] Install Python dependencies for AI validator:
  ```yaml
  - name: Install AI validator dependencies
    run: |
      pip install -r integration-testing/ai-validator/requirements.txt
  ```
- [ ] Add JBang cache to speed up subsequent runs:
  ```yaml
  - name: Cache JBang
    uses: actions/cache@v4
    with:
      path: ~/.jbang
      key: ${{ runner.os }}-jbang-${{ hashFiles('**/pom.xml') }}
      restore-keys: |
        ${{ runner.os }}-jbang-
  ```

### Phase 3b Implementation - Re-enable AI Validation
- [ ] Re-enable AI validation in `models/chat/helloworld/integration-tests/ExampleInfo.json`
- [ ] Re-enable AI validation in `kotlin/kotlin-hello-world/integration-tests/ExampleInfo.json`
- [ ] Test AI validation locally with Python environment
- [ ] Verify AI validation prompts are appropriate

### Phase 3b Testing
- [ ] Test workflow with optimized setup
- [ ] Verify JBang action works correctly
- [ ] Confirm Python environment is properly configured
- [ ] Test AI validation with all 3 examples
- [ ] Measure performance improvement from caching

### Phase 3b Troubleshooting
- [ ] If AI validation still fails, check Python path and dependencies
- [ ] If JBang action fails, check version compatibility
- [ ] Document any API key requirements for AI validation
- [ ] Check if AI validator needs ANTHROPIC_API_KEY or uses OPENAI_API_KEY

### Phase 3b Completion & Commit Point
- [ ] Commit optimizations: `git commit -m "feat: optimize workflow with official actions and AI validation support"`
- [ ] Create learnings document: `integration-testing/learnings/phase-3b-workflow-optimization.md`
- [ ] Document performance improvements
- [ ] Note any issues with AI validation
- [ ] Update documentation on Python requirements

---

## Phase 4: Multi-Version Matrix Testing (HIGH PRIORITY)

*Test the version management system with matrix execution - higher risk than full coverage*

### Phase 4 Pre-Conditions
- [ ] Read ALL previous learnings documents (phases 1-3)
- [ ] Reflect on cumulative learnings and assess if Phase 4 plan needs significant adjustment
- [ ] Review execution times and resource usage from Phase 3
- [ ] Confirm single-version workflow passes 3 examples reliably
- [ ] Understand GitHub Actions matrix strategies

### Phase 4 Implementation
- [ ] Create new workflow file: `.github/workflows/test-spring-ai-versions.yml`
- [ ] Add matrix strategy for Spring AI versions:
  ```yaml
  name: Test Spring AI Versions
  on:
    workflow_dispatch:
  
  jobs:
    test:
      runs-on: ubuntu-latest
      timeout-minutes: 30
      strategy:
        fail-fast: false
        matrix:
          spring-ai-version: ['1.0.1', '1.1.0-SNAPSHOT']
  ```
- [ ] Add job name with version for clarity: `name: Test with Spring AI ${{ matrix.spring-ai-version }}`
- [ ] Copy steps from Phase 3 workflow (JDK, cache, version update, test execution)
- [ ] Use matrix version: `./scripts/update-spring-ai-version.sh ${{ matrix.spring-ai-version }}`
- [ ] Add version reporting to output: `echo "Testing with Spring AI version: ${{ matrix.spring-ai-version }}"`
- [ ] Update artifact names to include version: `name: test-logs-${{ matrix.spring-ai-version }}`

### Phase 4 Validation
- [ ] Commit matrix workflow: `git commit -m "feat: add multi-version matrix testing"`
- [ ] Push to GitHub: `git push`
- [ ] Trigger matrix workflow execution
- [ ] Monitor parallel execution of both versions
- [ ] Verify version isolation (each job uses correct version)
- [ ] Check both versions pass the 3 examples
- [ ] Download and compare artifacts from both versions
- [ ] Document any version-specific differences

### Phase 4 Troubleshooting
- [ ] If version-specific failures occur, identify root cause
- [ ] Document which examples fail with which versions
- [ ] Update compatibility matrix
- [ ] Fix issues or mark as version-incompatible
- [ ] Re-run matrix to verify fixes

### Phase 4 Completion & Commit Point
- [ ] Commit matrix testing: `git commit -m "feat: add multi-version matrix testing workflow"`
- [ ] Create learnings document: `integration-testing/learnings/phase-4-matrix-testing.md`
- [ ] Create version compatibility matrix
- [ ] Document parallel execution insights
- [ ] Note resource usage for matrix builds
- [ ] Review Phase 5 and adjust based on learnings

---

## Phase 5: Automated Triggers & Path Filtering

### Phase 5 Pre-Conditions
- [ ] Read ALL previous learnings documents (phases 1-4)
- [ ] Reflect on learnings and assess if Phase 5 plan needs significant adjustment
- [ ] Review any version-specific issues discovered in Phase 4
- [ ] Confirm matrix testing runs reliably
- [ ] Understand GitHub Actions trigger types
- [ ] Plan path filtering strategy

### Phase 5 Implementation
- [ ] Open `.github/workflows/integration-tests-manual.yml`
- [ ] Add push event trigger:
  ```yaml
  push:
    branches: [main]
    paths-ignore:
      - 'integration-testing/**'
      - '**/*.md'
      - '.github/**'
      - '.gitignore'
      - 'CLAUDE.md'
  ```
- [ ] Add pull_request event trigger with same path filters
- [ ] Keep workflow_dispatch for manual runs
- [ ] Save workflow file

### Phase 5 Validation - Test PR 1 (Should Trigger)
- [ ] Create new branch: `git checkout -b test-trigger-1`
- [ ] Modify an example file (e.g., add comment to `models/chat/helloworld/src/main/java/`)
- [ ] Commit change: `git commit -m "test: verify workflow triggers on example change"`
- [ ] Push branch: `git push -u origin test-trigger-1`
- [ ] Create pull request
- [ ] Verify workflow triggers automatically
- [ ] Close PR after verification

### Phase 5 Validation - Test PR 2 (Should NOT Trigger)
- [ ] Create new branch: `git checkout -b test-trigger-2`
- [ ] Modify only README.md
- [ ] Commit change: `git commit -m "docs: update README"`
- [ ] Push branch and create PR
- [ ] Verify workflow does NOT trigger
- [ ] Close PR after verification

### Phase 5 Validation - Test PR 3 (Should NOT Trigger)
- [ ] Create new branch: `git checkout -b test-trigger-3`
- [ ] Modify integration-testing files only
- [ ] Commit and push
- [ ] Create PR
- [ ] Verify workflow does NOT trigger
- [ ] Close PR after verification

### Phase 5 Completion & Commit Point
- [ ] Commit trigger configuration: `git commit -m "feat: add automated triggers with path filtering"`
- [ ] Create learnings document: `integration-testing/learnings/phase-5-automated-triggers.md`
- [ ] Document trigger behavior
- [ ] Note any edge cases
- [ ] Document path filtering effectiveness
- [ ] Review Phase 6 and adjust based on learnings

---

## Phase 6: Full Coverage Rollout (32 Examples)

### Phase 6 Pre-Conditions
- [ ] Read ALL previous learnings documents (phases 1-5)
- [ ] Reflect on cumulative learnings and assess if Phase 6 plan needs significant adjustment
- [ ] Review any performance bottlenecks identified in previous phases
- [ ] Review total execution time for examples tested so far
- [ ] Calculate estimated time for 32 examples
- [ ] Ensure sufficient GitHub Actions minutes available

### Phase 6 Batch 1: Simple Examples (10 additional)
- [ ] Identify 10 simple examples without special requirements
- [ ] Add to workflow test execution
- [ ] Commit and push: `git commit -m "feat: add batch 1 simple examples"`
- [ ] Trigger workflow
- [ ] Monitor execution
- [ ] Fix any issues found
- [ ] Document results

### Phase 6 Batch 2: MCP Examples (15 additional)
- [ ] Configure BRAVE_API_KEY in GitHub secrets (if needed)
- [ ] Add 15 MCP examples to workflow
- [ ] Update timeout to 60 minutes if needed
- [ ] Commit and push: `git commit -m "feat: add batch 2 MCP examples"`
- [ ] Trigger workflow
- [ ] Monitor execution
- [ ] Fix any MCP-specific issues
- [ ] Document MCP requirements

### Phase 6 Batch 3: Remaining Complex Examples
- [ ] Add all remaining examples
- [ ] Optimize execution order for efficiency
- [ ] Commit and push: `git commit -m "feat: complete full coverage with all examples"`
- [ ] Trigger complete suite
- [ ] Monitor full execution
- [ ] Calculate final pass rate
- [ ] Verify 97% pass rate achieved

### Phase 6 Performance Optimization
- [ ] If execution exceeds 60 minutes, consider:
  - [ ] Splitting into multiple jobs
  - [ ] Parallel execution strategies
  - [ ] Caching optimizations
- [ ] Document optimization decisions

### Phase 6 Completion & Commit Point
- [ ] Final commit: `git commit -m "feat: complete full integration test coverage (32 examples)"`
- [ ] Create learnings document: `integration-testing/learnings/phase-6-full-coverage.md`
- [ ] Document final metrics (pass rate, execution time)
- [ ] List any persistently failing tests
- [ ] Document resource usage
- [ ] Review Phase 7 and adjust based on learnings

---

## Phase 7: Multi-Version Testing Matrix

### Phase 7 Pre-Conditions
- [ ] Read ALL previous learnings documents (phases 1-6)
- [ ] Reflect on learnings and assess if Phase 7 plan needs significant adjustment
- [ ] Review performance metrics from Phase 6 full coverage
- [ ] Confirm full suite runs successfully
- [ ] Define target Spring AI versions to test
- [ ] Understand GitHub Actions matrix strategies

### Phase 7 Implementation
- [ ] Create new workflow file: `.github/workflows/test-spring-ai-versions.yml`
- [ ] Add matrix strategy:
  ```yaml
  strategy:
    fail-fast: false
    matrix:
      spring-ai-version: ['1.0.0', '1.0.1', '1.1.0-SNAPSHOT']
  ```
- [ ] Configure job to use matrix version
- [ ] Add version to job name for clarity
- [ ] Add version reporting in test output

### Phase 7 Small-Scale Testing
- [ ] Limit initial matrix test to 3 examples only
- [ ] Push workflow: `git commit -m "feat: add multi-version testing matrix"`
- [ ] Trigger workflow
- [ ] Verify parallel execution works
- [ ] Check version isolation
- [ ] Review logs for each version

### Phase 7 Full-Scale Testing
- [ ] Expand to all 32 examples
- [ ] Trigger full matrix workflow
- [ ] Monitor parallel execution (3 versions × 32 examples)
- [ ] Document version-specific failures
- [ ] Create compatibility matrix

### Phase 7 Completion & Commit Point
- [ ] Commit multi-version testing: `git commit -m "feat: add multi-version testing matrix for all examples"`
- [ ] Create learnings document: `integration-testing/learnings/phase-7-multi-version.md`
- [ ] Create version compatibility matrix document
- [ ] Document version-specific issues
- [ ] Note execution time for matrix builds
- [ ] Review Phase 8 and adjust based on learnings

---

## Phase 8: Enhanced CI Features

### Phase 8 Pre-Conditions
- [ ] Read ALL previous learnings documents (phases 1-7)
- [ ] Reflect on learnings and assess if Phase 8 plan needs significant adjustment
- [ ] Review resource usage and cost implications from Phase 7
- [ ] Prioritize enhancement features based on actual needs discovered
- [ ] Confirm core functionality is stable
- [ ] Review GitHub Actions advanced features

### Phase 8 JSON Output Implementation
- [ ] Modify `run-integration-tests.sh` to add `--json` flag
- [ ] Implement JSON output format for test results
- [ ] Test JSON output locally
- [ ] Integrate JSON parsing in workflow

### Phase 8 GitHub Annotations
- [ ] Add problem matchers for test failures
- [ ] Implement workflow annotations
- [ ] Test annotation display in PR

### Phase 8 PR Comments
- [ ] Add action to comment test results on PRs
- [ ] Format results as markdown table
- [ ] Test with sample PR

### Phase 8 Caching
- [ ] Add Maven dependency caching
- [ ] Add JBang caching if applicable
- [ ] Measure performance improvement
- [ ] Document cache hit rates

### Phase 8 Scheduled Runs
- [ ] Add nightly schedule trigger:
  ```yaml
  schedule:
    - cron: '0 2 * * *'  # 2 AM UTC daily
  ```
- [ ] Configure to run against multiple versions
- [ ] Set up failure notifications

### Phase 8 Completion & Commit Point
- [ ] Commit enhancements: `git commit -m "feat: add CI enhancements (JSON output, annotations, caching)"`
- [ ] Create learnings document: `integration-testing/learnings/phase-8-enhancements.md`
- [ ] Document feature effectiveness
- [ ] Note performance improvements
- [ ] List any features deferred
- [ ] Review Phase 9 and adjust based on learnings

---

## Phase 9: Documentation & Finalization

### Phase 9 Pre-Conditions
- [ ] Read ALL learnings documents (phases 1-8)
- [ ] Reflect on entire journey and identify key success factors
- [ ] Document any technical debt or future improvements needed
- [ ] Compile list of key insights
- [ ] Review original goals vs. achievements
- [ ] Gather all configuration examples

### Phase 9 Documentation Tasks
- [ ] Update this plan with final architecture
- [ ] Create troubleshooting guide: `integration-testing/docs/GITHUB_ACTIONS_TROUBLESHOOTING.md`
- [ ] Create maintenance runbook: `integration-testing/docs/CI_MAINTENANCE.md`
- [ ] Update main README.md with CI/CD section
- [ ] Document all workflow files
- [ ] Create secrets management guide
- [ ] Document rollback procedures

### Phase 9 Validation
- [ ] Run full manual workflow
- [ ] Create test PR to verify automatic triggers
- [ ] Run multi-version matrix
- [ ] Verify scheduled run (if implemented)
- [ ] Test all documented procedures

### Phase 9 Completion & Commit Point
- [ ] Final documentation commit: `git commit -m "docs: complete GitHub Actions CI/CD documentation"`
- [ ] Create final summary: `integration-testing/learnings/github-actions-implementation-summary.md`
- [ ] Archive this plan with completion dates
- [ ] Document future enhancement opportunities
- [ ] Calculate total implementation time
- [ ] Mark project complete

---

## Key Principles

1. **Progressive Enhancement**: Start with 3 examples, expand gradually
2. **Early Validation**: Test in GitHub Actions as soon as Phase 2
3. **Continuous Learning**: Each phase reads all previous learnings
4. **Adaptive Planning**: Adjust later phases based on discoveries
5. **State in Documents**: Track progress via checkboxes and learnings

## Success Metrics

- **Phase 2**: First GitHub Actions run with 3 examples ✓
- **Phase 4**: 8 examples running reliably ✓
- **Phase 5**: Automatic triggers working ✓
- **Phase 6**: Full 32 examples with 97% pass rate ✓
- **Phase 7**: Multi-version testing operational ✓
- **Phase 8**: Enhanced features implemented ✓
- **Phase 9**: Complete documentation ✓

## Risk Mitigation

1. **API Rate Limits**: Implement caching, limit concurrent runs
2. **Secret Leakage**: Use GitHub secrets, never commit keys
3. **Long Execution Times**: Optimize order, consider splitting jobs
4. **Flaky Tests**: Add retry logic, document known issues
5. **Version Conflicts**: Maintain compatibility matrix

## Maintenance Notes

- Review workflow runs weekly
- Update Spring AI versions monthly
- Archive old logs quarterly
- Review and update documentation semi-annually
- Monitor GitHub Actions usage and costs

## Future Enhancements (Not in Current Scope)

- Parallel execution with dynamic port assignment
- Test result trending dashboard
- Automatic issue creation for failures
- Integration with Spring AI release process
- Performance regression detection