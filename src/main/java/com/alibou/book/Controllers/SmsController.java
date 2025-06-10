package com.alibou.book.Controllers;

import com.alibou.book.DTO.SendSmsRequest;
import com.alibou.book.Services.MNotifyV2SmsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/sms")
public class SmsController {

    @Autowired
    private MNotifyV2SmsService mNotifyV2SmsService;
    @PostMapping("/send")
    public String sendSms(@RequestBody SendSmsRequest request) throws JsonProcessingException {
        return mNotifyV2SmsService.sendSms(request.getRecipient(), request.getMessage());
    }
}
