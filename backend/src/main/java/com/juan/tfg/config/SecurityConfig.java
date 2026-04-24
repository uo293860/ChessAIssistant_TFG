package com.juan.tfg.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Desactivamos CSRF porque es una API REST y no usa formularios HTML clásicos
                .csrf(csrf -> csrf.disable())

                // Configuramos los permisos de las rutas
                .authorizeHttpRequests(auth -> auth
                        // Permitimos el acceso público a nuestra API de puzles para probar
                        .requestMatchers("/api/puzzles/**").permitAll()

                        // Cualquier otra petición requerirá autenticación
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
