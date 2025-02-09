# Spring AI MCP Weather STDIO Server

A Spring Boot starter project demonstrating how to build a Model Context Protocol (MCP) server that provides weather-related tools using the National Weather Service (weather.gov) API. This project showcases the Spring AI MCP Server Boot Starter capabilities with STDIO transport implementation.

## Prerequisites

- Java 17 or later
- Maven 3.6+
- Basic understanding of Spring Boot and Spring AI
- (Optional) Claude Desktop for AI assistant integration

## About Spring AI MCP Server Boot Starter

This project uses `spring-ai-mcp-server-spring-boot-starter`, which provides:
- Automatic configuration of MCP server components
- Support for both synchronous and asynchronous operation modes
- STDIO transport layer implementation
- Flexible tool registration through Spring beans
- Change notification capabilities

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── org/springframework/ai/mcp/sample/server/
│   │       ├── McpServerApplication.java    # Main application class with tool registration
│   │       └── WeatherService.java          # Weather service implementation with MCP tools
│   └── resources/
│       └── application.properties           # Server and transport configuration
└── test/
    └── java/
        └── org/springframework/ai/mcp/sample/client/
            └── ClientStdio.java             # Test client implementation
```

## Building and Running

The server uses STDIO transport mode and is typically started automatically by the client. You only need to build the server jar:

```bash
./mvnw clean install -DskipTests
```

## Tool Implementation

The project demonstrates how to implement and register MCP tools using Spring's dependency injection and auto-configuration:

```java
@Service
public class WeatherService {

    @Tool(description = "Get weather forecast for a specific latitude/longitude")
    public String getWeatherForecastByLocation(
        double latitude,   // Latitude coordinate
        double longitude   // Longitude coordinate
    ) {
        // Implementation
    }

    @Tool(description = "Get weather alerts for a US state")
    public String getAlerts(
        String state  // Two-letter US state code (e.g. CA, NY)
    ) {
        // Implementation
    }
}

@SpringBootApplication
public class McpServerApplication {
    @Bean
    public List<ToolCallback> weatherTools(WeatherService weatherService) {
        return ToolCallbacks.from(weatherService);
    }
}
```

The auto-configuration will automatically register these tools with the MCP server. You can have multiple beans producing lists of ToolCallbacks, and the auto-configuration will merge them.

## Available Tools

### 1. Weather Forecast Tool
```java
@Tool(description = "Get weather forecast for a specific latitude/longitude")
public String getWeatherForecastByLocation(
    double latitude,   // Latitude coordinate
    double longitude   // Longitude coordinate
) {
    // Returns detailed forecast including:
    // - Temperature and unit
    // - Wind speed and direction
    // - Detailed forecast description
}

// Example usage:
CallToolResult forecast = client.callTool(
    new CallToolRequest("getWeatherForecastByLocation",
        Map.of(
            "latitude", 47.6062,    // Seattle coordinates
            "longitude", -122.3321
        )
    )
);
```

### 2. Weather Alerts Tool
```java
@Tool(description = "Get weather alerts for a US state")
public String getAlerts(
    String state  // Two-letter US state code (e.g. CA, NY)
) {
    // Returns active alerts including:
    // - Event type
    // - Affected area
    // - Severity
    // - Description
    // - Safety instructions
}

// Example usage:
CallToolResult alerts = client.callTool(
    new CallToolRequest("getAlerts",
        Map.of("state", "NY")
    )
);
```

## Client Integration

### Java Client Example

```java
// Create server parameters
ServerParameters stdioParams = ServerParameters.builder("java")
    .args("-Dspring.ai.mcp.server.transport=STDIO",
          "-Dspring.main.web-application-type=none",
          "-Dlogging.pattern.console=",
          "-jar",
          "target/mcp-weather-stdio-server-0.0.1-SNAPSHOT.jar")
    .build();

// Initialize transport and client
var transport = new StdioClientTransport(stdioParams);
var client = McpClient.sync(transport).build();
```

### Claude Desktop Integration

To integrate with Claude Desktop, add the following configuration to your Claude Desktop settings:

```json
{
  "mcpServers": {
    "spring-ai-mcp-weather": {
      "command": "java",
      "args": [
        "-Dspring.ai.mcp.server.transport=STDIO",
        "-Dspring.main.web-application-type=none",
        "-Dlogging.pattern.console=",
        "-jar",
        "/absolute/path/to/mcp-weather-stdio-server-0.0.1-SNAPSHOT.jar"
      ]
    }
  }
}
```

Replace `/absolute/path/to/` with the actual path to your built jar file.

## Configuration

### Application Properties

All properties are prefixed with `spring.ai.mcp.server`


```properties
# Required STDIO Configuration
spring.main.web-application-type=none
spring.main.banner-mode=off
logging.pattern.console=

# Server Configuration
spring.ai.mcp.server.enabled=true
spring.ai.mcp.server.name=my-weather-server
spring.ai.mcp.server.version=0.0.1
# SYNC or ASYNC
spring.ai.mcp.server.type=SYNC
spring.ai.mcp.server.resource-change-notification=true
spring.ai.mcp.server.tool-change-notification=true
spring.ai.mcp.server.prompt-change-notification=true

# Optional file logging
logging.file.name=mcp-weather-stdio-server.log
```

### Key Configuration Notes

1. **STDIO Mode Requirements**:
   - Disable web application type (`spring.main.web-application-type=none`)
   - Disable Spring banner (`spring.main.banner-mode=off`)
   - Clear console logging pattern (`logging.pattern.console=`)

2. **Server Type**:
   - `SYNC` (default): Uses `McpSyncServer` for straightforward request-response patterns
   - `ASYNC`: Uses `McpAsyncServer` for non-blocking operations with Project Reactor support

## Additional Resources

- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [Model Context Protocol Specification](https://modelcontextprotocol.github.io/specification/)
- [Spring Boot Auto-configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.developing-auto-configuration)

