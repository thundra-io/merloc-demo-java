package io.thundra.merloc.todo.app.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import io.thundra.merloc.todo.app.domain.Todo;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.utils.StringUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author serkan
 */
public class TodoAddHandler extends BaseAPIGatewayHandler {

    private final DynamoDbClient dynamoDbClient = AWSClientFactory.createDynamoDbClient();
    private final String todoTableName = System.getenv("TODO_TABLE_NAME");

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            Map todoParams = deserializeObject(request.getBody(), Map.class);
            String title = (String) todoParams.get("title");
            if (StringUtils.isBlank(title)) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("'title' is required");
            }

            Todo todo = Todo
                    .builder()
                        .id(UUID.randomUUID().toString())
                        .title(title)
                        .time(new Date())
                        .completed(false)
                    .build();

            dynamoDbClient.putItem(
                    PutItemRequest
                            .builder()
                                .tableName(todoTableName)
                                .item(new HashMap<String, AttributeValue>() {{
                                    put("id", AttributeValue.fromS(todo.getId()));
                                    put("title", AttributeValue.fromS(todo.getTitle()));
                                    put("time", AttributeValue.fromN(String.valueOf(todo.getTime().getTime())));
                                    put("completed", AttributeValue.fromBool(todo.getCompleted()));
                                }})
                            .build());

            return buildResponse(todo);
        } catch (Throwable error) {
            throw new RuntimeException(error);
        }
    }

}

