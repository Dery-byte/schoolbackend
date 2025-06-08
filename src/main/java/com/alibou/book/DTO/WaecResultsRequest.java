package com.alibou.book.DTO;


import lombok.Data;

@Data
public class WaecResultsRequest {

    private String cindex;
    private String examyear;
    private Long examtype;
    private String reqref;
}
