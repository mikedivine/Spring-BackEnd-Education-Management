package com.cst438.controller;

import com.cst438.domain.Assignment;
import com.cst438.domain.AssignmentRepository;
import com.cst438.domain.Section;
import com.cst438.dto.AssignmentDTO;
import com.cst438.dto.SectionDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static com.cst438.test.utils.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

@AutoConfigureMockMvc
@SpringBootTest
public class AssignmentControllerUnitTest {

  @Autowired
  MockMvc mvc;

  @Autowired
  AssignmentRepository assignmentRepository;

  // instructor adds a new assignment
  @Test
  public void addNewAssignment() throws Exception {

    MockHttpServletResponse response;

    // create DTO with data for new assignment.
    // the primary key, id, is set to 0. it will be
    // set by the database when the section is inserted.
    AssignmentDTO assignment = new AssignmentDTO(
      0,
      "Test Assignment",
      "2024-03-03",
      "cst363",
      "The Good Stuff",
      1,
      8
    );

    // issue a http POST request to SpringTestServer
    // specify MediaType for request and response data
    // convert Assignment to String data and set as request content
    response = mvc.perform(
        MockMvcRequestBuilders
          .post("/assignments")
          .param("instructorEmail", "dwisneski@csumb.edu")
          .accept(MediaType.APPLICATION_JSON)
          .contentType(MediaType.APPLICATION_JSON)
          .content(asJsonString(assignment)))
      .andReturn()
      .getResponse();

    // check the response code for 200 meaning OK
    assertEquals(200, response.getStatus());

    // return data converted from String to DTO
    AssignmentDTO result = fromJsonString(response.getContentAsString(), AssignmentDTO.class);

    // primary key should have a non zero value from the database
    assertNotEquals(0, result.secNo());
    // check other fields of the DTO for expected values
    assertEquals("cst363", result.courseId());

    // check the database
    Assignment a = assignmentRepository.findById(result.id()).orElse(null);
    assertNotNull(a);
    assertEquals(8, a.getSection().getSectionNo());

    // clean up after test. issue http DELETE request for section
    response = mvc.perform(
        MockMvcRequestBuilders
          .delete("/assignments/" + result.id() + "?instructorEmail=dwisneski@csumb.edu"))
          .andReturn()
          .getResponse();

    assertEquals(200, response.getStatus());

    // check database for delete
    a = assignmentRepository.findById(result.id()).orElse(null);
    assertNull(a);  // assignment should not be found after delete
  }

  // instructor adds a new assignment with a due date past the end date of the class
  @Test
  public void addNewAssignmentBadDueDate() throws Exception {

    MockHttpServletResponse response;

    // create DTO with data for new assignment.
    // the primary key, id, is set to 0. it will be
    // set by the database when the section is inserted.
    AssignmentDTO assignment = new AssignmentDTO(
      0,
      "Test Assignment",
      "2025-03-03",
      "cst363",
      "The Good Stuff",
      1,
      8
    );

    // issue an http POST request to SpringTestServer
    // specify MediaType for request and response data
    // convert Assignment to String data and set as request content
    response = mvc.perform(
        MockMvcRequestBuilders
          .post("/assignments")
          .param("instructorEmail", "dwisneski@csumb.edu")
          .accept(MediaType.APPLICATION_JSON)
          .contentType(MediaType.APPLICATION_JSON)
          .content(asJsonString(assignment)))
      .andReturn()
      .getResponse();

    // response should be 409, CONFLICT
    assertEquals(409, response.getStatus());

    // check the expected error message
    String message = response.getErrorMessage();
    assertEquals("You have attempted to add an assignment with the Due Date: 2025-03-03 after the Section End Date: 2024-05-17", message);

  }
}
