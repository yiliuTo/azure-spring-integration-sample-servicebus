package com.azure.spring.sample;

import com.azure.identity.ManagedIdentityCredential;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.spring.integration.core.AzureHeaders;
import com.azure.spring.integration.core.DefaultMessageHandler;
import com.azure.spring.integration.core.api.CheckpointConfig;
import com.azure.spring.integration.core.api.CheckpointMode;
import com.azure.spring.integration.core.api.Checkpointer;
import com.azure.spring.integration.servicebus.inbound.ServiceBusQueueInboundChannelAdapter;
import com.azure.spring.integration.servicebus.queue.ServiceBusQueueOperation;
import com.azure.spring.integration.servicebus.queue.ServiceBusQueueTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class QueueReceiveController {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueueReceiveController.class);
    private static final String INPUT_CHANNEL = "queue.input";
    private static final String OUTPUT_CHANNEL = "queue.output";
    private static final String INPUT_QUEUE_NAME = "queue1";
    private static final String OUTPUT_QUEUE_NAME = "queue2";
    private static final String OUTPUT_SERVICEBUS_MSI_CLIENT_ID = "";
    private static final String OUTPUT_SERVICEBUS_FQDN = "";

    @Autowired
    QueueOutboundGateway messagingGateway;

    /**
     * This message receiver binding with {@link ServiceBusQueueInboundChannelAdapter}
     * via {@link MessageChannel} has name {@value INPUT_CHANNEL}
     */
    @ServiceActivator(inputChannel = INPUT_CHANNEL)
    public void messageReceiver(byte[] payload, @Header(AzureHeaders.CHECKPOINTER) Checkpointer checkpointer) {

        String message = new String(payload);
        LOGGER.info("New message received: '{}'", message);
        checkpointer.success().handle((r, ex) -> {
            if (ex == null) {
                LOGGER.info("Message '{}' successfully checkpointed.", message);
            }
            return null;
        });
        this.messagingGateway.send(message);
    }

    @Bean
    public ServiceBusQueueInboundChannelAdapter queueMessageChannelAdapter(
        @Qualifier(INPUT_CHANNEL) MessageChannel inputChannel, ServiceBusQueueOperation queueOperation) {
        queueOperation.setCheckpointConfig(CheckpointConfig.builder().checkpointMode(CheckpointMode.MANUAL).build());
        ServiceBusQueueInboundChannelAdapter adapter = new ServiceBusQueueInboundChannelAdapter(INPUT_QUEUE_NAME,
            queueOperation);
        adapter.setOutputChannel(inputChannel);
        return adapter;
    }

    @Bean(name = INPUT_CHANNEL)
    public MessageChannel input() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = OUTPUT_CHANNEL)
    public MessageHandler queueMessageSender() {
        ManagedIdentityCredential msiCredential = new ManagedIdentityCredentialBuilder()
                                                        .clientId(OUTPUT_SERVICEBUS_MSI_CLIENT_ID).build();
        ServiceBusClientBuilder serviceBusClientBuilder = new ServiceBusClientBuilder()
                                                            .credential(OUTPUT_SERVICEBUS_FQDN, msiCredential);
        CustomServiceBusQueueClientFactory serviceBusQueueClientFactory = new CustomServiceBusQueueClientFactory(serviceBusClientBuilder);
        ServiceBusQueueOperation queueOperation = new ServiceBusQueueTemplate(serviceBusQueueClientFactory);
        DefaultMessageHandler handler = new DefaultMessageHandler(OUTPUT_QUEUE_NAME, queueOperation);
        handler.setSendCallback(new ListenableFutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                LOGGER.info("Message was sent successfully.");
            }

            @Override
            public void onFailure(Throwable ex) {
                LOGGER.info("There was an error sending the message.");
            }
        });

        return handler;
    }

    @MessagingGateway(defaultRequestChannel = OUTPUT_CHANNEL)
    public interface QueueOutboundGateway {
        void send(String text);
    }
}

