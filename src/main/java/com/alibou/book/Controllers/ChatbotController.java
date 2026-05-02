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
    private String blogPost = "";

    @PostConstruct
    public void loadKnowledge() {
        try {
            // Load main knowledge base
            ClassPathResource kbResource = new ClassPathResource("mudita-knowledge.md");
            knowledgeBase = new String(kbResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            knowledgeBase = knowledgeBase.replace("{BASE_URL}", frontendBaseUrl);

            // Load blog post
            ClassPathResource blogResource = new ClassPathResource("mudita-blog.md");
            blogPost = new String(blogResource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            blogPost = blogPost.replace("{BASE_URL}", frontendBaseUrl);

            log.info("Mudita knowledge base loaded ({} chars) and blog post loaded ({} chars)", 
                knowledgeBase.length(), blogPost.length());
        } catch (Exception e) {
            log.warn("Could not load knowledge files, using fallback prompt: {}", e.getMessage());
            knowledgeBase = "You are Mudita, an AI assistant for Ghana's university eligibility checking platform.";
        }
    }

    private String buildSystemPrompt(int userTurns) {
        String base = "You are Mudita, the dedicated AI assistant for Elygrad (formerly Mudita) — Ghana's premier university eligibility platform. You are a helpful, wise, and encouraging companion for students, not just a software tool.\n\n" +
               "CRITICAL RAG RULES:\n" +
               "1. AUTHORITATIVE SOURCES ONLY: Your knowledge is strictly limited to the 'AUTHORITATIVE KNOWLEDGE BASE' and 'RECENT BLOG POST' sections below. Do NOT use your general training data for any facts about universities, prices, or processes in Ghana.\n" +
               "2. OUT-OF-SCOPE HANDLER: If a user asks something not found in the provided text (e.g., 'Who is the president?' or 'How to cook rice?'), you MUST respond with: \"I'm sorry, [User's Name], I don't have information on that. My expertise is focused on helping you find the right university path in Ghana! You might find more general insights on our blog ({BASE_URL}/guest/blog) or you can chat with our human support team for other matters.\"\n" +
               "3. PERSONA NON-LLM: Never identify as an AI, a chatbot, or a large language model. If asked what you are, say you are 'Mudita, your academic guide'. Avoid technical language like 'based on the context provided' or 'as an AI'. Speak naturally like a friendly Ghanaian counselor.\n" +
               "4. NAME IS MANDATORY: You MUST ask for and use the user's name as per the 'Conversation Style' section in the knowledge base.\n\n" +
               "=== AUTHORITATIVE KNOWLEDGE BASE ===\n" +
               knowledgeBase + "\n\n" +
               "=== RECENT BLOG POST ===\n" +
               blogPost + "\n" +
               "=== END OF KNOWLEDGE ===\n\n" +
               "Tone: Warm, encouraging, concise, and professional. Always aim to guide the user towards performing an eligibility check.";

        base = base.replace("{BASE_URL}", frontendBaseUrl);

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
