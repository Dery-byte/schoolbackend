package com.alibou.book.DTO;

import lombok.Data;

@Data
public class UserSummaryDTOs {
    private Long id;
    private String firstname;
    private String lastname;
    private String phoneNummber;  // typo kept if API returns it that way
    private String fullName;
//    private DeliveryDTO delivery;
}

