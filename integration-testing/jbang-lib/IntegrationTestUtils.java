/*
 * Centralized utilities for JBang integration tests
 * Provides common functionality to eliminate code duplication across test scripts
 */

import com.fasterxml.jackson.databind.*;
import org.zeroturnaround.exec.*;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import static java.lang.System.*;

public class IntegrationTestUtils {
    
    // Record for test configuration
    public record ExampleInfo(
        int timeoutSec, 
        String[] successRegex, 
        String[] requiredEnv,
        String[] setupCommands,
        String[] cleanupCommands
    ) {}
    
    // Load configuration from ExampleInfo.json
    public static ExampleInfo loadConfig() throws Exception {
        Path configPath = Path.of("integration-tests/ExampleInfo.json");
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper.readValue(configPath.toFile(), ExampleInfo.class);
    }
    
    // Verify required environment variables
    public static void verifyEnvironment(ExampleInfo cfg) {
        for (String envVar : cfg.requiredEnv()) {
            if (getenv(envVar) == null) {
                err.println("❌ Missing required environment variable: " + envVar);
                exit(1);
            }
        }
    }
    
    // Run a command with timeout
    public static void runCommand(String[] cmd, int timeoutSec) throws Exception {
        ProcessResult result = new ProcessExecutor()
            .command(cmd)
            .timeout(timeoutSec, TimeUnit.SECONDS)
            .redirectOutput(System.out)
            .redirectError(System.err)
            .execute();
            
        int exit = result.getExitValue();
        if (exit != 0) {
            throw new RuntimeException("Command failed with exit code " + exit + ": " + String.join(" ", cmd));
        }
    }
    
    // Run setup commands if specified
    public static void runSetupCommands(ExampleInfo cfg) throws Exception {
        if (cfg.setupCommands() != null) {
            for (String setupCmd : cfg.setupCommands()) {
                out.println("🔧 Running setup: " + setupCmd);
                runCommand(setupCmd.split("\\s+"), 60); // 1 minute timeout for setup
            }
        }
    }
    
    // Run cleanup commands if specified
    public static void runCleanupCommands(ExampleInfo cfg) {
        if (cfg.cleanupCommands() != null) {
            for (String cleanupCmd : cfg.cleanupCommands()) {
                out.println("🧹 Running cleanup: " + cleanupCmd);
                try {
                    runCommand(cleanupCmd.split("\\s+"), 30);
                } catch (Exception e) {
                    err.println("⚠️  Cleanup command failed (non-fatal): " + e.getMessage());
                }
            }
        }
    }
    
    // Build the application
    public static void buildApplication(String moduleName) throws Exception {
        out.println("🏗️  Building " + moduleName + "...");
        runCommand(new String[]{"./mvnw", "clean", "package", "-q", "-DskipTests"}, 300);
    }
    
    // Create log file path
    public static Path createLogFile(String moduleName) throws Exception {
        // Try different relative paths based on module depth
        Path logDir = null;
        if (Files.exists(Paths.get("../../../integration-testing"))) {
            logDir = Paths.get("../../../integration-testing/logs/integration-tests");
        } else if (Files.exists(Paths.get("../../integration-testing"))) {
            logDir = Paths.get("../../integration-testing/logs/integration-tests");
        } else if (Files.exists(Paths.get("../../../../integration-testing"))) {
            logDir = Paths.get("../../../../integration-testing/logs/integration-tests");
        } else {
            throw new RuntimeException("Could not find integration-testing directory");
        }
        
        Files.createDirectories(logDir);
        return logDir.resolve(moduleName + "-spring-boot-" + System.currentTimeMillis() + ".log");
    }
    
    // Run the Spring Boot application
    public static ProcessResult runSpringBootApp(ExampleInfo cfg, Path logFile) throws Exception {
        return new ProcessExecutor()
            .command("./mvnw", "spring-boot:run", "-q")
            .timeout(cfg.timeoutSec(), TimeUnit.SECONDS)
            .redirectOutput(Files.newOutputStream(logFile))
            .redirectErrorStream(true)
            .execute();
    }
    
    // Display full output
    public static void displayOutput(String output) {
        out.println("📋 Full Application Output:");
        out.println("---");
        out.println(output);
        out.println("---");
    }
    
    // Display log file path
    public static void displayLogPath(Path logFile) {
        out.println("📁 Full Spring Boot log: " + logFile.toAbsolutePath().normalize());
    }
    
    // Check results and exit appropriately
    public static void checkResults(int exitCode, int failedPatterns) {
        if (exitCode != 0) {
            err.println("❌ Application exited with code: " + exitCode);
            exit(exitCode);
        }
        
        if (failedPatterns > 0) {
            err.println("❌ Failed pattern verification: " + failedPatterns + " patterns missing");
            exit(1);
        }
        
        out.println("🎉 Integration test completed successfully!");
    }
    
    // Main test execution flow
    public static void runIntegrationTest(String moduleName) throws Exception {
        // Load configuration
        ExampleInfo cfg = loadConfig();
        
        // Verify environment
        verifyEnvironment(cfg);
        
        try {
            // Build application
            buildApplication(moduleName);
            
            // Run setup commands AFTER build to avoid clean removing test files
            runSetupCommands(cfg);
            
            // Create log file
            Path logFile = createLogFile(moduleName.toLowerCase().replace(" ", "-"));
            
            // Run application
            out.println("🚀 Running " + moduleName + "...");
            ProcessResult result = runSpringBootApp(cfg, logFile);
            int exitCode = result.getExitValue();
            
            // Read output
            String output = Files.readString(logFile);
            
            // Verify patterns and display log path
            out.println("✅ Verifying output patterns...");
            out.println("📁 Full Spring Boot log: " + logFile.toAbsolutePath().normalize());
            
            // Display full output first
            displayOutput(output);
            
            // Then verify patterns
            int failedPatterns = 0;
            for (String pattern : cfg.successRegex()) {
                if (output.matches("(?s).*" + pattern + ".*")) {
                    out.println("  ✓ Found: " + pattern);
                } else {
                    err.println("  ❌ Missing: " + pattern);
                    failedPatterns++;
                }
            }
            
            // Keep log file for debugging - DO NOT DELETE
            out.println("\n📁 Spring Boot log preserved: " + logFile.toAbsolutePath().normalize());
            
            // Check results
            checkResults(exitCode, failedPatterns);
            
        } finally {
            // Run cleanup commands
            runCleanupCommands(cfg);
        }
    }
}