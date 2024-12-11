package org.springframework.ai.mcp.samples.brave;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.client.McpClient;
import org.springframework.ai.mcp.client.McpSyncClient;
import org.springframework.ai.mcp.client.stdio.ServerParameters;
import org.springframework.ai.mcp.client.stdio.StdioServerTransport;
import org.springframework.ai.mcp.spring.McpFunctionCallback;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean
	public CommandLineRunner predefinedQuestions(ChatClient.Builder chatClientBuilder,
			McpSyncClient mcpClient, ConfigurableApplicationContext context) {

		return args -> {

			List<McpFunctionCallback> functionCallbacks = mcpClient.listTools(null)
					.tools()
					.stream()
					.map(tool -> new McpFunctionCallback(mcpClient, tool))
					.toList();

			System.out.println("Available tools:");
			functionCallbacks.stream().map(fc -> fc.getName()).forEach(System.out::println);

			var chatClient = chatClientBuilder
					.defaultFunctions(functionCallbacks.toArray(new McpFunctionCallback[0]))
					.build();

			String question = "Can you explain what is Spring AI and if it supports the Model Context Protocol?";
			System.out.println("QUESTION: " + question);
			System.out.println("ASSISTANT: " + chatClient.prompt(question).call().content());

			context.close();
		};
	}

	@Bean(destroyMethod = "close")
	public McpSyncClient mcpClient() {

		// https://github.com/modelcontextprotocol/servers/tree/main/src/brave-search
		var stdioParams = ServerParameters.builder("npx")
				.args("-y", "@modelcontextprotocol/server-brave-search")
				.addEnvVar("BRAVE_API_KEY", System.getenv("BRAVE_API_KEY"))
				.build();

		var mcpClient = McpClient.sync(new StdioServerTransport(stdioParams));
		var init = mcpClient.initialize();
		System.out.println("MCP Initialized: " + init);
		return mcpClient;
	}

}