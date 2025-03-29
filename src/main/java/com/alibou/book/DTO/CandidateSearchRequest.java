package com.alibou.book.DTO;

import lombok.Data;

@Data
public class CandidateSearchRequest {
    private String cindex;
    private String examyear;
    private String examtype;
}
