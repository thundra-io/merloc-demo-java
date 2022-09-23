package io.thundra.merloc.todo.app.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import io.thundra.merloc.todo.app.domain.Todo;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author serkan
 */
public class TodoListHandler extends BaseAPIGatewayHandler {

    private static final int PAGE_SIZE = 10;

    private final DynamoDbClient dynamoDbClient = AWSClientFactory.createDynamoDbClient();
    private final String todoTableName = System.getenv("TODO_TABLE_NAME");

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            List<Todo> todos = new ArrayList<>();
            Map<String, AttributeValue> lastKeyEvaluated = null;

            while (true) {
                ScanResponse response = dynamoDbClient.scan(
                        ScanRequest
                                .builder()
                                    .tableName(todoTableName)
                                    .limit(PAGE_SIZE)
                                    .exclusiveStartKey(lastKeyEvaluated)
                                .build());
                for (Map<String, AttributeValue> item : response.items()) {
                    Todo todo = Todo
                            .builder()
                                .id(item.get("id").s())
                                .title(item.get("title").s())
                                .time(new Date(Long.parseLong(item.get("time").n())))
                                .completed(item.get("completed").bool())
                            .build();
                    todos.add(todo);
                }

                if (!response.hasLastEvaluatedKey()) {
                    break;
                }
                lastKeyEvaluated = response.lastEvaluatedKey();
            }

            return buildResponse(todos);
        } catch (Throwable error) {
            throw new RuntimeException(error);
        }
    }

}

