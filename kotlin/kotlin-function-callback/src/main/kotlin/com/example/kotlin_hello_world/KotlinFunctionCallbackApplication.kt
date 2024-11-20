package com.example.kotlin_hello_world

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.model.function.FunctionCallback
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Description

@SpringBootApplication
class KotlinFunctionCallbackApplication {

	companion object {
		@JvmStatic
		fun main(args: Array<String>) {
			runApplication<KotlinFunctionCallbackApplication>(*args)
		}
	}

	@Bean
	open fun init(chatModel: ChatModel) = CommandLineRunner {
		try {
			val userMessage = UserMessage(
				"What are the weather conditions in San Francisco, Tokyo, and Paris? Find the temperature in Celsius for each of the three locations."
			)

			val response = chatModel.call(
				Prompt(
					listOf(userMessage),
					OpenAiChatOptions.builder().withFunction("WeatherInfo").build()
				)
			)

			println("Response: $response")
		} catch (e: Exception) {
			println("Error during weather check: ${e.message}")
			e.printStackTrace()
		}
	}
}

@Configuration
class Config {
	@Bean
	fun weatherFunctionInfo(currentWeather: (WeatherRequest) -> WeatherResponse): FunctionCallback {
		return FunctionCallback.builder()
			.description(
				"Find the weather conditions, forecasts, and temperatures for a location, like a city or state."
			)
			.function("WeatherInfo", currentWeather)
			.inputType(WeatherRequest::class.java)
			.build()
	}

	@Bean
	@Description("Get current weather")
	fun currentWeather(): (WeatherRequest) -> WeatherResponse = { request ->
		MockKotlinWeatherService().invoke(request)
	}
}
