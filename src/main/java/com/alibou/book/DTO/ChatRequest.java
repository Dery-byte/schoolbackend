package com.alibou.book.DTO;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    private String message;
    private List<ChatMessageDto> history;
    private int userTurns;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessageDto {
        private String role;    // "user" or "assistant"
        private String content;
    }
}
