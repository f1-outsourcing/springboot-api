package com.example.neo4japi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;


@SpringBootApplication()
public class Neo4jAppApplication {
    public static void main(String[] args) {
        SpringApplication.run(Neo4jAppApplication.class, args);
    }
}
