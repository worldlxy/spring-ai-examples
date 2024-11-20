package com.example.kotlin_hello_world

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.model.ChatModel
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean

import org.springframework.ai.chat.client.entity

data class Joke(val setup: String, val punchline: String)

@SpringBootApplication
class KotlinHelloWorldApplication {

	@Bean
	fun jokeRunner(chatModel: ChatModel) = CommandLineRunner {
		val response = ChatClient.create(chatModel).prompt().user("Tell me a joke").call().entity<Joke>()

		println("\nJoke:")
		println("Setup: ${response.setup}")
		println("Punchline: ${response.punchline}")
	}
}

fun main(args: Array<String>) {
	runApplication<KotlinHelloWorldApplication>(*args)
}
