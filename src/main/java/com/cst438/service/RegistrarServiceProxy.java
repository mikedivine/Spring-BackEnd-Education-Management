package com.cst438.service;

import com.cst438.domain.*;
import com.cst438.dto.CourseDTO;
import com.cst438.dto.SectionDTO;
import com.cst438.dto.UserDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

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

  @RabbitListener(queues = "gradebook_service")
  public void receiveFromRegistrar(String message)  {
    // receive messages for new or updated or deleted courses, sections, users,
    //  and enrollments.  Perform the necessary update to the local database.
    try {
      System.out.println("Receive from Registrar: " + message);
      String[] messageParts = message.split(" ", 2);
      Course course = null;
      CourseDTO courseDTO = null;
      Section section = null;
      SectionDTO sectionDTO;
      Term term;
      User instructor;
      User user = null;
      UserDTO userDTO;
      Enrollment enrollment = new Enrollment();
      int sectionNo;
      int studentId;
      int enrollmentId;
      Date today = new Date();

      switch (messageParts[0]) {

        case "newCourse":
          courseDTO = fromJsonString(messageParts[1], CourseDTO.class);
          course.setCredits(courseDTO.credits());
          course.setTitle(courseDTO.title());
          course.setCourseId(courseDTO.courseId());

          courseRepository.save(course);
          break;

        case "updateCourse":
          courseDTO = fromJsonString(messageParts[1], CourseDTO.class);
          course = courseRepository.findById(courseDTO.courseId()).orElse(null);
          if (course==null) {
            throw new ResponseStatusException( HttpStatus.NOT_FOUND, "Course with ID: "
              + courseDTO.courseId() + " not found.");
          } else {
            course.setCredits(courseDTO.credits());
            course.setTitle(courseDTO.title());
            courseRepository.save(course);
          }
          break;

        case "deleteCourse":
          courseDTO = fromJsonString(messageParts[1], CourseDTO.class);
          course = courseRepository.findById(courseDTO.courseId()).orElse(null);
          // if course does not exist, do nothing.
          if (course != null) {
            courseRepository.delete(course);
          }
          break;

        case "newSection":
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

        case "editSection":
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
          sectionDTO = fromJsonString(messageParts[1], SectionDTO.class);
          section = sectionRepository.findById(sectionDTO.secNo()).orElse(null);

          if (!(section.getEnrollments().isEmpty())) {
            throw new ResponseStatusException( HttpStatus.CONFLICT,
              "Cannot delete a section with current enrollments.");
          }

          if (section != null) {
            sectionRepository.delete(section);
          }
          break;

        case "newUser":
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

        case "editUser":
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

        case "addCourse":
          sectionNo = parseInt(messageParts[1]);
          studentId = parseInt(messageParts[2]);

          user = userRepository.findById(studentId).orElse(null);
          // Verify user exists and is a student
          if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User ID: "
              + studentId + " not found.");
          }
          if (!(user.getType().equals("STUDENT"))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
              "User with ID: " + studentId + " is not a student.");
          }

          enrollment.setUser(user);

          // check that the Section entity with primary key sectionNo exists
          section = sectionRepository.findById(sectionNo)
            .orElseThrow(() -> new ResponseStatusException(
              HttpStatus.NOT_FOUND, "Section with Section Number: " +
                sectionNo + " not found"));

          enrollment.setSection(section);

          // check that today is between addDate and addDeadline for the section
          term = section.getTerm();
          if (today.before(term.getAddDate())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
              "You have attempted to add a course before the Add Date.");
          } else if (today.after(term.getAddDeadline())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
              "You have attempted to add a course after the Add Deadline.");
          }

          // check that student is not already enrolled into this section
          List<Enrollment> enrollments = enrollmentRepository.findEnrollmentsByStudentIdOrderByTermId(studentId);
          for (Enrollment anEnrollment: enrollments) {
            if (anEnrollment.getSection().getSectionNo() == sectionNo) {
              throw new ResponseStatusException(HttpStatus.CONFLICT,
                "You have attempted to add a course the student is already enrolled in.");
            }
          }

          // create a new enrollment entity and save.  The enrollment grade will
          // be NULL until instructor enters final grades for the course.
          enrollmentRepository.save(enrollment);
          break;

        case "dropCourse":
          enrollmentId = parseInt(messageParts[1]);
          studentId = parseInt(messageParts[2]);

          user = userRepository.findById(studentId).orElse(null);
          // Verify user exists and is a student
          if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User ID: "
              + studentId + " not found.");
          }
          if (!(user.getType().equals("STUDENT"))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
              "User with ID: " + studentId + " is not a student.");
          }

          enrollment = enrollmentRepository.findById(enrollmentId).orElse(null);
          if (enrollment == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
              "Enrollment ID: " + enrollmentId + " not found.");
          }

          List<Grade> grades = enrollment.getGrades();
          section = enrollment.getSection();
          term = section.getTerm();
          course = section.getCourse();

          // check that today is not after the dropDeadline for section
          if (today.before(term.getDropDeadline())) {
            for (Grade grade: grades) {
              gradeRepository.delete(grade);
            }
            enrollmentRepository.delete(enrollment);
          } else {
            throw new ResponseStatusException(
              HttpStatus.CONFLICT,
              "Course: " + course.getTitle() + " for Section Number: " +
                section.getSectionNo() + " cannot be dropped after the Drop Deadline.");
          }
          break;
      }
    } catch (Exception exception) {
      System.out.println("Exception received from RegistrarService: " + exception.getMessage());
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