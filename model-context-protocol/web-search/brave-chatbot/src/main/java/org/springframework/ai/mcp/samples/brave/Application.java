package org.springframework.ai.mcp.samples.brave;

import java.util.List;
import java.util.Scanner;

import io.modelcontextprotocol.client.McpSyncClient;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean
	public CommandLineRunner chatbot(ChatClient.Builder chatClientBuilder, List<McpSyncClient> mcpSyncClients) {

		return args -> {

			var chatClient = chatClientBuilder
					.defaultSystem("You are useful assistant and can perform web searches Brave's search API to reply to your questions.")
					.defaultToolCallbacks(new SyncMcpToolCallbackProvider(mcpSyncClients))
					.defaultAdvisors(MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().build()).build())
					.build();

			// Start the chat loop
			System.out.println("\nI am your AI assistant.\n");
			try (Scanner scanner = new Scanner(System.in)) {
				while (true) {
					System.out.print("\nUSER: ");
					System.out.println("\nASSISTANT: " +
							chatClient.prompt(scanner.nextLine()) // Get the user input
									.call()
									.content());
				}
			}

		};
	}
}