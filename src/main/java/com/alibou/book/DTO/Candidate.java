package com.alibou.book.DTO;

import lombok.Data;

@Data
public class Candidate {
    private String cname;
    private String cindex;
    private String dob;
    private int gender;
    private String cimage; // base64
    private String examtype;
}
