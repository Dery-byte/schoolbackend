package com.alibou.book.Controllers;

import com.alibou.book.DTO.ChatRequest;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth/chat")
@CrossOrigin(origins = "*")
public class ChatbotController {

    private static final Logger log = LoggerFactory.getLogger(ChatbotController.class);
    @Value("${openai.api.url}")
    private String openAiUrl;

    @Value("${openai.api.key}")
    private String openAiKey;

    @Value("${application.mailing.frontend.baseUrl}")
    private String frontendBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    private String knowledgeBase = "";

    @PostConstruct
    public void loadKnowledge() {
        try {
            ClassPathResource resource = new ClassPathResource("mudita-knowledge.md");
            knowledgeBase = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            // Replace {BASE_URL} placeholders with the actual configured URL
            knowledgeBase = knowledgeBase.replace("{BASE_URL}", frontendBaseUrl);
            log.info("Mudita knowledge base loaded ({} chars)", knowledgeBase.length());
        } catch (Exception e) {
            log.warn("Could not load mudita-knowledge.md, using fallback prompt: {}", e.getMessage());
            knowledgeBase = "You are Mudita, an AI assistant for Ghana's university eligibility checking platform.";
        }
    }

    private String buildSystemPrompt(int userTurns) {
        String base = "You are Mudita, a friendly AI assistant for Ghana's university eligibility checking platform.\n\n" +
               "ALWAYS consult the knowledge base below first before answering. " +
               "If the answer is in the knowledge base, use it directly and precisely — especially for URLs, prices, and steps. " +
               "Only use general knowledge if the topic is not covered below.\n\n" +
               "=== KNOWLEDGE BASE ===\n" +
               knowledgeBase +
               "\n=== END OF KNOWLEDGE BASE ===\n\n" +
               "Be warm, encouraging, and concise.";

        if (userTurns >= 4) {
            base += "\n\nIMPORTANT: The user has now sent " + userTurns + " messages. " +
                    "At a natural point in your next response — after answering their question — " +
                    "casually ask whether they would like to speak directly with a member of the support team " +
                    "who can assist them further. Only ask once; do not repeat this offer in later turns if already asked.";
        }

        return base;
    }

    @PostMapping("/message")
    public ResponseEntity<Map<String, String>> chat(@RequestBody ChatRequest request) {
        try {
            List<Map<String, String>> messages = new ArrayList<>();

            // System prompt
            messages.add(Map.of("role", "system", "content", buildSystemPrompt(request.getUserTurns())));

            // Conversation history (skip the initial greeting)
            if (request.getHistory() != null) {
                for (ChatRequest.ChatMessageDto msg : request.getHistory()) {
                    if (msg.getContent().startsWith("Hi there!") || msg.getContent().startsWith("Hi! I'm Mudita")) continue;
                    messages.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
                }
            }

            // Current user message
            messages.add(Map.of("role", "user", "content", request.getMessage()));

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", "gpt-4o-mini");
            requestBody.put("messages", messages);
            requestBody.put("max_tokens", 1024);
            requestBody.put("temperature", 0.7);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openAiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(openAiUrl, entity, Map.class);

            if (response == null) {
                return ResponseEntity.ok(Map.of("reply", "No response received. Please try again."));
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String reply = (String) message.get("content");

            return ResponseEntity.ok(Map.of("reply", reply));

        } catch (HttpClientErrorException e) {
            log.error("OpenAI client error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            int status = e.getStatusCode().value();
            String msg = status == 401
                    ? "I'm having an authentication issue right now. Please try again shortly."
                    : status == 429
                    ? "I'm receiving too many requests at the moment. Please wait a few seconds and try again."
                    : "I ran into a problem processing your request. Please try again in a moment.";
            return ResponseEntity.ok(Map.of("reply", msg));
        } catch (HttpServerErrorException e) {
            log.error("OpenAI server error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return ResponseEntity.ok(Map.of("reply",
                    "The AI service is temporarily unavailable. Please try again in a moment."));
        } catch (Exception e) {
            log.error("Chatbot error: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of("reply",
                    "I'm sorry, something went wrong on my end. Please try again in a moment."));
        }
    }
}
