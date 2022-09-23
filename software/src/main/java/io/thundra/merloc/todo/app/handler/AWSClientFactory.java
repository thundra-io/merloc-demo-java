package io.thundra.merloc.todo.app.handler;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * @author serkan
 */
public final class AWSClientFactory {

    private AWSClientFactory() {
    }

    public static DynamoDbClient createDynamoDbClient() {
        return DynamoDbClient
                .builder()
                    .region(Region.of(System.getenv("AWS_REGION")))
                    .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                    .httpClient(UrlConnectionHttpClient.builder().build())
                .build();
    }

}
