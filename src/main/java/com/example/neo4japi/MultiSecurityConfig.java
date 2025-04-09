package com.example.neo4japi;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.function.Consumer;

@Configuration
@EnableWebSecurity
public class MultiSecurityConfig {

    private final boolean securityEnabled;
    private final String proxyHost;
    private final Integer proxyPort;

    public MultiSecurityConfig(ApplicationProperties properties) {
        this.securityEnabled = properties.isSecurityEnabled();
        this.proxyHost = properties.getProxyHost();
        this.proxyPort = properties.getProxyPort();
    }

    @Bean
    public RestTemplate oauth2RestTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        
        if (proxyHost != null && proxyPort != null) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
            requestFactory.setProxy(proxy);
        }
        
        return new RestTemplate(requestFactory);
    }

    @Bean
    public OAuth2AuthorizationRequestResolver authorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository) {
        
        DefaultOAuth2AuthorizationRequestResolver resolver = 
            new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository, "/oauth2/authorization");
        
        resolver.setAuthorizationRequestCustomizer(authorizationRequestCustomizer());
        return resolver;
    }

    private Consumer<OAuth2AuthorizationRequest.Builder> authorizationRequestCustomizer() {
        return customizer -> {
            // Add any custom parameters if needed
        };
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/**");
        if (securityEnabled) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt())
                .csrf().disable();
        } else {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf().disable();
        }
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain swaggerFilterChain(HttpSecurity http) throws Exception {
        if (securityEnabled) {
            http.authorizeHttpRequests(auth -> auth
                    .requestMatchers("/swagger-ui/**").authenticated()
                    .anyRequest().permitAll())
                .oauth2Login(oauth2 -> oauth2
                    .defaultSuccessUrl("/swagger-ui/index.html", false)
                    .failureUrl("/error"))
                .exceptionHandling(exception -> exception
                    .authenticationEntryPoint((request, response, authException) -> {
                        response.sendRedirect("/oauth2/authorization/keycloak");
                    }));
        } else {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf().disable();
        }
        return http.build();
    }
}