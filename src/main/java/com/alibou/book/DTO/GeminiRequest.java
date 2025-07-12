package com.alibou.book.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GeminiRequest {

    private List<Content> contents;

    public GeminiRequest(String text) {
        this.contents = new ArrayList<>();
        this.contents.add(new Content(text));
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Content {
        private List<Part> parts;

        public Content(String text) {
            this.parts = new ArrayList<>();
            this.parts.add(new Part(text));
        }
    }

    @Data
    @NoArgsConstructor
    public static class Part {
        private String text;

        public Part(String text) {
            this.text = text;
        }
    }



}
