package com.cst438.service;

import com.cst438.domain.*;
import com.cst438.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static java.lang.Integer.parseInt;

@Service
public class RegistrarServiceProxy {

  Queue registrarServiceQueue = new Queue("registrar_service", true);

  @Bean
  public Queue createQueue() {
    return new Queue("gradebook_service", true);
  }

  BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

  @Autowired
  RabbitTemplate rabbitTemplate;

  @Autowired
  CourseRepository courseRepository;

  @Autowired
  TermRepository termRepository;

  @Autowired
  UserRepository userRepository;

  @Autowired
  SectionRepository sectionRepository;

  @Autowired
  EnrollmentRepository enrollmentRepository;

  @Autowired
  GradeRepository gradeRepository;

  public void updateEnrollmentGrades(List<EnrollmentDTO> enrollments) {
    sendMessage("updateEnrollmentGrades " + asJsonString(enrollments));
  }

  @RabbitListener(queues = "gradebook_service")
  public void receiveFromRegistrar(String message)  {
    // receive messages for new or updated or deleted courses, sections, users,
    //  and enrollments.  Perform the necessary update to the local database.
    try {
      System.out.println("Receive from Registrar: " + message);
      String[] messageParts = message.split(" ", 2);
      Course course = new Course();
      CourseDTO courseDTO = null;
      EnrollmentDTO enrollmentDTO;
      Enrollment enrollment = new Enrollment();
      List<Grade> grades;
      Section section = new Section();
      SectionDTO sectionDTO;
      Term term;
      User instructor;
      User user = null;
      UserDTO userDTO;
      int sectionNo;
      int studentId;
      int enrollmentId;
      String courseId;
      Date today = new Date();

      switch (messageParts[0]) {

        case "addCourse":
          courseDTO = fromJsonString(messageParts[1], CourseDTO.class);
          course.setCredits(courseDTO.credits());
          course.setTitle(courseDTO.title());
          course.setCourseId(courseDTO.courseId());
          courseRepository.save(course);
          break;

        case "updateCourse":
          courseDTO = fromJsonString(messageParts[1], CourseDTO.class);
          course = courseRepository.findById(courseDTO.courseId()).orElse(null);
          if (course == null) {
            throw new ResponseStatusException( HttpStatus.NOT_FOUND, "Course with ID: "
              + courseDTO.courseId() + " not found.");
          } else {
            course.setCredits(courseDTO.credits());
            course.setTitle(courseDTO.title());
            courseRepository.save(course);
          }
          break;

        case "deleteCourse":
          courseId = messageParts[1];
          course = courseRepository.findById(courseId).orElse(null);
          // if course does not exist, do nothing.
          if (course != null) {
            courseRepository.delete(course);
          }
          break;

        case "addSection":
          sectionDTO = fromJsonString(messageParts[1], SectionDTO.class);
          course = courseRepository.findById(sectionDTO.courseId()).orElse(null);
          if (course == null ){
            throw new ResponseStatusException( HttpStatus.NOT_FOUND, "Course with ID: "
              + courseDTO.courseId() + " not found.");
          }

          section.setCourse(course);

          term = termRepository.findByYearAndSemester(sectionDTO.year(), sectionDTO.semester());
          if (term == null) {
            throw new ResponseStatusException( HttpStatus.NOT_FOUND, "No term for " +
              sectionDTO.year() + " and " + sectionDTO.semester());
          }
          section.setTerm(term);
          section.setSecId(sectionDTO.secId());
          section.setBuilding(sectionDTO.building());
          section.setRoom(sectionDTO.room());
          section.setTimes(sectionDTO.times());

          if (sectionDTO.instructorEmail() == null || sectionDTO.instructorEmail().equals("")) {
            section.setInstructor_email("");
          } else {
            instructor = userRepository.findByEmail(sectionDTO.instructorEmail());
            if (instructor == null || !instructor.getType().equals("INSTRUCTOR")) {
              throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "email not found or not an instructor " + sectionDTO.instructorEmail());
            }
            section.setInstructor_email(sectionDTO.instructorEmail());
          }

          sectionRepository.save(section);
          break;

        case "updateSection":
          sectionDTO = fromJsonString(messageParts[1], SectionDTO.class);
          // can only change instructor email, sec_id, building, room, times, start, end dates
          section = sectionRepository.findById(sectionDTO.secNo()).orElse(null);
          if (section==null) {
            throw new ResponseStatusException( HttpStatus.NOT_FOUND, "section not found " + sectionDTO.secNo());
          }
          section.setSecId(sectionDTO.secId());
          section.setBuilding(sectionDTO.building());
          section.setRoom(sectionDTO.room());
          section.setTimes(sectionDTO.times());

          if (sectionDTO.instructorEmail() == null || sectionDTO.instructorEmail().equals("")) {
            section.setInstructor_email("");
          } else {
            instructor = userRepository.findByEmail(sectionDTO.instructorEmail());
            if (instructor == null || !instructor.getType().equals("INSTRUCTOR")) {
              throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Email " + sectionDTO.instructorEmail() + " not found or is not an instructor.");
            }
            section.setInstructor_email(sectionDTO.instructorEmail());
          }
          sectionRepository.save(section);
          break;

        case "deleteSection":
          sectionNo = parseInt(messageParts[1]);
          section = sectionRepository.findById(sectionNo).orElse(null);

          if (!(section.getEnrollments().isEmpty())) {
            throw new ResponseStatusException( HttpStatus.CONFLICT,
              "Cannot delete a section with current enrollments.");
          }

          if (section != null) {
            sectionRepository.delete(section);
          }
          break;

        case "addUser":
          userDTO = fromJsonString(messageParts[1], UserDTO.class);
          user.setName(userDTO.name());
          user.setEmail(userDTO.email());

          // create password and encrypt it
          String password = userDTO.name() + "2024";
          String enc_password = encoder.encode(password);
          user.setPassword(enc_password);

          user.setType(userDTO.type());
          if (!userDTO.type().equals("STUDENT") &&
            !userDTO.type().equals("INSTRUCTOR") &&
            !userDTO.type().equals("ADMIN")) {
            // invalid type
            throw  new ResponseStatusException( HttpStatus.BAD_REQUEST, "invalid user type");
          }
          userRepository.save(user);
          break;

        case "updateUser":
          userDTO = fromJsonString(messageParts[1], UserDTO.class);
          user = userRepository.findById(userDTO.id()).orElse(null);

          if (user == null) {
            throw new ResponseStatusException( HttpStatus.NOT_FOUND, "User id not found");
          }

          user.setName(userDTO.name());
          user.setEmail(userDTO.email());
          user.setType(userDTO.type());

          if (!userDTO.type().equals("STUDENT") &&
            !userDTO.type().equals("INSTRUCTOR") &&
            !userDTO.type().equals("ADMIN")) {
            // invalid type
            throw new ResponseStatusException( HttpStatus.BAD_REQUEST, "invalid user type");
          }

          userRepository.save(user);
          break;

        case "deleteUser":
          int id = parseInt(messageParts[1]);
          user = userRepository.findById(id).orElse(null);

          if (user != null) {
            userRepository.delete(user);
          }
          break;

        case "addEnrollment":
          enrollmentDTO = fromJsonString(messageParts[1], EnrollmentDTO.class);
          enrollment.setEnrollmentId(enrollmentDTO.enrollmentId());
          enrollment.setGrade(enrollmentDTO.grade());
          user = userRepository.findById(enrollmentDTO.studentId()).orElse(null);
          if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student id not found");
          }
          enrollment.setUser(user);

          // check that the Section entity with primary key sectionNo exists
          section = sectionRepository.findById(enrollmentDTO.sectionNo())
            .orElseThrow(() -> new ResponseStatusException(
              HttpStatus.NOT_FOUND, "Section not found"));
          enrollment.setSection(section);

          enrollmentRepository.save(enrollment);
          break;

        case "deleteEnrollment":
          String[] idSplit = messageParts[1].split(" ", 2);
          enrollmentId = parseInt(idSplit[0]);
          studentId = parseInt(idSplit[1]);

          enrollment = enrollmentRepository.findById(enrollmentId).orElse(null);

          if (enrollment == null) {
            sendMessage("MESSAGE Enrollment ID: " + enrollmentId + " not found. Enrollment not removed.");
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Enrollment id not found");
          }

          grades = gradeRepository.findByEnrollmentId(enrollmentId);
          section = enrollment.getSection();
          term = section.getTerm();
          course = section.getCourse();

          // check that today is not after the dropDeadline for section
          if (today.before(term.getDropDeadline())) {
            if(!(grades == null)) {
              for (Grade grade: grades) {
                gradeRepository.delete(grade);
              }
            }
            enrollmentRepository.delete(enrollment);
            System.out.println("Enrollment with ID: " + enrollmentId + " deleted.");
            sendMessage("MESSAGE Enrollment with ID: " + enrollmentId + " deleted.");
          } else {
            sendMessage("MESSAGE Course: " + course.getTitle() + " for Section Number: " +
              section.getSectionNo() + " cannot be dropped after the Drop Deadline.");
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course: " +
              course.getTitle() + " for Section Number: " + section.getSectionNo() +
                " cannot be dropped after the Drop Deadline.") {
            };
          }
          break;

        case "MESSAGE":
          System.out.println("Message received from RegistrarService: " + messageParts[1]);
          break;

        default:
          System.out.println("No method was found for the Request: " + message);
          break;
      }
    } catch (Exception exception) {
      System.out.println("Exception received from RegistrarService: " + exception.toString());
    }
  }

  private void sendMessage(String message) {
    System.out.println("Gradebook to Registrar: " + message);
    rabbitTemplate.convertAndSend(registrarServiceQueue.getName(), message);
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