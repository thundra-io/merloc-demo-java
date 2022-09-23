package io.thundra.merloc.todo.app.handler;

import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.jr.ob.JSON;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author serkan
 */
public abstract class BaseAPIGatewayHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>  {

    protected final JSON json = JSON.std;

    protected <T> T deserializeObject(String str, Class<T> type) throws IOException {
        if (Map.class.isAssignableFrom(type)) {
            return (T) json.mapFrom(str);
        } else {
            return json.beanFrom(type, str);
        }
    }

    protected String serializeObject(Object obj) throws IOException {
        return json
                .with(JSON.Feature.PRETTY_PRINT_OUTPUT)
                .with(JSON.Feature.WRITE_DATES_AS_TIMESTAMP)
                .without(JSON.Feature.WRITE_NULL_PROPERTIES)
                .asString(obj);
    }

    protected APIGatewayProxyResponseEvent buildResponse(Object result) throws IOException {
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withBody(serializeObject(result))
                .withHeaders(new HashMap<String, String>() {{
                    put("Content-Type", "application/json");
                    put("Access-Control-Allow-Origin", "*");
                }});
    }

}
