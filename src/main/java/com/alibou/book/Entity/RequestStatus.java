package com.alibou.book.Entity;

import jakarta.persistence.Embeddable;

@Embeddable
public class RequestStatus {

    private String msg;
    private int msgcode;
    private String gencode;
    private String reqtime;
    private long tid;
    private int requesttype;

    // getters and setters
}
