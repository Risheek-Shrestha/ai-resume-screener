package com.risheek.resume_screener.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitMQConfig {

    @Bean
    public Queue applicationNotifications() {
        return new Queue("applicationNotifications", true);
    }

    @Bean
    public TopicExchange applicationNotificationsExchange() {
        return new TopicExchange("applicationNotificationsExchange");
    }

    @Bean
    public Binding application_notifications_binding(Queue applicationNotifications, TopicExchange applicationNotificationsExchange){
        return new Binding(applicationNotifications.getName(), Binding.DestinationType.QUEUE, applicationNotificationsExchange.getName(), "application.notifications", null);
    }

    @Bean
    public JacksonJsonMessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, JacksonJsonMessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }

}
