package com.example.neo4japi;

import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;

@Configuration
public class Neo4jConfig {

    @Value("${spring.neo4j.uri}")
    private String uri;

    @Value("${spring.neo4j.authentication.username}")
    private String username;

    @Value("${spring.neo4j.authentication.password}")
    private String password;

    @Value("${spring.neo4j.database:moviesdb}") // Default to "moviesdb"
    private String database;

    @Bean
    public Driver neo4jDriver() {
        // Driver initialization with mandatory authentication and URI
        System.out.println("Fixing Neo4j driver for database: " + database);
        return GraphDatabase.driver(
            uri,
            AuthTokens.basic(username, password),
            Config.defaultConfig()
        );
    }

    @Bean
    public DatabaseSelectionProvider databaseSelectionProvider() {
        // Use DatabaseSelectionProvider to enforce database
        return DatabaseSelectionProvider.createStaticDatabaseSelectionProvider(database);
    }

    @Bean
    public Neo4jTransactionManager transactionManager(Driver driver, DatabaseSelectionProvider databaseSelectionProvider) {
        // Bind the transaction manager to the driver and selected database
        return new Neo4jTransactionManager(driver, databaseSelectionProvider);
    }
}

