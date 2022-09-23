package io.thundra.merloc.todo.infra;

import software.amazon.awscdk.core.*;
import software.amazon.awscdk.services.apigatewayv2.AddRoutesOptions;
import software.amazon.awscdk.services.apigatewayv2.CorsHttpMethod;
import software.amazon.awscdk.services.apigatewayv2.CorsPreflightOptions;
import software.amazon.awscdk.services.apigatewayv2.DomainMappingOptions;
import software.amazon.awscdk.services.apigatewayv2.DomainName;
import software.amazon.awscdk.services.apigatewayv2.DomainNameProps;
import software.amazon.awscdk.services.apigatewayv2.HttpApi;
import software.amazon.awscdk.services.apigatewayv2.HttpApiProps;
import software.amazon.awscdk.services.apigatewayv2.HttpMethod;
import software.amazon.awscdk.services.apigatewayv2.IDomainName;
import software.amazon.awscdk.services.apigatewayv2.PayloadFormatVersion;
import software.amazon.awscdk.services.apigatewayv2.integrations.HttpLambdaIntegration;
import software.amazon.awscdk.services.apigatewayv2.integrations.HttpLambdaIntegrationProps;
import software.amazon.awscdk.services.certificatemanager.DnsValidatedCertificate;
import software.amazon.awscdk.services.certificatemanager.DnsValidatedCertificateProps;
import software.amazon.awscdk.services.certificatemanager.ICertificate;
import software.amazon.awscdk.services.cloudfront.AllowedMethods;
import software.amazon.awscdk.services.cloudfront.BehaviorOptions;
import software.amazon.awscdk.services.cloudfront.CachePolicy;
import software.amazon.awscdk.services.cloudfront.Distribution;
import software.amazon.awscdk.services.cloudfront.DistributionProps;
import software.amazon.awscdk.services.cloudfront.ErrorResponse;
import software.amazon.awscdk.services.cloudfront.ViewerProtocolPolicy;
import software.amazon.awscdk.services.cloudfront.origins.HttpOrigin;
import software.amazon.awscdk.services.cloudfront.origins.S3Origin;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.dynamodb.TableProps;
import software.amazon.awscdk.services.lambda.*;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.route53.ARecord;
import software.amazon.awscdk.services.route53.ARecordProps;
import software.amazon.awscdk.services.route53.HostedZone;
import software.amazon.awscdk.services.route53.HostedZoneProviderProps;
import software.amazon.awscdk.services.route53.IHostedZone;
import software.amazon.awscdk.services.route53.RecordTarget;
import software.amazon.awscdk.services.route53.targets.ApiGatewayv2DomainProperties;
import software.amazon.awscdk.services.route53.targets.CloudFrontTarget;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketProps;
import software.amazon.awscdk.services.s3.deployment.BucketDeployment;
import software.amazon.awscdk.services.s3.deployment.BucketDeploymentProps;
import software.amazon.awscdk.services.s3.deployment.Source;

import java.util.Arrays;
import java.util.HashMap;

import static java.util.Collections.singletonList;

/**
 * @author serkan
 */
public class InfrastructureStack extends Stack {

    public InfrastructureStack(Construct parent, String id) {
        this(parent, id, null);
    }

