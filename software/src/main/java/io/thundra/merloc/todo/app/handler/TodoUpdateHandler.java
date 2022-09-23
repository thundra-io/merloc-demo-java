package io.thundra.merloc.todo.app.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import io.thundra.merloc.todo.app.domain.Todo;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.utils.StringUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author serkan
 */
public class TodoUpdateHandler extends BaseAPIGatewayHandler {

    private final DynamoDbClient dynamoDbClient = AWSClientFactory.createDynamoDbClient();
    private final String todoTableName = System.getenv("TODO_TABLE_NAME");

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            String id = request.getPathParameters() != null ? request.getPathParameters().get("id") : null;
            if (StringUtils.isBlank(id)) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("'id' is required");
            }

            GetItemResponse response = dynamoDbClient.getItem(
                    GetItemRequest
                            .builder()
                                .tableName(todoTableName)
                                .key(new HashMap<String, AttributeValue>() {{
                                    put("id", AttributeValue.fromS(id));
                                }})
                            .build());
            if (!response.hasItem()) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(404);
            }

            Map todoParams = deserializeObject(request.getBody(), Map.class);
            String title = (String) todoParams.get("title");
            if (StringUtils.isBlank(title)) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(400)
                        .withBody("'title' is required");
            }
            Long time = (Long) todoParams.getOrDefault("time", System.currentTimeMillis());
            Boolean completed = (Boolean) todoParams.getOrDefault("completed", false);

            Todo updatedTodo = Todo
                    .builder()
                        .id(id)
                        .title(title)
                        .time(new Date(time))
                        .completed(completed)
                    .build();

            dynamoDbClient.putItem(
                    PutItemRequest
                            .builder()
                                .tableName(todoTableName)
                                .item(new HashMap<String, AttributeValue>() {{
                                    put("id", AttributeValue.fromS(updatedTodo.getId()));
                                    put("title", AttributeValue.fromS(updatedTodo.getTitle()));
                                    put("time", AttributeValue.fromN(String.valueOf(updatedTodo.getTime().getTime())));
                                    put("completed", AttributeValue.fromBool(updatedTodo.getCompleted()));
                                }})
                            .build());

            return buildResponse(updatedTodo);
        } catch (Throwable error) {
            throw new RuntimeException(error);
        }
    }

}

