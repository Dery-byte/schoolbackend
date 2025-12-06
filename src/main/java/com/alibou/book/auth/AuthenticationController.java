package com.alibou.book.auth;

import com.alibou.book.DTO.ForgottenPasswordRequest;
import com.alibou.book.DTO.Projections.UserSummaryDTO;
import com.alibou.book.DTO.ResetPasswordRequest;
import com.alibou.book.user.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;

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
    ) throws MessagingException, UnsupportedEncodingException {
        System.out.println("Received registration request: " + request);
        service.register(request);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody @Valid AuthenticationRequest request) throws MessagingException {
        return ResponseEntity.ok(service.authenticate(request));
    }


    @GetMapping("/activate-account")
    public void confirm(@RequestParam String token) throws MessagingException, UnsupportedEncodingException {
        service.activateAccount(token);
    }






    @PostMapping("/forgotten-password")
    public ResponseEntity<Void> forgottenPassword(
            @RequestBody ForgottenPasswordRequest request
    ) throws MessagingException, UnsupportedEncodingException {
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






    @GetMapping("/latestUsersSummary")
    public ResponseEntity<Page<UserSummaryDTO>> getLatestUsersSummary(
            @RequestParam(defaultValue = "6") int count) {
        return ResponseEntity.ok(service.getLatestUsersSummary(count));
    }

    // Get latest X non-admin users
    @GetMapping("/latest/non-admins")
    public ResponseEntity<Page<User>> getLatestNonAdminUsers(
            @RequestParam(defaultValue = "10") int count) {
        return ResponseEntity.ok(service.getLatestNonAdminUsers(count));
    }

    // Returns just the count number
    @GetMapping("/count")
    public long getNonAdminCount() {
        return service.countNonAdminUsers();
    }
}
