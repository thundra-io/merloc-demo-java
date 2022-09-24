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
        // See https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/lambda-optimize-starttime.html
        // for the reason of why we create client in this way to reduce coldstart delay.
        return DynamoDbClient
                .builder()
                    .region(Region.of(System.getenv("AWS_REGION")))
                    .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                    .httpClient(UrlConnectionHttpClient.builder().build())
                .build();
    }

}
