# Spring AI MCP Weather Server Sample with WebFlux Starter

This sample project demonstrates how to create an MCP server using the Spring AI MCP Server Boot Starter with WebFlux transport. It implements a weather service that exposes tools for retrieving weather information using the National Weather Service API.

## Overview

The sample showcases:
- Integration with `spring-ai-mcp-server-webflux-spring-boot-starter`
- Support for both SSE (Server-Sent Events) and STDIO transports
- Automatic tool registration using Spring AI's `@Tool` annotation
- Two weather-related tools:
  - Get weather forecast by location (latitude/longitude)
  - Get weather alerts by US state

## Dependencies

The project uses the Spring AI MCP Server WebFlux Boot Starter:

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-mcp-server-webflux-spring-boot-starter</artifactId>
</dependency>
```

This starter provides:
- Reactive transport using Spring WebFlux (`WebFluxSseServerTransport`)
- Automatically configured reactive SSE endpoints
- Optional STDIO transport
- Included `spring-boot-starter-webflux` and `mcp-spring-webflux` dependencies

## Building the Project

```bash
./mvnw clean install -DskipTests
```

## Running the Server

The server supports two transport modes:

### WebFlux SSE Mode (Default)
```bash
java -jar target/mcp-weather-starter-webflux-server-0.0.1-SNAPSHOT.jar
```

### STDIO Mode
Enable STDIO transport by setting the appropriate properties:
```bash
java -Dspring.ai.mcp.server.stdio=true -Dspring.main.web-application-type=none -jar target/mcp-weather-starter-webflux-server-0.0.1-SNAPSHOT.jar
```

## Configuration

The server can be configured through `application.properties`:

```properties
# Server identification
spring.ai.mcp.server.name=my-weather-server
spring.ai.mcp.server.version=0.0.1

# Server type (SYNC/ASYNC)
spring.ai.mcp.server.type=SYNC

# Transport configuration
spring.ai.mcp.server.stdio=false
spring.ai.mcp.server.sse-message-endpoint=/mcp/message

# Change notifications
spring.ai.mcp.server.resource-change-notification=true
spring.ai.mcp.server.tool-change-notification=true
spring.ai.mcp.server.prompt-change-notification=true

# Logging (required for STDIO transport)
spring.main.banner-mode=off
logging.file.name=./target/starter-webflux-server.log
```

## Available Tools

### Weather Forecast Tool
- Name: `getWeatherForecastByLocation`
- Description: Get weather forecast for a specific latitude/longitude
- Parameters:
  - `latitude`: double - Latitude coordinate
  - `longitude`: double - Longitude coordinate
- Example:
```java
CallToolResult forecastResult = client.callTool(new CallToolRequest("getWeatherForecastByLocation",
        Map.of("latitude", 47.6062, "longitude", -122.3321)));
```

### Weather Alerts Tool
- Name: `getAlerts`
- Description: Get weather alerts for a US state
- Parameters:
  - `state`: String - Two-letter US state code (e.g. CA, NY)
- Example:
```java
CallToolResult alertResult = client.callTool(new CallToolRequest("getAlerts", 
        Map.of("state", "NY")));
```

## Server Implementation

The server uses Spring Boot and Spring AI's tool annotations for automatic tool registration:

```java
@SpringBootApplication
public class McpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }

    @Bean
    public List<ToolCallback> weatherTools(WeatherService weatherService) {
        return List.of(ToolCallbacks.from(weatherService));
    }
}
```

The `WeatherService` implements the weather tools using the `@Tool` annotation:

```java
@Service
public class WeatherService {
    @Tool(description = "Get weather forecast for a specific latitude/longitude")
    public String getWeatherForecastByLocation(double latitude, double longitude) {
        // Implementation using weather.gov API
    }

    @Tool(description = "Get weather alerts for a US state. Input is Two-letter US state code (e.g. CA, NY)")
    public String getAlerts(String state) {
        // Implementation using weather.gov API
    }
}
```

## Sample Clients

### WebFlux SSE Client
```java
var transport = new WebFluxSseClientTransport(WebClient.builder().baseUrl("http://localhost:8080"));
var client = McpClient.sync(transport).build();
```

### STDIO Client
```java
var stdioParams = ServerParameters.builder("java")
    .args("-Dspring.ai.mcp.server.stdio=true",
          "-Dspring.main.web-application-type=none",
          "-Dspring.main.banner-mode=off",
          "-Dlogging.pattern.console=",
          "-jar",
          "target/mcp-weather-starter-webflux-server-0.0.1-SNAPSHOT.jar")
    .build();

var transport = new StdioClientTransport(stdioParams);
var client = McpClient.sync(transport).build();
```

### Claude Desktop Configuration

```json
{
  "mcpServers": {
    "spring-ai-mcp-weather": {
      "command": "java",
      "args": [
        "-Dspring.ai.mcp.server.stdio=true",
        "-Dspring.main.web-application-type=none",
        "-Dspring.main.banner-mode=off",
        "-Dlogging.pattern.console=",
        "-jar",
        "<YOUR ABSOLUTE PATH TO>/mcp-weather-starter-webflux-server-0.0.1-SNAPSHOT.jar"
      ]
    }
  }
}
```

### Client Usage Example

```java
// Initialize client
client.initialize();

// Test connection
client.ping();

// List available tools
ListToolsResult tools = client.listTools();
System.out.println("Available tools: " + tools);

// Get weather forecast for Seattle
CallToolResult weatherForcastResult = client.callTool(new CallToolRequest("getWeatherForecastByLocation",
        Map.of("latitude", 47.6062, "longitude", -122.3321)));
System.out.println("Weather Forecast: " + weatherForcastResult);

// Get weather alerts for New York
CallToolResult alertResult = client.callTool(new CallToolRequest("getAlerts", Map.of("state", "NY")));
System.out.println("Alert Response = " + alertResult);

// Close client
client.closeGracefully();
```

## Additional Resources

* [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
* [Model Context Protocol Specification](https://modelcontextprotocol.github.io/specification/)
* [Spring Boot Auto-configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.developing-auto-configuration)
