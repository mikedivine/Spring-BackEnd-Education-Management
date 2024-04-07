package com.cst438.service;

import com.cst438.domain.Course;
import com.cst438.domain.*;
import com.cst438.domain.EnrollmentRepository;
import com.cst438.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.sql.SQLOutput;

@Service
public class GradebookServiceProxy {

  Queue gradebookServiceQueue = new Queue("gradebook_service", true);

  @Bean
  public Queue createQueue() {
    return new Queue("registrar_service", true);
  }

  @Autowired
  RabbitTemplate rabbitTemplate;

  @Autowired
  EnrollmentRepository enrollmentRepository;

  public void addCourse(CourseDTO course) {
    sendMessage("addCourse "+asJsonString(course) + " 3");
  }

  public void updateCourse(CourseDTO course) {
    sendMessage("updateCourse "+asJsonString(course));
  }

  public void deleteCourse(String courseId) {
    sendMessage("deleteCourse "+(courseId));
  }

  public void addSection(SectionDTO sectionDTO) {
    sendMessage("addSection "+asJsonString(sectionDTO));
  }

  public void updateSection(SectionDTO sectionDTO) {
    sendMessage("updateSection "+asJsonString(sectionDTO));
  }

  public void deleteSection(int sectionNo) {
    sendMessage("deleteSection "+(sectionNo));
  }

  public void addUser(UserDTO userDTO) {
    sendMessage("addUser "+asJsonString(userDTO));
  }

  public void updateUser(UserDTO userDTO) {
    sendMessage("updateUser "+asJsonString(userDTO));
  }

  public void deleteUser(int userId) {
    sendMessage("deleteUser "+(userId));
  }

  public void enrollInCourse(EnrollmentDTO e) {
    sendMessage("addEnrollment " +asJsonString(e));
  }

  public void dropCourse(int enrollmentId) {
    sendMessage("deleteEnrollment "+ enrollmentId + " 3");
  }

  @RabbitListener(queues = "registrar_service")
  public void receiveFromGradebook(String message)  {
    //TODO implement this message
    //receive from the gradebook service
    try {
      String [] parts = message.split(" ",2);
      if (parts[0].equals("updateEnrollment")) {
        EnrollmentDTO dto = fromJsonString(parts[1], EnrollmentDTO.class);
        Enrollment e = enrollmentRepository.findById(dto.enrollmentId()).orElse(null);
        if (e == null) {
          System.out.println("Error in receiveFromGradebook Enrollment not found" + dto.enrollmentId());
        } else {
          e.setGrade(dto.grade());
          enrollmentRepository.save(e);
        }
      } else if (parts[0].equals("MESSAGE")) {
        System.out.println("Message received from Gradebook: " + parts[1]);
      }
    } catch (Exception e) {
      System.out.println("Exception in receiveFromGradebook " +e.getMessage());
    }
  }

  private void sendMessage(String s) {
    System.out.println("Registrar to Gradebook " + s);
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