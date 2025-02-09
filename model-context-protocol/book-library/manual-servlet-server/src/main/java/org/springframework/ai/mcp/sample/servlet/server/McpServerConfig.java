package org.springframework.ai.mcp.sample.servlet.server;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures.SyncPromptRegistration;
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceRegistration;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolRegistration;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransport;
import io.modelcontextprotocol.server.transport.StdioServerTransport;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.ServerMcpTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
public class McpServerConfig implements WebMvcConfigurer {

	private static final Logger logger = LoggerFactory.getLogger(McpServerConfig.class);

	@Bean
	@ConditionalOnProperty(prefix = "transport", name = "mode", havingValue = "sse")
	public HttpServletSseServerTransport webMvcSseServerTransport() {
		return new HttpServletSseServerTransport(new ObjectMapper(), "/mcp/message");
	}

	@Bean
	@ConditionalOnProperty(prefix = "transport", name = "mode", havingValue = "sse")
	public ServletRegistrationBean customServletBean(HttpServletSseServerTransport servlet) {
		return new ServletRegistrationBean(servlet);
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
			.serverInfo("MCP Demo Servlet Server", "1.0.0")
			.capabilities(capabilities)
			.resources(systemInfoResourceRegistration())
			.prompts(greetingPromptRegistration())
			.tools(
				McpToolUtils.toSyncToolRegistration(
					FunctionToolCallback.builder("toUpperCase", (Function<ToUpperCaseInput, String>) s -> s.input().toUpperCase())
					.description("To upper case")
					.inputType(ToUpperCaseInput.class)						
					.build()))
			.tools(openLibraryToolRegistrations(openLibrary))
			.build();		
		return server; // @formatter:on
	} // @formatter:on

	public static List<SyncToolRegistration> openLibraryToolRegistrations(OpenLibrary openLibrary) {
		ToolCallback[] tools = ToolCallbacks.from(openLibrary);
		return McpToolUtils.toSyncToolRegistration(tools);
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
}
