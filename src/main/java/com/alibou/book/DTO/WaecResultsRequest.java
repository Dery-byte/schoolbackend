package com.alibou.book.DTO;


import lombok.Data;

@Data
public class WaecResultsRequest {

    private String cindex;
    private String examyear;
    private int examtype;
    private String reqref;
}
