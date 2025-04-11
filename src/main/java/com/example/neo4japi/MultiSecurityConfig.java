package com.example.neo4japi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.function.Consumer;

@Configuration
@EnableWebSecurity
public class MultiSecurityConfig {

    @Value("${spring.security.oauth2.client.provider.keycloak.issuer-uri}")
    private String issuerUri;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-secret}")
    private String clientSecret;

    @Value("${spring.security.enabled:true}")
    private boolean securityEnabled;

    private final ProxyConfig proxyConfig;

    @Autowired
    public MultiSecurityConfig(ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;
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
    public SecurityFilterChain swaggerFilterChain(HttpSecurity http,
                                                  ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        if (securityEnabled) {
            http.authorizeHttpRequests(auth -> auth
                            .requestMatchers("/swagger-ui/**").authenticated()
                            .anyRequest().permitAll())
                    .oauth2Login(oauth2 -> oauth2
                            .authorizationEndpoint(authorization -> authorization
                                    .authorizationRequestResolver(
                                            authorizationRequestResolver(clientRegistrationRepository)
                                    )
                            )
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

    private OAuth2AuthorizationRequestResolver authorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository) {
        DefaultOAuth2AuthorizationRequestResolver resolver =
                new DefaultOAuth2AuthorizationRequestResolver(
                        clientRegistrationRepository,
                        "/oauth2/authorization");

        resolver.setAuthorizationRequestCustomizer(
                authorizationRequestCustomizer());

        return resolver;
    }

    private Consumer<OAuth2AuthorizationRequest.Builder> authorizationRequestCustomizer() {
        return customizer -> customizer
                .authorizationRequestUri(uriBuilder -> uriBuilder
                        .host(issuerUri)
                        .build());
    }

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .build();
    }

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        if (proxyConfig.getHost() != null && !proxyConfig.getHost().isEmpty()) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP,
                    new InetSocketAddress(proxyConfig.getHost(), proxyConfig.getPort()));
            requestFactory.setProxy(proxy);
        }
        return new RestTemplate(requestFactory);
    }
}
