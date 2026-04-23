package com.alibou.book.DTO;

import lombok.Data;

@Data
public class ReqStatus {
    private String msg;
    private int msgcode;
    private String gencode;
    private String reqtime;
    private long tid;
    private int requesttype;
}
