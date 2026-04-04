package org.maria.testbox.console.chat;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.Scanner;

public class ConsoleChatDeclarative {
    private static final int MEMORY_WINDOW = 20;
    private static final String SYSTEM_PROMPT = "You are a helpful assistant";

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load();
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(dotenv.get("OPENAI_API_KEY"))
                .modelName(OpenAiChatModelName.GPT_4_O_MINI)
                .build();
        ChatMemory memory = MessageWindowChatMemory.withMaxMessages(MEMORY_WINDOW);
        ChatService service = AiServices.builder(ChatService.class)
                .chatModel(model)
                .chatMemory(memory)
                .build();

        System.out.println("Chat started. Type 'exit' or press Enter on an empty line to quit.");
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print(">> ");
                String input = scanner.nextLine();
                if (input.isBlank() || input.equalsIgnoreCase("exit")) {
                    break;
                }
                try {
                    System.out.println(service.chat(input));
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                }
            }
        }
    }

    interface ChatService {
        @SystemMessage(SYSTEM_PROMPT)
        String chat(@UserMessage String content);
    }
}
