package com.juan.tfg.config;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

public class FirebaseTokenFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        // 1. Verificamos si la petición trae el token
        if (header != null && header.startsWith("Bearer ")) {
            String idToken = header.replace("Bearer ", "");
            try {
                // 2. Verificamos el token con el SDK de Firebase
                FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);

                // 3. Si es válido, creamos la autenticación en el contexto de Spring
                // Usamos el UID de Firebase como el "principal" (identificador del usuario)
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        decodedToken.getUid(), null, new ArrayList<>());

                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (Exception e) {
                // Si el token es falso o ha caducado, no hacemos nada
                // (Spring Security lanzará el 401 automáticamente)
                System.err.println("Error validating token: " + e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
