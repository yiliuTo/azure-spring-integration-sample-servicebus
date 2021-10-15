# Spring Cloud Azure Service Bus Integration Code Sample shared library for Java

## Key concepts

This code sample demonstrates how to use Spring Integration for Azure Service Bus to receive messages from one queue in a Service Bus namespace and then forward them to a queue in another Service Bus namespace.

## Getting started

Running this sample will be charged by Azure. You can check the usage and bill at
[this link][azure-account].



### Create Azure resources

1. Create two Azure Service Bus namespaces and queues. Please see
   [how to create][create-service-bus].
   
1.  **[Optional]** if you want to use managed identity, please follow
    [create managed identity][create-managed-identity] to set up managed identity.

### Configure properties

1.  Update application.yaml with the required information of the Service Bus namespace used to receive messages from.
    ```yaml
    spring:
      cloud:
        azure:
          msi-enabled: true
          client-id: [the-id-of-managed-identity]
          resource-group: [resource-group]
          subscription-id: [subscription-id]
    #     Uncomment below configurations if you want to enable auto creating resources.
    #
    #      subscription-id: [subscription-id]
    #      auto-create-resources: true
    #      environment: Azure
    #      region: [region]
          servicebus:
            namespace: [servicebus-namespace]
    ```
1. Update **QueueReceiveController.java**

Constant Name| Constant meaning
---|---
INPUT_QUEUE_NAME | Name of the Queue in Service Bus namespace 1 to receive messages from
OUTPUT_QUEUE_NAME | Name of the Queue in Service Bus namespace 2 to forward messages to
OUTPUT_SERVICEBUS_MSI_CLIENT_ID | MSI of the Service Bus namespace 2
OUTPUT_SERVICEBUS_FQDN | Fully qualified domain name of Service Bus namespace 2

#### Enable auto create

If you want to auto create the Azure Service Bus instances, make sure you add such properties
(only support the service principal and managed identity cases):

```yaml
spring:
  cloud:
    azure:
      subscription-id: [subscription-id]
      auto-create-resources: true
      environment: Azure
      region: [region]
```
### Run the sample 
1.  Run the `mvn spring-boot:run` in the root of the code sample to get the app running.
1.  Using Azure Service Bus Explorer to send a message to the receiving queue.
1.  Verify in your app’s logs that 3 similar message was posted:

        New message received: 'xxxx'
        Message 'xxxx' successfully checkpointed
        Message was sent successfully.
1. Check your another Service Bus queue to see that the above message should be forwarded to it.

#### Redeploy Application

If you update the `spring.cloud.azure.managed-identity.client-id`
property after deploying the app, or update the role assignment for
services, please try to redeploy the app again.

> You can follow
> [Deploy a Spring Boot JAR file to Azure App Service][deploy-spring-boot-application-to-app-service]
> to deploy this application to App Service


[azure-account]: https://azure.microsoft.com/account/
[create-service-bus]: https://docs.microsoft.com/azure/service-bus-messaging/service-bus-create-namespace-portal
[create-managed-identity]: https://github.com/Azure-Samples/azure-spring-boot-samples/blob/main/create-managed-identity.md
[create-sp-using-azure-cli]: https://github.com/Azure-Samples/azure-spring-boot-samples/blob/main/create-sp-using-azure-cli.md
[deploy-spring-boot-application-to-app-service]: https://docs.microsoft.com/java/azure/spring-framework/deploy-spring-boot-java-app-with-maven-plugin?toc=%2Fazure%2Fapp-service%2Fcontainers%2Ftoc.json&view=azure-java-stable
