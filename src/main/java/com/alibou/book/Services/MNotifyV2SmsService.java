package com.alibou.book.Services;


import com.alibou.book.DTO.SmsRequest;
import com.alibou.book.config.MNotifyV2Config;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Service
public class MNotifyV2SmsService {

    @Autowired
    private MNotifyV2Config mNotifyV2Config;

    private final RestTemplate restTemplate = new RestTemplate();

    public String sendSms(List<String> recipients, String message) {
        try {
            String urlWithKey = UriComponentsBuilder.fromHttpUrl(mNotifyV2Config.getUrl())
                    .queryParam("key", mNotifyV2Config.getKey())
                    .toUriString();

            System.out.println("Sending to: " + urlWithKey);
            System.out.println("Using key: " + mNotifyV2Config.getKey());

            SmsRequest smsRequest = new SmsRequest();
            smsRequest.setRecipient(recipients);
            smsRequest.setSender(mNotifyV2Config.getSenderId());
            smsRequest.setMessage(message);


            System.out.println(smsRequest.getMessage());
            System.out.println(smsRequest);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<SmsRequest> requestEntity = new HttpEntity<>(smsRequest, headers);
            System.out.println(requestEntity);

            ResponseEntity<String> response = restTemplate.exchange(
                    urlWithKey,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );
            System.out.println("This is the reponses"+ response);

            System.out.println(new ObjectMapper().writeValueAsString(smsRequest));
            System.out.println("Response: " + response.getBody());

            return response.getBody();

        } catch (Exception e) {
            System.err.println("❌ Exception during SMS sending: " + e.getMessage());
            e.printStackTrace();
            return "SMS sending failed: " + e.getMessage();
        }
    }

}
