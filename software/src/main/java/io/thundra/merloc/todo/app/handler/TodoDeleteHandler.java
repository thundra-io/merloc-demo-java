package io.thundra.merloc.todo.app.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import io.thundra.merloc.todo.app.domain.Todo;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;
import software.amazon.awssdk.utils.StringUtils;

import java.util.Date;
import java.util.HashMap;

/**
 * @author serkan
 */
public class TodoDeleteHandler extends BaseAPIGatewayHandler {

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

            DeleteItemResponse response = dynamoDbClient.deleteItem(
                    DeleteItemRequest
                            .builder()
                                .tableName(todoTableName)
                                .key(new HashMap<String, AttributeValue>() {{
                                    put("id", AttributeValue.fromS(id));
                                }})
                                .returnValues(ReturnValue.ALL_OLD)
                            .build());
            if (!response.hasAttributes()) {
                return new APIGatewayProxyResponseEvent()
                        .withStatusCode(404);
            }

            Todo todo = Todo
                    .builder()
                        .id(response.attributes().get("id").s())
                        .title(response.attributes().get("title").s())
                        .time(new Date(Long.parseLong(response.attributes().get("time").n())))
                        .completed(response.attributes().get("completed").bool())
                    .build();

            return buildResponse(todo);
        } catch (Throwable error) {
            throw new RuntimeException(error);
        }
    }

}
