//package com.alibou.book.auth;
//
//import com.alibou.book.role.Role;
//import com.alibou.book.role.RoleRepository;
//import com.alibou.book.security.JwtService;
//import com.alibou.book.user.User;
//import com.alibou.book.user.UserRepository;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.oauth2.core.user.OAuth2User;
//import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//import java.util.List;
//
//@Component
//public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
//
//    private final JwtService jwtService;
//    private final UserRepository userRepository;
//    private final RoleRepository roleRepository;
//
//    public OAuth2LoginSuccessHandler(JwtService jwtService, UserRepository userRepository, RoleRepository roleRepository) {
//        this.jwtService = jwtService;
//        this.userRepository = userRepository;
//        this.roleRepository = roleRepository;
//    }
//
//    @Override
//    public void onAuthenticationSuccess(HttpServletRequest request,
//                                        HttpServletResponse response,
//                                        Authentication authentication) throws IOException {
//        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
//        String email = oAuth2User.getAttribute("email");
//        String name = oAuth2User.getAttribute("name");
//
////        var userRole = roleRepository.findByName("USER");
//        var userRole = roleRepository.findByName("USER")
//                // todo - better exception handling
//                .orElseThrow(() -> new IllegalStateException("ROLE USER was not initiated"));
//        // 1. Find or create user
//        User user = (User) userRepository.findByEmail(email).orElseGet(() -> {
//            User newUser = new User();
//            newUser.setEmail(email);
//            newUser.setFirstname(name);
//
//            // fetch role from DB
//
//
//            newUser.setRoles(List.of(userRole));
//            return userRepository.save(newUser);
//        });
//        // 2. Generate JWT with full User (including roles)
//        String token = jwtService.generateToken(String.valueOf(user));
//
//        // 3. Redirect to Angular with JWT
//        getRedirectStrategy().sendRedirect(
//                request,
//                response,
//                "http://localhost:4200/login-success?token=" + token
//        );
//    }
//}




package com.alibou.book.auth;

import com.alibou.book.role.Role;
import com.alibou.book.role.RoleRepository;
import com.alibou.book.security.JwtService;
import com.alibou.book.user.User;
import com.alibou.book.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {


    @Value("${application.mailing.frontend.baseUrl}")
    private String frontendRedirect;


    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public OAuth2LoginSuccessHandler(JwtService jwtService,
                                     UserRepository userRepository,
                                     RoleRepository roleRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String firstName = oAuth2User.getAttribute("given_name");

        String lastName = oAuth2User.getAttribute("family_name");
        System.out.println("This is the last name " + lastName);

        System.out.println(" This is teh first name : " + firstName);

        // Ensure USER role exists
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("ROLE USER was not initiated"));

        // Find or create user
        User user = (User) userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setUsername(email);
            newUser.setFirstname(firstName);
            newUser.setLastname(lastName);
            newUser.setPassword(""); // no password for OAuth users
            newUser.setRoles(List.of(userRole)); // assign default USER role
            return userRepository.save(newUser);
        });

        // Ensure roles are not null for existing user
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            user.setRoles(List.of(userRole));
            user = userRepository.save(user);
        }

        // Generate JWT for the actual User entity
//        String token = jwtService.generateToken(user.getEmail());
        String token = jwtService.generateToken(new HashMap<>(), user);


        // Redirect to Angular with JWT
        String redirectUrl = frontendRedirect + "/login-success?token=" + token;
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
