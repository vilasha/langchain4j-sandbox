package org.maria.testbox;

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
    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load();
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(dotenv.get("OPENAI_API_KEY"))
                .modelName(OpenAiChatModelName.GPT_4_O_MINI)
                .build();
        ChatMemory memory = MessageWindowChatMemory.withMaxMessages(20);
        ChatService service = AiServices.builder(ChatService.class)
                .chatModel(model)
                .chatMemory(memory)
                .build();

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print(">> ");
                String input = scanner.nextLine();
                if (input.isBlank()) {
                    break;
                }
                System.out.println(service.chat(input));
            }
        }
    }

    interface ChatService {
        @SystemMessage("You are a helpful assistant")
        String chat(@UserMessage String content);
    }
}
