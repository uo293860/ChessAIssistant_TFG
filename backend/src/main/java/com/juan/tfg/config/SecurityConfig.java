package com.juan.tfg.config;

import com.google.firebase.FirebaseApp;
import com.juan.tfg.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configures HTTP security, CORS, CSRF, authorization rules, and Firebase token authentication.
     *
     * @param http the Spring Security HTTP configuration builder.
     * @param firebaseTokenFilter the filter that validates Firebase bearer tokens.
     * @return the configured security filter chain.
     * @throws Exception if Spring Security cannot build the filter chain.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, FirebaseTokenFilter firebaseTokenFilter) throws Exception {
        http.cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(firebaseTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Creates the Firebase token filter used by the security filter chain.
     *
     * @param userService the service that creates or loads authenticated users.
     * @param firebaseApp the initialized Firebase application.
     * @return a Firebase token filter instance.
     */
    @Bean
    public FirebaseTokenFilter firebaseTokenFilter(UserService userService, FirebaseApp firebaseApp) {
        return new FirebaseTokenFilter(userService, firebaseApp);
    }
}
