package com.alibou.book.auth;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AuthenticationResponse {
    private String token;
    private String fullName;
    private String firstName;
    private String lastName;
    private String email;
}
