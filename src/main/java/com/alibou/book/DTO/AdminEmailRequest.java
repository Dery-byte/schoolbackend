package com.alibou.book.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminEmailRequest {
    private String toEmail;
    private String recipientName;
    private String subject;
    private String messageBody;
}
