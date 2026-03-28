package org.maria.testbox;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ConsoleChatProgrammatic {
    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load();
        String input;
        List<ChatMessage> history = new ArrayList<>();
        history.add(SystemMessage.from("You are a helpful assistant"));
        ChatModel model = OpenAiChatModel.builder()
                .apiKey(dotenv.get("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build();
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print(">> ");
                input = scanner.nextLine();
                if (input.isEmpty()) {
                    break;
                }
                UserMessage userMessage = UserMessage.from(input);
                history.add(userMessage);
                AiMessage response = model.chat(history.toArray(ChatMessage[]::new)).aiMessage();
                System.out.println(response.text());
                history.add(response);
            }
        }
    }
}
