# Spring AI Model Context Protocol (MCP) Examples

This directory contains various examples demonstrating the usage of Spring AI's Model Context Protocol (MCP). Each example showcases different aspects of MCP implementation, including various transport methods and client-server configurations.


## Transport Types

The examples demonstrate two main types of transport:

1. **STDIO Transport**
   - Process-based communication
   - Synchronous communication
   - Used in all client examples
   - Available in all server implementations

2. **HTTP SSE (Server-Sent Events) Transport**
   - HTTP-based streaming communication
   - Asynchronous communication
   - Available in server implementations
   - Implemented in three variants:
     - Servlet-based (Spring MVC)
     - WebFlux-based (Reactive)
     - WebMVC-based

## API Types

The examples demonstrate both synchronous and asynchronous API usage:

- **Synchronous API**: Used in STDIO transport implementations
- **Asynchronous API**: Used in SSE transport implementations

## Getting Started

Each example project can be built using Maven:

```bash
./mvnw clean install
```

For running the examples:
1. For STDIO transport: Run the client application directly
2. For SSE transport: Start the server first, then run the client

## Note

- All server implementations support both STDIO and SSE transport modes
- Transport mode can be configured using the `transport.mode` property
- Client examples primarily use STDIO transport for simplicity
- Server starters provide auto-configuration support for easier integration
## Example Projects Overview

### Current Implementations
These projects use current Spring AI MCP dependencies:

#### Spring Boot Starter Projects
These use the `spring-ai-mcp-spring-boot-starter` dependency:

#### `brave-chatbot`
- **Type**: Client
- **Transport**: STDIO
- **Framework**: Spring Boot with MCP Starter
- **Description**: Enhanced version of the Brave client with chatbot capabilities.

#### `brave-starter`
- **Type**: Client Starter
- **Transport**: STDIO
- **Framework**: Spring Boot with MCP Starter
- **Description**: Spring Boot starter version of the Brave client.

#### `mcp-webflux-server-starter`
- **Type**: Server Starter
- **Transport**: Supports both HTTP SSE and STDIO
- **Framework**: Spring WebFlux with MCP Starter
- **Description**: Spring Boot starter for WebFlux server with auto-configuration support.

#### `mcp-weather-server-quickstart`
- **Type**: Server Quickstart
- **Transport**: Supports both HTTP SSE and STDIO
- **Framework**: Spring Boot with MCP Starter
- **Description**: Simplified version of the weather server for quick start purposes.

#### `mcp-weather-server-starter`
- **Type**: Server Starter
- **Transport**: Supports both HTTP SSE and STDIO
- **Framework**: Spring Boot with MCP Starter
- **Description**: Spring Boot starter version of the weather server, with auto-configuration support.

#### Manual Configuration Examples
These demonstrate how to create MCP applications without Spring Boot auto-configuration:

#### `mcp-weather-server`
- **Type**: Server
- **Transport**: Supports both HTTP SSE and STDIO
- **Framework**: Spring Boot with WebFlux
- **Dependencies**: Uses `spring-ai-bom` and `mcp-bom` for dependency management
- **Description**: Weather service implementation showing how to manually configure an MCP application without using spring-boot-starter. Demonstrates manual configuration patterns while maintaining clean dependency management through BOMs.

### Legacy Projects (Using Experimental Dependency)
These projects use the experimental dependency `org.springframework.experimental:spring-ai-mcp:0.6.0` and need updating:

#### Server Implementations

#### `mcp-servlet-server`
- **Type**: Server
- **Transport**: Supports both HTTP SSE and STDIO
- **Framework**: Spring MVC
- **Status**: Uses experimental dependency
- **Description**: Demonstrates a server implementation using Spring MVC with servlet-based SSE transport. Includes OpenLibrary integration.

#### `mcp-webflux-server`
- **Type**: Server
- **Transport**: Supports both HTTP SSE and STDIO
- **Framework**: Spring WebFlux
- **Status**: Uses experimental dependency
- **Description**: Shows a reactive server implementation using Spring WebFlux with SSE transport. Includes OpenLibrary integration.

#### `mcp-webmvc-server`
- **Type**: Server
- **Transport**: Supports both HTTP SSE and STDIO
- **Framework**: Spring WebMVC
- **Status**: Uses experimental dependency
- **Description**: Another server implementation using Spring WebMVC, demonstrating SSE transport integration.

#### Client Examples

#### `brave`
- **Type**: Client
- **Transport**: STDIO
- **Framework**: Spring Boot
- **Status**: Uses experimental dependency
- **Description**: Client implementation for Brave Search integration.

#### `filesystem`
- **Type**: Client
- **Transport**: STDIO
- **Framework**: Spring Boot
- **Status**: Uses experimental dependency
- **Description**: Example showing filesystem operations through MCP.

#### `sqlite`
- **Type**: Client
- **Transport**: STDIO
- **Framework**: Spring Boot
- **Status**: Uses experimental dependency
- **Description**: Demonstrates SQLite database integration through MCP (includes both simple and chatbot variants).


