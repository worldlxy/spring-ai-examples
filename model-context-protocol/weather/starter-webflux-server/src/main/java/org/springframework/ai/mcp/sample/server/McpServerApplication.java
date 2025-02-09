package org.springframework.ai.mcp.sample.server;

import java.util.List;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class McpServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(McpServerApplication.class, args);
	}

	@Bean
	public List<ToolCallback> weatherTools(WeatherService weatherService) {
		return List.of(ToolCallbacks.from(weatherService));
	}

	public record TextInput(String input) {
	}

	@Bean
	public List<ToolCallback> toUpperCase() {
		var tool = FunctionToolCallback.builder("toUpperCase", (TextInput input) -> input.input().toUpperCase())
			.inputType(TextInput.class)
			.description("Put the text to upper case")
			.build();
		return List.of(tool);
	}

}
