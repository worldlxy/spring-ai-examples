# Spring AI MCP Servlet SSE Server Sample Project

This sample project demonstrates the usage of the Spring AI Model Context Protocol (MCP) implementation with HttpServletSseServerTransport. It showcases how to create and use MCP servers with Servlet-based SSE transport and various capabilities.

## Overview

The sample provides:
- Two transport mode implementations: 
  - HttpServletSseServerTransport for SSE (Server-Sent Events)
  - StdioServerTransport for standard I/O
- Server capabilities:
  - Tools support with list changes notifications
  - Resources support with list changes notifications (no subscribe support)
  - Prompts support with list changes notifications
- Sample implementations:
  - Two MCP tools: Weather and Calculator
  - One MCP resource: System Information
  - One MCP prompt: Greeting

## Building the Project

```bash
./mvnw clean install -DskipTests
```

## Running the Server

The server can be started in two transport modes, controlled by the `transport.mode` property:

### Stdio Mode (Default)

```bash
java -Dtransport.mode=stdio -jar target/mcp-servlet-server-0.0.1-SNAPSHOT.jar
```

The Stdio mode server is automatically started by the client - no explicit server startup is needed.
But you have to build the server jar first: `./mvnw clean install -DskipTests`.

In Stdio mode the server must not emit any messages/logs to the console (e.g. standard out) but the JSON messages produced by the server.

### SSE Mode with HttpServletSseServerTransport
```bash
java -Dtransport.mode=sse -jar target/mcp-servlet-server-0.0.1-SNAPSHOT.jar
```

The HttpServletSseServerTransport provides two endpoints:
- SSE endpoint at `/sse` - For server-to-client events
- Message endpoint at `/mcp/message` - For client-to-server messages

## Sample Clients

The project includes example clients for both transport modes:

### Stdio Client (ClientStdio.java)
```java
var stdioParams = ServerParameters.builder("java")
    .args("-Dtransport.mode=stdio", "-jar",
            "target/mcp-servlet-server-0.0.1-SNAPSHOT.jar")
    .build();

var transport = new StdioClientTransport(stdioParams);
var client = McpClient.using(transport).sync();
```

### SSE Client (HttpClientSse.java)
```java
var transport = new HttpClientSseClientTransport("http://localhost:8080");
var client = McpClient.using(transport).sync();
```

## Available Tools

### Weather Tool
- Name: `weather`
- Description: Weather forecast tool by location
- Parameters:
  - `city`: String - The city to get weather for
- Example:
```java
CallToolResult response = client.callTool(
    new CallToolRequest("weather", Map.of("city", "Sofia"))
);
```

### Calculator Tool
- Name: `calculator`
- Description: Performs basic arithmetic operations
- Parameters (JSON Schema):
  ```json
  {
    "operation": {
      "type": "string",
      "enum": ["add", "subtract", "multiply", "divide"],
      "description": "The arithmetic operation to perform"
    },
    "a": {
      "type": "number",
      "description": "First operand"
    },
    "b": {
      "type": "number",
      "description": "Second operand"
    }
  }
  ```
- Example:
```java
CallToolResult response = client.callTool(
    new CallToolRequest("calculator", 
        Map.of("operation", "multiply", "a", 2.0, "b", 3.0))
);
```

## Available Resources

### System Information Resource
- URI: `system://info`
- Description: Provides basic system information including Java version, OS, etc.
- MIME Type: application/json
- Returns: JSON object containing: javaVersion, osName, osVersion, osArch, processors, timestamp

## Available Prompts

### Greeting Prompt
- Name: `greeting`
- Description: A friendly greeting prompt
- Parameters:
  - `name`: String (required) - The name to greet
- Returns: A personalized greeting message from an assistant

## Client Usage Example

```java
// Initialize client
client.initialize();

// Test connection
client.ping();

// List available tools
ListToolsResult tools = client.listTools();
System.out.println("Available tools: " + tools);

// Call weather tool
CallToolResult weather = client.callTool(
    new CallToolRequest("weather", Map.of("city", "Sofia"))
);
System.out.println("Weather: " + weather);

// Access system info resource
ReadResourceResult sysInfo = client.readResource(
    new ReadResourceRequest("system://info")
);
System.out.println("System Info: " + sysInfo);

// Use greeting prompt
GetPromptResult greeting = client.getPrompt(
    new GetPromptRequest("greeting", Map.of("name", "John"))
);
System.out.println("Greeting: " + greeting);

// Close client
client.closeGracefully();
```

## Server Configuration Example

```java
@Configuration
@EnableWebMvc
public class McpServerConfig implements WebMvcConfigurer {

    @Bean
    @ConditionalOnProperty(prefix = "transport", name = "mode", havingValue = "sse")
    public HttpServletSseServerTransport webSseServerTransport() {
        return new HttpServletSseServerTransport(new ObjectMapper(), "/mcp/message");
    }

    @Bean
    @ConditionalOnProperty(prefix = "transport", name = "mode", havingValue = "sse")
    public ServletRegistrationBean customServletBean(HttpServletSseServerTransport servlet) {
        return new ServletRegistrationBean(servlet, "/sse", "/mcp/message");
    }

    @Bean
    public McpAsyncServer mcpServer(ServerMcpTransport transport) {
        // Configure server capabilities
        var capabilities = McpSchema.ServerCapabilities.builder()
            .resources(false, true)  // Resource support with list changes notifications
            .tools(true)            // Tool support with list changes notifications
            .prompts(true)          // Prompt support with list changes notifications
            .build();

        // Create and configure the server
        return McpServer.using(transport)
            .serverInfo("MCP Demo Server", "1.0.0")
            .capabilities(capabilities)
            // Add your tools, resources, and prompts here
            .async();
    }
}
```

## Configuration

The application can be configured through `application.properties`:

- `transport.mode`: Transport mode to use (stdio/sse)
- `server.port`: Server port for SSE mode (default: 8080)
- Various logging configurations are available for debugging

## Implementation Details

The sample demonstrates:
- Using HttpServletSseServerTransport for SSE-based communication
- Creating an MCP server with custom tools, resources, and prompts
- Configuring different transport modes
- Implementing tool handlers with JSON schema validation
- Resource implementations with dynamic content generation
- Prompt implementations with parameter handling
- Error handling and response formatting
- Synchronous client usage patterns
