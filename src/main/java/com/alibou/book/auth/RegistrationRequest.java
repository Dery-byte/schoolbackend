package com.alibou.book.auth;

import jakarta.persistence.Transient;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class RegistrationRequest {
    @NotEmpty(message = "First Name is mandatory")
    @NotNull(message = "First Name is mandatory")
    private String firstname;
    @NotEmpty(message = "Last Name is mandatory")
    @NotNull(message = "Last Name is mandatory")
    private String lastname;
    @Email(message = "Email is not well formatted")
    @NotEmpty(message = "Email is mandatory")
    @NotNull(message = "Email is mandatory")
    private String email;
    @NotEmpty(message = "Password is mandatory")
    @NotNull(message = "Password is mandatory")
    @Size(min = 8, message = "Password should be 8 characters long minimum")
    private String password;
    @Transient
    private String confirmPassword;

    @NotEmpty(message = "Phone No. is required")
    private List<String> phoneNumber;
}
