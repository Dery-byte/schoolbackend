//package com.alibou.book.security;
//
//
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
//import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
//import org.springframework.security.oauth2.core.user.OAuth2User;
//import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
//import org.springframework.stereotype.Service;
//
//
//import java.io.IOException;
//import java.util.Collections;
//
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
//import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
//
//@Service
//public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
//
//    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
//    private final HttpServletResponse response;
//
//    private final JwtService jwtService;
//
//    public CustomOAuth2UserService(HttpServletResponse response, JwtService jwtService) {
//        this.response = response;
//        this.jwtService = jwtService;
//    }
//
//    @Override
//    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
//        // Load user info from Google
//        OAuth2User oAuth2User = delegate.loadUser(userRequest);
//        UserDetails email = oAuth2User.getAttribute("email");
//        String fullName = oAuth2User.getAttribute("name");
//
//        // Generate JWT
//        String token = jwtService.generateToken(email);
//
//        // Redirect Angular with token
//        try {
//            response.sendRedirect("http://localhost:4200/login-success?token=" + token);
//        } catch (IOException e) {
////            throw new OAuth2AuthenticationException("Failed to redirect to frontend", e);
//        }
//
//        // Return a minimal OAuth2User (Spring Security requires it)
//        return new DefaultOAuth2User(
//                Collections.singleton(new SimpleGrantedAuthority("USER")),
//                Collections.singletonMap("email", email),
//                "email"
//        );
//    }
//}
