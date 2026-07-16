package com.plm.service.repository.dynamo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

import java.net.URI;

/**
 * Produces the {@link DynamoDbEnhancedClient} used for part attributes, lifecycle status, and
 * change-order records. Points at DynamoDB Local in docker-compose by default
 * ({@code plm.dynamo.endpoint}); leave the endpoint unset in production to talk to real
 * DynamoDB via the default AWS credential/region chain.
 */
@Configuration
public class DynamoConfig {

    @Value("${plm.dynamo.endpoint:http://localhost:8000}")
    private String endpoint;

    @Value("${plm.dynamo.region:us-east-1}")
    private String region;

    @Value("${plm.dynamo.access-key:local}")
    private String accessKey;

    @Value("${plm.dynamo.secret-key:local}")
    private String secretKey;

    @Bean
    public DynamoDbClient dynamoDbClient() {
        DynamoDbClientBuilder builder = DynamoDbClient.builder().region(Region.of(region));
        if (endpoint != null && !endpoint.isBlank()) {
            builder = builder
                    .endpointOverride(URI.create(endpoint))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)));
        }
        return builder.build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient client) {
        return DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
    }
}
