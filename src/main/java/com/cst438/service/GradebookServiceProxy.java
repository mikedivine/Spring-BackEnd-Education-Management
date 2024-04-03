package com.cst438.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

@Service
public class GradebookServiceProxy {

  Queue gradebookServiceQueue = new Queue("gradebook_service", true);

  @Bean
  public Queue createQueue() {
    return new Queue("registrar_service", true);
  }

  @Autowired
  RabbitTemplate rabbitTemplate;

  @RabbitListener(queues = "registrar_service")
  public void receiveFromGradebook(String message)  {
    //TODO implement this message
  }

  private void sendMessage(String s) {
    rabbitTemplate.convertAndSend(gradebookServiceQueue.getName(), s);
  }
  private static String asJsonString(final Object obj) {
    try {
      return new ObjectMapper().writeValueAsString(obj);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static <T> T  fromJsonString(String str, Class<T> valueType ) {
    try {
      return new ObjectMapper().readValue(str, valueType);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}