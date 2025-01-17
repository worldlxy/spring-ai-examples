package org.springframework.ai.mcp.sample.server;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.mcp.server.McpAsyncServer;
import org.springframework.ai.mcp.server.McpServer;
import org.springframework.ai.mcp.server.McpServer.PromptRegistration;
import org.springframework.ai.mcp.server.McpServer.ResourceRegistration;
import org.springframework.ai.mcp.server.McpServer.ToolRegistration;
import org.springframework.ai.mcp.server.transport.StdioServerTransport;
import org.springframework.ai.mcp.server.transport.WebFluxSseServerTransport;
import org.springframework.ai.mcp.spec.McpSchema;
import org.springframework.ai.mcp.spec.McpSchema.CallToolResult;
import org.springframework.ai.mcp.spec.McpSchema.GetPromptResult;
import org.springframework.ai.mcp.spec.McpSchema.LoggingMessageNotification;
import org.springframework.ai.mcp.spec.McpSchema.PromptMessage;
import org.springframework.ai.mcp.spec.McpSchema.Role;
import org.springframework.ai.mcp.spec.McpSchema.TextContent;
import org.springframework.ai.mcp.spec.ServerMcpTransport;
import org.springframework.ai.mcp.spring.ToolHelper;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.server.RouterFunction;

@Configuration
public class McpServerConfig {

	private static final Logger logger = LoggerFactory.getLogger(McpServerConfig.class);

	// STDIO transport
	@Bean
	@ConditionalOnProperty(prefix = "transport", name = "mode", havingValue = "stdio")
	public StdioServerTransport stdioServerTransport() {
		return new StdioServerTransport();
	}

	// SSE transport
	@Bean
	@ConditionalOnProperty(prefix = "transport", name = "mode", havingValue = "sse")
	public WebFluxSseServerTransport sseServerTransport() {
		return new WebFluxSseServerTransport(new ObjectMapper(), "/mcp/message");
	}

	// Router function for SSE transport used by Spring WebFlux to start an HTTP
	// server.
	@Bean
	@ConditionalOnProperty(prefix = "transport", name = "mode", havingValue = "sse")
	public RouterFunction<?> mcpRouterFunction(WebFluxSseServerTransport transport) {
		return transport.getRouterFunction();
	}

	public static record ToUpperCaseInput(String input) {
	}

	@Bean
	public McpAsyncServer mcpServer(ServerMcpTransport transport, OpenLibrary openLibrary) { // @formatter:off

		// Configure server capabilities with resource support
		var capabilities = McpSchema.ServerCapabilities.builder()
			.resources(false, true) // No subscribe support, but list changes notifications
			.tools(true) // Tool support with list changes notifications
			.prompts(true) // Prompt support with list changes notifications
			.logging() // Logging support
			.build();

		// Create the server with both tool and resource capabilities
		var server = McpServer.using(transport)
			.serverInfo("MCP Demo WebFlux Server", "1.0.0")
			.capabilities(capabilities)
			.resources(systemInfoResourceRegistration()) // Resources
			.prompts(greetingPromptRegistration()) // Prompts
			.tools(openLibraryToolRegistrations(openLibrary)) // Method based tools
			.tools(ToolHelper.toToolRegistration( // java.util.Function based tools
					FunctionCallback.builder()
						.function("toUpperCase", (Function<ToUpperCaseInput, String>) s -> s.input().toUpperCase())
						.description("To upper case")
						.inputType(ToUpperCaseInput.class)						
						.build()))			
			.async();
		
		return server; // @formatter:on
	} // @formatter:on

	public static List<ToolRegistration> openLibraryToolRegistrations(OpenLibrary openLibrary) {

		var books = FunctionCallback.builder()
			.method("getBooks", String.class)
			.description("Get list of Books by title")
			.targetObject(openLibrary)
			.build();

		var bookTitlesByAuthor = FunctionCallback.builder()
			.method("getBookTitlesByAuthor", String.class)
			.description("Get book titles by author")
			.targetObject(openLibrary)
			.build();

		return ToolHelper.toToolRegistration(books, bookTitlesByAuthor);
	}

	private static ResourceRegistration systemInfoResourceRegistration() {

		// Create a resource registration for system information
		var systemInfoResource = new McpSchema.Resource( // @formatter:off
			"system://info",
			"System Information",
			"Provides basic system information including Java version, OS, etc.",
			"application/json", null
		);

		var resourceRegistration = new ResourceRegistration(systemInfoResource, (request) -> {
			try {
				var systemInfo = Map.of(
					"javaVersion", System.getProperty("java.version"),
					"osName", System.getProperty("os.name"),
					"osVersion", System.getProperty("os.version"),
					"osArch", System.getProperty("os.arch"),
					"processors", Runtime.getRuntime().availableProcessors(),
					"timestamp", System.currentTimeMillis());

				String jsonContent = new ObjectMapper().writeValueAsString(systemInfo);

				return new McpSchema.ReadResourceResult(
						List.of(new McpSchema.TextResourceContents(request.uri(), "application/json", jsonContent)));
			}
			catch (Exception e) {
				throw new RuntimeException("Failed to generate system info", e);
			}
		}); // @formatter:on

		return resourceRegistration;
	}

	private static PromptRegistration greetingPromptRegistration() {

		var prompt = new McpSchema.Prompt("greeting", "A friendly greeting prompt",
				List.of(new McpSchema.PromptArgument("name", "The name to greet", true)));

		return new PromptRegistration(prompt, getPromptRequest -> {

			String nameArgument = (String) getPromptRequest.arguments().get("name");
			if (nameArgument == null) {
				nameArgument = "friend";
			}

			var userMessage = new PromptMessage(Role.USER,
					new TextContent("Hello " + nameArgument + "! How can I assist you today?"));

			return new GetPromptResult("A personalized greeting message", List.of(userMessage));
		});
	}

	@Bean
	public OpenLibrary openLibrary() {
		return new OpenLibrary(RestClient.builder());
	}

}
