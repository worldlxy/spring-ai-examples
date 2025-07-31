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
import argparse
from typing import List, Tuple, Dict, Optional

# Configuration
JBANG = shutil.which("jbang")
MAX_WORKERS = 1  # Sequential execution to avoid Spring Boot port conflicts

def log(message: str, level: str = "INFO", verbose: bool = False):
    """Enhanced logging with timestamps and levels"""
    if level == "DEBUG" and not verbose:
        return
    timestamp = time.strftime("%H:%M:%S")
    colors = {
        "INFO": "\033[0m",     # Default
        "DEBUG": "\033[90m",   # Gray
        "WARN": "\033[33m",    # Yellow
        "ERROR": "\033[31m",   # Red
        "SUCCESS": "\033[32m"  # Green
    }
    reset = "\033[0m"
    color = colors.get(level, colors["INFO"])
    print(f"{color}[{timestamp}] {level}: {message}{reset}")

def find_integration_tests(filter_pattern: Optional[str] = None) -> List[pathlib.Path]:
    """Find all JBang integration test launchers with optional filtering"""
    launchers = list(pathlib.Path(".").rglob("integration-tests/Run*.java"))
    
    if filter_pattern:
        filtered_launchers = []
        for launcher in launchers:
            module_path = str(launcher.parent.parent)
            if filter_pattern.lower() in module_path.lower():
                filtered_launchers.append(launcher)
        launchers = filtered_launchers
    
    log(f"Found {len(launchers)} integration test launchers")
    if filter_pattern:
        log(f"Filtered by pattern: {filter_pattern}")
    
    return launchers

def validate_environment() -> bool:
    """Validate required tools and environment"""
    if not JBANG:
        log("JBang not found in PATH. Please install JBang.", "ERROR")
        log("Install with: curl -Ls https://sh.jbang.dev | bash -s - app setup", "INFO")
        return False
    
    # Check Java availability
    try:
        result = subprocess.run(["java", "-version"], 
                              capture_output=True, text=True, timeout=10)
        if result.returncode != 0:
            log("Java not found or not working properly", "ERROR")
            return False
    except (subprocess.TimeoutExpired, FileNotFoundError):
        log("Java not available", "ERROR")
        return False
    
    log("Environment validation passed", "SUCCESS")
    return True

