//package com.alibou.book.auth;
//
//import com.alibou.book.Entity.Providers;
//import com.alibou.book.role.Role;
//import com.alibou.book.role.RoleRepository;
//import com.alibou.book.security.JwtService;
//import com.alibou.book.user.User;
//import com.alibou.book.user.UserRepository;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
//import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
//import org.springframework.security.oauth2.core.user.OAuth2User;
//import org.springframework.security.web.DefaultRedirectStrategy;
//import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//import java.util.UUID;
//
//@Component
//public class OAuthAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
//
//    private static final Logger logger = LoggerFactory.getLogger(OAuthAuthenticationSuccessHandler.class);
//
//    @Autowired
//    private UserRepository userRepo;
//
//    @Autowired
//    private RoleRepository roleRepo;
//
//    @Autowired
//    private JwtService jwtService;
//
//    @Override
//    public void onAuthenticationSuccess(
//            HttpServletRequest request,
//            HttpServletResponse response,
//            Authentication authentication) throws IOException, ServletException {
//
//        try {
//            logger.info("OAuthAuthenticationSuccessHandler invoked");
//
//            if (!(authentication instanceof OAuth2AuthenticationToken)) {
//                logger.error("Authentication is not OAuth2AuthenticationToken: {}", authentication.getClass());
//                redirectToError(response, "Invalid authentication type");
//                return;
//            }
//
//            OAuth2AuthenticationToken oauth2Token = (OAuth2AuthenticationToken) authentication;
//            String provider = oauth2Token.getAuthorizedClientRegistrationId();
//            logger.info("Provider: {}", provider);
//
//            OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
//            loggerOAuthAttributes(oauthUser);
//
//            // Extract user information based on provider
//            User user = extractUserFromOAuth(oauthUser, provider);
//            if (user == null) {
//                redirectToError(response, "Failed to extract user information from OAuth provider");
//                return;
//            }
//
//            // Process user (find existing or create new)
//            User savedUser = processUser(user, oauthUser, provider);
//            if (savedUser == null) {
//                redirectToError(response, "Failed to process user");
//                return;
//            }
//
//            // Generate JWT token
//            String jwt = jwtService.generateToken(savedUser);
//            logger.info("Generated JWT for user: {}", savedUser.getEmail());
//
//            // Redirect to frontend with token
//            redirectToFrontend(request, response, jwt);
//
//        } catch (Exception e) {
//            logger.error("OAuth authentication success handler failed", e);
//            redirectToError(response, "Authentication failed: " + e.getMessage());
//        }
//    }
//
//    private void loggerOAuthAttributes(OAuth2User oauthUser) {
//        Map<String, Object> attributes = oauthUser.getAttributes();
//        logger.info("OAuth2 User Attributes:");
//        attributes.forEach((key, value) ->
//                logger.info("{} : {}", key, value != null ? value.toString() : "null"));
//    }
//
//    private User extractUserFromOAuth(OAuth2User oauthUser, String provider) {
//        try {
//            String email;
//            String name;
//            String providerUserId = oauthUser.getName();
//
//            if ("google".equalsIgnoreCase(provider)) {
//                email = oauthUser.getAttribute("email");
//                name = oauthUser.getAttribute("name");
//            } else if ("github".equalsIgnoreCase(provider)) {
//                email = oauthUser.getAttribute("email");
//                if (email == null || email.isEmpty()) {
//                    String login = oauthUser.getAttribute("login");
//                    email = (login != null ? login + "@github.com" : UUID.randomUUID() + "@github.com");
//                }
//                name = oauthUser.getAttribute("name");
//                if (name == null || name.isEmpty()) {
//                    name = oauthUser.getAttribute("login");
//                }
//            } else {
//                logger.error("Unsupported OAuth provider: {}", provider);
//                return null;
//            }
//
//            // Validate required fields
//            if (email == null || email.isEmpty()) {
//                logger.error("Email is null or empty for provider: {}", provider);
//                return null;
//            }
//
//            if (name == null || name.isEmpty()) {
//                name = email.split("@")[0]; // Use email prefix as name
//            }
//
//            User user = new User();
//            user.setEmail(email);
//            user.setFirstname(name);
//            user.setProviderUserId(providerUserId);
//            user.setProvider(Providers.valueOf(provider.toUpperCase()));
//
//            return user;
//
//        } catch (Exception e) {
//            logger.error("Error extracting user from OAuth data", e);
//            return null;
//        }
//    }
//
//    private User processUser(User user, OAuth2User oauthUser, String provider) {
//        try {
//            // Try to find existing user
//            Optional<Object> existingUserOpt = userRepo.findByEmail(user.getEmail());
//
//
//            if (existingUserOpt.isPresent()) {
//                User existingUser = (User) existingUserOpt.get();
//                logger.info("Found existing user: {}", existingUser.getEmail());
//
//                // Update provider information if needed
//                if (!existingUser.getProvider().name().equalsIgnoreCase(provider)) {
//                    existingUser.setProvider(Providers.valueOf(provider.toUpperCase()));
//                    existingUser.setProviderUserId(oauthUser.getName());
//                    existingUser = userRepo.save(existingUser);
//                }
//
//                return existingUser;
//            } else {
//                // Create new user
//                logger.info("Creating new user for email: {}", user.getEmail());
//
//                // Get USER role safely
//                Role userRole = roleRepo.findByName("USER")
//                        .orElseGet(() -> {
//                            logger.warn("USER role not found, creating it");
//                            Role newRole = new Role();
//                            newRole.setName("USER");
//                            return roleRepo.save(newRole);
//                        });
//
//                user.setRoles(List.of(userRole));
//                user.setEmailVerified(true);
//                user.setEnabled(true);
//                user.setPassword(UUID.randomUUID().toString()); // Secure random password
//
//                User savedUser = userRepo.save(user);
//                logger.info("Created new user: {}", savedUser.getEmail());
//
//                return savedUser;
//            }
//        } catch (Exception e) {
//            logger.error("Error processing user", e);
//            return null;
//        }
//    }
//
//    private void redirectToFrontend(HttpServletRequest request, HttpServletResponse response, String jwt)
//            throws IOException {
//        String redirectUrl = "http://localhost:4200/login-success?token=" + jwt;
//        logger.info("Redirecting to: {}", redirectUrl);
//        new DefaultRedirectStrategy().sendRedirect(request, response, redirectUrl);
//    }
//
//    private void redirectToError(HttpServletResponse response, String errorMessage) throws IOException {
//        String encodedError = java.net.URLEncoder.encode(errorMessage, "UTF-8");
//        String redirectUrl = "http://localhost:4200/auth/error?error=" + encodedError;
//        logger.error("Redirecting to error page: {}", redirectUrl);
//        response.sendRedirect(redirectUrl);
//    }
//}