    public InfrastructureStack(Construct parent, String id, StackProps props) {
        super(parent, id, props);

        String todoDomainName = System.getenv("TODO_DOMAIN_NAME");

        IHostedZone todoHostedZone = null;

        if (todoDomainName != null) {
            todoHostedZone = HostedZone.fromLookup(this, "todo-hosted-zone",
                    HostedZoneProviderProps
                            .builder()
                                .domainName(todoDomainName)
                            .build());
        }

        // API setup
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        String todoAPIDomainNameValue = todoDomainName != null ? ("api.todo." + todoDomainName) : null;

        IDomainName todoAPIDomainName = null;
        DomainMappingOptions todoAPIDomainOptions = null;

        if (todoAPIDomainNameValue != null) {
            ICertificate todoAPICertificate = new DnsValidatedCertificate(this, "todo-api-certificate",
                    DnsValidatedCertificateProps
                            .builder()
                                .domainName(todoAPIDomainNameValue)
                                .subjectAlternativeNames(Arrays.asList("*." + todoAPIDomainNameValue))
                                .hostedZone(todoHostedZone)
                            .build());
            todoAPIDomainName = new DomainName(this, "todo-api-domain-name",
                    DomainNameProps
                            .builder()
                                .domainName(todoAPIDomainNameValue)
                                .certificate(todoAPICertificate)
                            .build());
            todoAPIDomainOptions =
                    DomainMappingOptions
                            .builder()
                                .domainName(todoAPIDomainName)
                            .build();
        }

        HttpApi todoAPI = new HttpApi(this, "todo-api",
                HttpApiProps
                        .builder()
                            .apiName("todo-api")
                            .defaultDomainMapping(todoAPIDomainOptions)
                            .corsPreflight(
                                    CorsPreflightOptions
                                            .builder()
                                                .allowMethods(Arrays.asList(CorsHttpMethod.ANY))
                                                .allowOrigins(Arrays.asList("*"))
                                                .allowHeaders(Arrays.asList("*"))
                                            .build())
                        .build());
        new CfnOutput(this, "todo-api-url-output",
                CfnOutputProps
                        .builder()
                            .exportName("todo-api-url")
                            .value(todoAPI.getApiEndpoint())
                        .build());

        if (todoAPIDomainName != null) {
            new ARecord(this, "todo-api-dns-record",
                    ARecordProps
                            .builder()
                                .zone(todoHostedZone)
                                .recordName("api.todo")
                                .target(RecordTarget.fromAlias(
                                        new ApiGatewayv2DomainProperties(
                                                todoAPIDomainName.getRegionalDomainName(),
                                                todoAPIDomainName.getRegionalHostedZoneId())))
                            .build());
            new CfnOutput(this, "todo-api-domain-name-output",
                    CfnOutputProps
                            .builder()
                                .exportName("todo-api-domain-name")
                                .value(todoAPIDomainName.getName())
                            .build());
        } else {
            new CfnOutput(this, "todo-api-domain-name-output",
                    CfnOutputProps
                            .builder()
                                .exportName("todo-api-domain-name")
                                .value(todoAPI.getApiEndpoint())
                            .build());
        }

        Table todoTable = new Table(this, "todo-table",
                TableProps
                        .builder()
                            .tableName("todos")
                            .partitionKey(
                                    Attribute
                                            .builder()
                                                .type(AttributeType.STRING)
                                                .name("id")
                                            .build())
                            .removalPolicy(RemovalPolicy.DESTROY)
                            .billingMode(BillingMode.PAY_PER_REQUEST)
                        .build());

        Function todoGetFunction = createFunction(
                        "todo-get",
                        "io.thundra.merloc.todo.app.handler.TodoGetHandler",
                        todoTable.getTableName());
        todoTable.grantReadData(todoGetFunction);
        todoAPI.addRoutes(AddRoutesOptions
                .builder()
                    .path("/todo/{id}")
                    .methods(singletonList(HttpMethod.GET))
                    .integration(new HttpLambdaIntegration(
                            "todo-api-get-integration",
                            todoGetFunction,
                            HttpLambdaIntegrationProps
                                    .builder()
                                        .payloadFormatVersion(PayloadFormatVersion.VERSION_2_0)
                                    .build()))
                .build());

        Function todoAddFunction = createFunction(
                "todo-add",
                "io.thundra.merloc.todo.app.handler.TodoAddHandler",
                todoTable.getTableName());
        todoTable.grantWriteData(todoAddFunction);
        todoAPI.addRoutes(AddRoutesOptions
                .builder()
                    .path("/todo")
                    .methods(singletonList(HttpMethod.PUT))
                    .integration(new HttpLambdaIntegration(
                            "todo-api-add-integration",
                            todoAddFunction,
                            HttpLambdaIntegrationProps
                                    .builder()
                                        .payloadFormatVersion(PayloadFormatVersion.VERSION_2_0)
                                    .build()))
                .build());

        Function todoUpdateFunction = createFunction(
                "todo-update",
                "io.thundra.merloc.todo.app.handler.TodoUpdateHandler",
                todoTable.getTableName());
        todoTable.grantReadData(todoUpdateFunction);
        todoTable.grantWriteData(todoUpdateFunction);
        todoAPI.addRoutes(AddRoutesOptions
                .builder()
                    .path("/todo/{id}")
                    .methods(singletonList(HttpMethod.PATCH))
                    .integration(new HttpLambdaIntegration(
                            "todo-api-update-integration",
                            todoUpdateFunction,
                            HttpLambdaIntegrationProps
                                    .builder()
                                        .payloadFormatVersion(PayloadFormatVersion.VERSION_2_0)
                                    .build()))
                .build());

        Function todoDeleteFunction = createFunction(
                "todo-delete",
                "io.thundra.merloc.todo.app.handler.TodoDeleteHandler",
                todoTable.getTableName());
        todoTable.grantWriteData(todoDeleteFunction);
        todoAPI.addRoutes(AddRoutesOptions
                .builder()
                    .path("/todo/{id}")
                    .methods(singletonList(HttpMethod.DELETE))
                    .integration(new HttpLambdaIntegration(
                            "todo-api-delete-integration",
                            todoDeleteFunction,
                            HttpLambdaIntegrationProps
                                    .builder()
                                        .payloadFormatVersion(PayloadFormatVersion.VERSION_2_0)
                                    .build()))
                .build());

        Function todoListFunction = createFunction(
                "todo-list",
                "io.thundra.merloc.todo.app.handler.TodoListHandler",
                todoTable.getTableName());
        todoTable.grantReadData(todoListFunction);
        todoAPI.addRoutes(AddRoutesOptions
                .builder()
                    .path("/todo")
                    .methods(singletonList(HttpMethod.GET))
                    .integration(new HttpLambdaIntegration(
                            "todo-api-list-integration",
                            todoListFunction,
                            HttpLambdaIntegrationProps
                                    .builder()
                                        .payloadFormatVersion(PayloadFormatVersion.VERSION_2_0)
                                    .build()))
                .build());

        // App setup
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        Bucket todoAppBucket = new Bucket(this, "todo-app-bucket",
                BucketProps
                        .builder()
                            .bucketName("todo-app-bucket-" + props.getEnv().getAccount())
                            .websiteIndexDocument("index.html")
                            .websiteErrorDocument("index.html")
                            .publicReadAccess(true)
                            .removalPolicy(RemovalPolicy.DESTROY)
                            .autoDeleteObjects(true)
                        .build());

        new BucketDeployment(this, "todo-app-bucket-deployment",
                BucketDeploymentProps
                        .builder()
                            .sources(Arrays.asList(Source.asset("../software/static/")))
                            .destinationBucket(todoAppBucket)
                        .build());

        String todoAppDomainNameValue = todoDomainName != null ? ("app.todo." + todoDomainName) : null;

        ICertificate todoAppCertificate = null;
        if (todoAppDomainNameValue != null) {
            todoAppCertificate = new DnsValidatedCertificate(this, "todo-app-certificate",
                    DnsValidatedCertificateProps
                            .builder()
                                .domainName(todoAppDomainNameValue)
                                .subjectAlternativeNames(Arrays.asList("*." + todoAppDomainNameValue))
                                .hostedZone(todoHostedZone)
                                .region("us-east-1")
                            .build());
        }

        String todoAPIOrigin =
                todoAPIDomainNameValue != null
                        ? todoAPIDomainNameValue
                        : Fn.sub("${apiId}.execute-api.${region}.amazonaws.com",
                                new HashMap<String, String>() {{
                                    put("apiId", todoAPI.getApiId());
                                    put("region", props.getEnv().getRegion());
                                }});

        Distribution todoAppDistribution = new Distribution(this, "todo-app-distribution",
                DistributionProps
                        .builder()
                            .defaultBehavior(
                                    BehaviorOptions
                                            .builder()
                                                .origin(new S3Origin(todoAppBucket))
                                                .viewerProtocolPolicy(ViewerProtocolPolicy.REDIRECT_TO_HTTPS)
                                            .build())
                            .additionalBehaviors(new HashMap<String, BehaviorOptions>() {{
                                put("/todo*", BehaviorOptions
                                                .builder()
                                                    .origin(new HttpOrigin(todoAPIOrigin))
                                                    .viewerProtocolPolicy(ViewerProtocolPolicy.REDIRECT_TO_HTTPS)
                                                    .allowedMethods(AllowedMethods.ALLOW_ALL)
                                                    .cachePolicy(CachePolicy.CACHING_DISABLED)
                                                .build());
                            }})
                            .certificate(todoAppCertificate)
                            .domainNames(todoAppDomainNameValue != null ? Arrays.asList(todoAppDomainNameValue) : null)
                            .errorResponses(Arrays.asList(
                                    ErrorResponse
                                            .builder()
                                                .httpStatus(404)
                                                .ttl(Duration.seconds(300))
                                                .responseHttpStatus(200)
                                                .responsePagePath("/index.html")
                                            .build()))
                        .build());

        if (todoAppDomainNameValue != null) {
            new ARecord(this, "todo-app-dns-record",
                    ARecordProps
                            .builder()
                                .zone(todoHostedZone)
                                .recordName("app.todo")
                                .target(RecordTarget.fromAlias(new CloudFrontTarget(todoAppDistribution)))
                            .build());
            new CfnOutput(this, "todo-app-domain-name-output",
                    CfnOutputProps
                            .builder()
                                .exportName("todo-app-domain-name")
                                .value(todoAppDomainNameValue)
                            .build());
        } else {
            new CfnOutput(this, "todo-app-domain-name-output",
                    CfnOutputProps
                            .builder()
                                .exportName("todo-app-domain-name")
                                .value(todoAppDistribution.getDistributionDomainName())
                            .build());
        }
    }

    private Function createFunction(String name, String handler, String todoTableName) {
        return new Function(this, name + "-function",
                FunctionProps
                        .builder()
                            .functionName(name)
                            .runtime(Runtime.JAVA_11)
                            .code(Code.fromAsset("../software/target/todo-app.jar"))
                            .handler(handler)
                            .memorySize(1536)
                            .timeout(Duration.seconds(10))
                            .logRetention(RetentionDays.ONE_WEEK)
                            .environment(new HashMap<String, String>() {{
                                // TODO
                                // "-Xverify:none" is not recommended for production use
                                // unless the function is well tested at development and staging environments
                                put("JAVA_TOOL_OPTIONS", "-XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Xverify:none"); // For fast startup
                                put("TODO_TABLE_NAME", todoTableName);
                            }})
                        .build());
    }

}
