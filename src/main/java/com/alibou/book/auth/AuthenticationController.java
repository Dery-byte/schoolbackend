package com.alibou.book.auth;

import com.alibou.book.DTO.ForgottenPasswordRequest;
import com.alibou.book.DTO.ResetPasswordRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthenticationController {

    private final AuthenticationService service;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<?> register(
            @RequestBody @Valid RegistrationRequest request
    ) throws MessagingException {
        System.out.println("Received registration request: " + request);
        service.register(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody @Valid AuthenticationRequest request) throws MessagingException {
        return ResponseEntity.ok(service.authenticate(request));
    }


    @GetMapping("/activate-account")
    public void confirm(@RequestParam String token) throws MessagingException {
        service.activateAccount(token);
    }






    @PostMapping("/forgotten-password")
    public ResponseEntity<Void> forgottenPassword(
            @RequestBody ForgottenPasswordRequest request
    ) throws MessagingException {
        service.forgottenPassword(request);
        return ResponseEntity.accepted().build();
    }
    @PostMapping("/update-password")
    public ResponseEntity<Void> resetPassword(
            @RequestBody ResetPasswordRequest request
    ) {
        service.resetPassword(request);
        return ResponseEntity.ok().build();
    }







}