def run_integration_test(launcher: pathlib.Path, verbose: bool = False, stream: bool = False) -> Tuple[pathlib.Path, bool, str, float]:
    """
    Run a single integration test
    Returns: (launcher_path, success, error_message, execution_time)
    """
    start_time = time.time()
    info_file = launcher.parent / "ExampleInfo.json"
    
    if not info_file.exists():
        return launcher, False, f"Missing {info_file}", time.time() - start_time
    
    try:
        with info_file.open() as f:
            config = json.load(f)
    except json.JSONDecodeError as e:
        return launcher, False, f"Invalid JSON in {info_file}: {e}", time.time() - start_time
    
    module_name = f"{launcher.parent.parent.name}/{launcher.parent.name}"
    log(f"Running {module_name}", "INFO", verbose)
    
    # Set up paths
    module_dir = launcher.parent.parent.resolve()
    
    try:
        # Set up output capture and execution with persistent log files
        launcher_absolute = launcher.resolve()
        launcher_relative = launcher_absolute.relative_to(module_dir)
        
        # Create logs directory and log file for this test
        logs_dir = pathlib.Path("logs")
        logs_dir.mkdir(exist_ok=True)
        timestamp = time.strftime("%Y%m%d_%H%M%S")
        log_file = logs_dir / f"{module_name.replace('/', '_')}_{timestamp}.log"
        
        timeout = config.get("timeoutSec", 300)
        
        if stream:
            # Stream mode: show live output while writing to log file
            log(f"🚀 Streaming {module_name} (timeout: {timeout}s) → {log_file}", "INFO")
            
            try:
                with open(log_file, 'w') as logf:
                    process = subprocess.Popen(
                        [JBANG, str(launcher_relative)],
                        cwd=module_dir,
                        stdout=subprocess.PIPE,
                        stderr=subprocess.STDOUT,
                        text=True,
                        bufsize=1,  # Line buffering
                        universal_newlines=True
                    )
                    
                    start_stream_time = time.time()
                    
                    # Read output line by line in real-time
                    for line in iter(process.stdout.readline, ''):
                        if not line:
                            break
                            
                        # Write to log file
                        logf.write(line)
                        logf.flush()
                        
                        elapsed = time.time() - start_stream_time
                        
                        # Check for timeout
                        if elapsed > timeout:
                            process.terminate()
                            return launcher, False, f"Timeout after {timeout}s", time.time() - start_time
                        
                        # Show progress indicator with cleaner output
                        progress = min(elapsed / timeout * 100, 99)
                        clean_line = line.strip()[:80]  # Limit line length
                        if clean_line:  # Only show meaningful lines
                            print(f"🔄 [{progress:5.1f}%] {clean_line}")
                        
                        # Check if process finished
                        if process.poll() is not None:
                            break
                    
                    # Get final return code
                    return_code = process.wait()
                    print()  # New line after progress display
                    
            except Exception as e:
                return launcher, False, f"Streaming error: {e}", time.time() - start_time
            
        else:
            # Non-stream mode: capture to log file
            log(f"Running {module_name} → {log_file}", "INFO", verbose)
            
            try:
                with open(log_file, 'w') as logf:
                    result = subprocess.run(
                        [JBANG, str(launcher_relative)],
                        cwd=module_dir,
                        stdout=logf,
                        stderr=subprocess.STDOUT,
                        text=True,
                        timeout=timeout
                    )
                    return_code = result.returncode
                    
            except subprocess.TimeoutExpired:
                return launcher, False, f"Timeout after {timeout}s", time.time() - start_time
        
        # Read the log file for pattern verification (unified for both modes)
        with open(log_file, 'r') as logf:
            output = logf.read()
            
        if verbose and not stream:
            log(f"Output from {launcher.name}:\n{output}", "DEBUG", verbose)
        
        # Check exit code and trust JBang's pattern verification
        if return_code != 0:
            return launcher, False, f"Exit code {return_code}", time.time() - start_time
        
        # JBang script does its own pattern verification and exits with proper code
        # If we reach here with return_code == 0, the JBang script verified all patterns
        # No need to duplicate pattern verification in Python
        
        execution_time = time.time() - start_time
        if stream:
            log(f"✅ {module_name} completed in {execution_time:.1f}s", "SUCCESS")
        else:
            log(f"✅ {module_name} completed in {execution_time:.1f}s", "SUCCESS", verbose)
        return launcher, True, "Success", execution_time
                
    except Exception as e:
        return launcher, False, f"Execution error: {e}", time.time() - start_time

def generate_report(results: List[Tuple], total_time: float, output_file: Optional[str] = None):
    """Generate detailed test report"""
    total_tests = len(results)
    passed_tests = sum(1 for _, success, _, _ in results if success)
    failed_tests = [(launcher, error) for launcher, success, error, _ in results if not success]
    
    # Calculate timing statistics
    execution_times = [exec_time for _, _, _, exec_time in results]
    avg_time = sum(execution_times) / len(execution_times) if execution_times else 0
    max_time = max(execution_times) if execution_times else 0
    
    report = f"""
# Integration Test Report

**Generated**: {time.strftime('%Y-%m-%d %H:%M:%S')}
**Total Duration**: {total_time:.1f}s
**Average Test Time**: {avg_time:.1f}s
**Longest Test**: {max_time:.1f}s

## Summary
- **Total Tests**: {total_tests}
- **Passed**: {passed_tests} ({passed_tests/total_tests*100:.1f}%)
- **Failed**: {len(failed_tests)} ({len(failed_tests)/total_tests*100:.1f}%)

## Results by Test
"""
    
    for launcher, success, message, exec_time in results:
        module_name = f"{launcher.parent.parent.name}/{launcher.parent.name}"
        status = "✅ PASS" if success else "❌ FAIL"
        report += f"- **{module_name}**: {status} ({exec_time:.1f}s)\n"
        if not success:
            report += f"  - Error: {message}\n"
    
    if failed_tests:
        report += "\n## Failed Tests Details\n"
        for launcher, error in failed_tests:
            module_name = f"{launcher.parent.parent.name}/{launcher.parent.name}"
            report += f"### {module_name}\n"
            report += f"**Error**: {error}\n\n"
    
    if output_file:
        with open(output_file, 'w') as f:
            f.write(report)
        log(f"Report saved to {output_file}", "INFO")
    
    return report

