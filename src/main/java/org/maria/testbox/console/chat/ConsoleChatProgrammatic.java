package org.maria.testbox.console.chat;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ConsoleChatProgrammatic {
    private static final String SYSTEM_PROMPT = "You are a helpful assistant";

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load();
        String input;
        List<ChatMessage> history = new ArrayList<>();
        history.add(SystemMessage.from(SYSTEM_PROMPT));
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(dotenv.get("OPENAI_API_KEY"))
                .modelName(OpenAiChatModelName.GPT_4_O_MINI)
                .build();

        System.out.println("Chat started. Type 'exit' or press Enter on an empty line to quit.");
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print(">> ");
                input = scanner.nextLine();
                if (input.isBlank() || input.equalsIgnoreCase("exit")) {
                    break;
                }
                UserMessage userMessage = UserMessage.from(input);
                history.add(userMessage);
                try {
                    AiMessage response = model.chat(history.toArray(ChatMessage[]::new)).aiMessage();
                    System.out.println(response.text());
                    history.add(response);
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                    history.remove(history.size() - 1);
                }
            }
        }
    }
}
