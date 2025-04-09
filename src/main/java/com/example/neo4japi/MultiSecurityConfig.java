package com.example.neo4japi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@Configuration
public class MultiSecurityConfig {

    @Value("${spring.security.enabled}") // Toggle security via application.yml
    private boolean securityEnabled;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwt-custom-uri}") // Toggle security via application.yml
    private String jwtCustomUri;

    // for proxy 
    private final JwtDecoder jwtDecoder;
    public MultiSecurityConfig(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/**"); // Explicit matcher definition

        if (securityEnabled) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    //.httpBasic()
                    // does not work with default proxy
                    //.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(JwtDecoders.fromIssuerLocation(jwtCustomUri)) ))
                    //.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder))) // Use the custom JwtDecoder)
                    //.oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwkSetUri("https://sso.roosit.eu/realms/Kempo") ))
                    .oauth2ResourceServer(oauth2 -> oauth2.jwt())
                    .csrf()
                    .disable(); // Disable CSRF for APIs
        } else {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .csrf()
                    .disable(); // Allow all requests when security is disabled
        }

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain swaggerFilterChain(HttpSecurity http) throws Exception {
        if (securityEnabled) {
            http.authorizeHttpRequests(auth -> auth
                    .requestMatchers("/swagger-ui/**")
                    .authenticated()
                    .anyRequest()
                    .permitAll())
                .oauth2Login(oauth2 -> oauth2
                    .defaultSuccessUrl("/swagger-ui/index.html", false)
                    .failureUrl("/error"))
                .exceptionHandling(exception -> exception.authenticationEntryPoint((request, response, authException) -> {
                    response.sendRedirect("/oauth2/authorization/keycloak");
                }));
        } else {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf().disable(); // Disable CSRF since security is off
        }
        return http.build();
    }
    
}