def main():
    """Main execution function with CLI argument parsing"""
    parser = argparse.ArgumentParser(description="Run Spring AI Examples Integration Tests")
    parser.add_argument("--verbose", "-v", action="store_true", 
                       help="Enable verbose output")
    parser.add_argument("--filter", "-f", type=str, 
                       help="Filter tests by module name pattern")
    parser.add_argument("--workers", "-w", type=int, default=MAX_WORKERS,
                       help="Number of parallel workers")
    parser.add_argument("--report", "-r", type=str,
                       help="Output file for test report")
    parser.add_argument("--fail-fast", action="store_true",
                       help="Stop on first failure")
    parser.add_argument("--stream", "-s", action="store_true",
                       help="Stream live output from tests (shows progress in real-time)")
    
    args = parser.parse_args()
    
    start_time = time.time()
    
    log("🚀 Starting Spring AI Examples Integration Tests", "INFO")
    
    # Validate environment
    if not validate_environment():
        sys.exit(1)
    
    # Find all integration tests
    launchers = find_integration_tests(args.filter)
    if not launchers:
        log("No integration tests found", "WARN")
        if args.filter:
            log(f"Try without filter or check pattern: {args.filter}", "INFO")
        return
    
    # Run tests in parallel
    results = []
    failed_tests = []
    
    log(f"Running {len(launchers)} tests with {args.workers} workers", "INFO")
    
    with concurrent.futures.ThreadPoolExecutor(max_workers=args.workers) as executor:
        # Submit all tests
        future_to_launcher = {
            executor.submit(run_integration_test, launcher, args.verbose, args.stream): launcher 
            for launcher in launchers
        }
        
        # Collect results
        for future in concurrent.futures.as_completed(future_to_launcher):
            launcher, success, message, exec_time = future.result()
            results.append((launcher, success, message, exec_time))
            
            module_name = f"{launcher.parent.parent.name}/{launcher.parent.name}"
            if success:
                log(f"✅ {module_name} ({exec_time:.1f}s)", "SUCCESS")
            else:
                log(f"❌ {module_name}: {message} ({exec_time:.1f}s)", "ERROR")
                failed_tests.append((launcher, message))
                
                if args.fail_fast:
                    log("Stopping due to --fail-fast", "ERROR")
                    # Cancel remaining futures
                    for pending_future in future_to_launcher:
                        pending_future.cancel()
                    break
    
    total_time = time.time() - start_time
    
    # Generate and display report
    report = generate_report(results, total_time, args.report)
    
    # Summary
    total_tests = len(results)
    passed_tests = sum(1 for _, success, _, _ in results if success)
    
    log(f"\n📊 Integration Test Results:", "INFO")
    log(f"  Total: {total_tests}", "INFO")
    log(f"  Passed: {passed_tests}", "SUCCESS")
    log(f"  Failed: {len(failed_tests)}", "ERROR" if failed_tests else "INFO")
    log(f"  Duration: {total_time:.1f}s", "INFO")
    
    if failed_tests:
        log("\n💥 Failed Tests:", "ERROR")
        for launcher, error in failed_tests:
            module_name = f"{launcher.parent.parent.name}/{launcher.parent.name}"
            log(f"  - {module_name}: {error}", "ERROR")
        sys.exit(1)
    else:
        log("🎉 All integration tests passed!", "SUCCESS")

if __name__ == "__main__":
    main()