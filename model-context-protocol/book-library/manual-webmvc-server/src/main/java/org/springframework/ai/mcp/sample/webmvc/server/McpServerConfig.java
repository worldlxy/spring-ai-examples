package org.springframework.ai.mcp.sample.webmvc.server;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.mcp.server.McpServer;
import org.springframework.ai.mcp.server.McpServerFeatures.SyncPromptRegistration;
import org.springframework.ai.mcp.server.McpServerFeatures.SyncResourceRegistration;
import org.springframework.ai.mcp.server.McpServerFeatures.SyncToolRegistration;
import org.springframework.ai.mcp.server.McpSyncServer;
import org.springframework.ai.mcp.server.transport.StdioServerTransport;
import org.springframework.ai.mcp.server.transport.WebMvcSseServerTransport;
import org.springframework.ai.mcp.spec.McpSchema;
import org.springframework.ai.mcp.spec.McpSchema.GetPromptResult;
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
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration
@EnableWebMvc
public class McpServerConfig implements WebMvcConfigurer {

	private static final Logger logger = LoggerFactory.getLogger(McpServerConfig.class);

	@Bean
	@ConditionalOnProperty(prefix = "transport", name = "mode", havingValue = "sse")
	public WebMvcSseServerTransport webMvcSseServerTransport() {
		return new WebMvcSseServerTransport(new ObjectMapper(), "/mcp/message");
	}

	@Bean
	@ConditionalOnProperty(prefix = "transport", name = "mode", havingValue = "sse")
	public RouterFunction<ServerResponse> routerFunction(WebMvcSseServerTransport transport) {
		return transport.getRouterFunction();
	}

	// STDIO transport
	@Bean
	@ConditionalOnProperty(prefix = "transport", name = "mode", havingValue = "stdio")
	public StdioServerTransport stdioServerTransport() {
		return new StdioServerTransport();
	}

	public static record ToUpperCaseInput(String input) {
	}

	@Bean
	public McpSyncServer mcpServer(ServerMcpTransport transport, OpenLibrary openLibrary) { // @formatter:off

		// Configure server capabilities with resource support
		var capabilities = McpSchema.ServerCapabilities.builder()
			.resources(false, true) // No subscribe support, but list changes notifications
			.tools(true) // Tool support with list changes notifications
			.prompts(true) // Prompt support with list changes notifications
			.logging() // Logging support
			.build();

		// Create the server with both tool and resource capabilities
		var server = McpServer.sync(transport)
			.serverInfo("MCP Demo WebMVC Server", "1.0.0")
			.capabilities(capabilities)
			.resources(systemInfoResourceRegistration())
			.prompts(greetingPromptRegistration())
			.tools(
				ToolHelper.toSyncToolRegistration(
					FunctionCallback.builder()
						.function("toUpperCase", (Function<ToUpperCaseInput, String>) s -> s.input().toUpperCase())
						.description("To upper case")
						.inputType(ToUpperCaseInput.class)						
						.build()))
			.tools(openLibraryToolRegistrations(openLibrary))
			.build();
		return server; // @formatter:on
	} // @formatter:on

	public static List<SyncToolRegistration> openLibraryToolRegistrations(OpenLibrary openLibrary) {

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

		return ToolHelper.toSyncToolRegistration(books, bookTitlesByAuthor);
	}

	private static SyncResourceRegistration systemInfoResourceRegistration() {

		// Create a resource registration for system information
		var systemInfoResource = new McpSchema.Resource( // @formatter:off
			"system://info",
			"System Information",
			"Provides basic system information including Java version, OS, etc.",
			"application/json", null
		);

		var resourceRegistration = new SyncResourceRegistration(systemInfoResource, (request) -> {
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

	private static SyncPromptRegistration greetingPromptRegistration() {

		var prompt = new McpSchema.Prompt("greeting", "A friendly greeting prompt",
				List.of(new McpSchema.PromptArgument("name", "The name to greet", true)));

		return new SyncPromptRegistration(prompt, getPromptRequest -> {

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
