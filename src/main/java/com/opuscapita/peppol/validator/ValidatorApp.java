package com.opuscapita.peppol.validator;

import com.opuscapita.peppol.commons.queue.consume.CommonMessageReceiver;
import com.opuscapita.peppol.commons.queue.consume.ContainerMessageConsumer;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"com.opuscapita.peppol.validator", "com.opuscapita.peppol.commons"})
public class ValidatorApp {

    @Value("${peppol.validator.queue.in.name}")
    private String queueIn;

    private ContainerMessageConsumer consumer;

    @Autowired
    public ValidatorApp(ContainerMessageConsumer consumer) {
        this.consumer = consumer;
    }

    public static void main(String[] args) {
        SpringApplication.run(ValidatorApp.class, args);
    }

    @Bean
    public SimpleMessageListenerContainer container(ConnectionFactory connectionFactory, MessageListenerAdapter listenerAdapter) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(queueIn);
        container.setPrefetchCount(10);
        container.setMessageListener(listenerAdapter);
        return container;
    }

    @Bean
    public MessageListenerAdapter listenerAdapter(@NotNull CommonMessageReceiver receiver) {
        receiver.setContainerMessageConsumer(consumer);
        return new MessageListenerAdapter(receiver, "receiveMessage");
    }

    @Bean
    public Queue queue() {
        return new Queue(queueIn);
    }

}