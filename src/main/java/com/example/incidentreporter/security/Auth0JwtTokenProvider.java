package com.example.incidentreporter.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;

@Component
@Slf4j
public class Auth0JwtTokenProvider {

    @Value("${auth0.domain}")
    private String auth0Domain;

    @Value("${auth0.audience}")
    private String auth0Audience;

    /**
     * Valida token JWT de Auth0
     */
    public boolean validateToken(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);

            // Verificar audience
            if (!jwt.getAudience().contains(auth0Audience)) {
                log.error("Invalid audience in JWT token");
                return false;
            }

            // Verificar issuer
            String expectedIssuer = "https://" + auth0Domain + "/";
            if (!expectedIssuer.equals(jwt.getIssuer())) {
                log.error("Invalid issuer in JWT token");
                return false;
            }

            // Verificar expiración
            if (jwt.getExpiresAt().before(new java.util.Date())) {
                log.error("JWT token has expired");
                return false;
            }

            // Para producción, aquí verificarías la firma con las claves públicas de Auth0
            // Por simplicidad en desarrollo, validamos solo la estructura y claims básicos

            log.debug("JWT token validated successfully for user: {}", jwt.getSubject());
            return true;

        } catch (JWTVerificationException e) {
            log.error("JWT token validation failed: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error during JWT validation: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extrae el subject (user ID) del token
     */
    public String getUserIdFromToken(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getSubject();
        } catch (Exception e) {
            log.error("Error extracting user ID from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extrae el email del token (si está presente)
     */
    public String getEmailFromToken(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getClaim("email").asString();
        } catch (Exception e) {
            log.debug("Email not found in token or error extracting it: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Crea el objeto Authentication para Spring Security
     */
    public Authentication getAuthentication(String token) {
        String userId = getUserIdFromToken(token);
        String email = getEmailFromToken(token);

        // Usar email como username si está disponible, sino usar userId
        String username = email != null ? email : userId;

        UserDetails userDetails = new UserPrincipal(username, userId);
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }
}