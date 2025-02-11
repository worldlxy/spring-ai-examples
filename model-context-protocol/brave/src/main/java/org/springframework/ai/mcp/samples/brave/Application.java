package org.springframework.ai.mcp.samples.brave;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallback;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {

	private static final Logger logger = LoggerFactory.getLogger(Application.class);

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean
	public CommandLineRunner predefinedQuestions(ChatClient.Builder chatClientBuilder,
												 McpSyncClient mcpSyncClient, ConfigurableApplicationContext context) {

		return args -> {

			var chatClient = chatClientBuilder
					.defaultTools(mcpSyncClient.listTools(null)
							.tools()
							.stream()
							.map(tool -> new SyncMcpToolCallback(mcpSyncClient, tool))
							.toArray(SyncMcpToolCallback[]::new))
					.build();

			String question = "Does Spring AI supports the Model Context Protocol? Please provide some references.";
			logger.info("QUESTION: {}\n", question);
			logger.info("ASSISTANT: {}\n", chatClient.prompt(question).call().content());

			context.close();
		};
	}

	@Bean
	public McpSyncClient mcpClient() {

		// https://github.com/modelcontextprotocol/servers/tree/main/src/brave-search
		var stdioParams = ServerParameters.builder("npx")
				.args("-y", "@modelcontextprotocol/server-brave-search")
				.addEnvVar("BRAVE_API_KEY", System.getenv("BRAVE_API_KEY"))
				.build();

		var mcpClient = McpClient.sync(new StdioClientTransport(stdioParams)).build();
		var init = mcpClient.initialize();
		logger.info("MCP Initialized: {}", init);
		return mcpClient;
	}

}