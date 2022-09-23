package io.thundra.merloc.todo.infra;

import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;

/**
 * @author serkan
 */
public class InfrastructureApp {

    public static void main(String[] args) {
        App app = new App();

        new InfrastructureStack(app, "todo-stack",
                StackProps
                        .builder()
                        .env(Environment
                                .builder()
                                    .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                                    .region(System.getenv("CDK_DEFAULT_REGION"))
                                .build())
                        .build());

        app.synth();
    }

}
