package com.cst438.controller;

import com.cst438.domain.Section;
import com.cst438.domain.SectionRepository;
import com.cst438.dto.LoginDTO;
import com.cst438.dto.SectionDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SectionControllerWithSecurityUnitTest {

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private TestRestTemplate template;

    @Test
    public void addSection() throws Exception {

        // create DTO with data for new section.
        // the primary key, secNo, is set to 0. it will be
        // set by the database when the section is inserted.
        SectionDTO section = new SectionDTO(
                0,
                2024,
                "Spring",
                "cst499",
                "",
                1,
                "052",
                "104",
                "W F 1:00-2:50 pm",
                "Joshua Gross",
                "jgross@csumb.edu"
        );

        // issue a http POST request to SpringTestServer
        ResponseEntity<String> result = template.withBasicAuth("admin@csumb.edu", "admin")
                .getForEntity("/login", String.class);
        assertEquals(HttpStatus.OK, result.getStatusCode());

        // login controller returns jwt token and user type
        LoginDTO dto = fromJsonString(result.getBody(), LoginDTO.class);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(dto.jwt());
        HttpEntity<SectionDTO> entity = new HttpEntity<>(section, headers);

        // post to /sections to create new section
        result = template.exchange("/sections", HttpMethod.POST, entity, String.class);

        assertEquals(HttpStatus.OK, result.getStatusCode());

        // return data converted from String to DTO
        SectionDTO resultSection = fromJsonString(result.getBody(), SectionDTO.class);
        // primary key should have a non zero value from the database
        assertNotEquals(0, resultSection.secNo());
        // check other fields of the DTO for expected values
        assertEquals("cst499", resultSection.courseId());

        // check the database
        Section s = sectionRepository.findById(resultSection.secNo()).orElse(null);
        assertNotNull(s);
        assertEquals("cst499", s.getCourse().getCourseId());

        // clean up after test. issue http DELETE request for section
        sectionRepository.deleteById(resultSection.secNo());
    }

    @Test
    public void testMethodSecurity() {

        SectionDTO section = new SectionDTO(
                0,
                2024,
                "Spring",
                "cst499",
                "",
                1,
                "052",
                "104",
                "W F 1:00-2:50 pm",
                "Joshua Gross",
                "jgross@csumb.edu"
        );

        // issue a http POST request to SpringTestServer
        ResponseEntity<String> result = template.withBasicAuth("user@csumb.edu", "user")
                .getForEntity("/login", String.class);
        assertEquals(HttpStatus.OK, result.getStatusCode());

        // login controller returns jwt token and user type
        LoginDTO dto = fromJsonString(result.getBody(), LoginDTO.class);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(dto.jwt());
        HttpEntity<SectionDTO> entity = new HttpEntity<>(section, headers);

        // post to /sections to create new section
        result = template.exchange("/sections", HttpMethod.POST, entity, String.class);

        assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
    }

   @Test
    public void addSectionFailsBadCourse( ) throws Exception {

        MockHttpServletResponse response;

        // course id cst599 does not exist.
        SectionDTO section = new SectionDTO(
                0,
                2024,
                "Spring",
                "cst599",
                "",
                1,
                "052",
                "104",
                "W F 1:00-2:50 pm",
                "Joshua Gross",
                "jgross@csumb.edu"
        );

       // issue a http POST request to SpringTestServer
       ResponseEntity<String> result = template.withBasicAuth("admin@csumb.edu", "admin")
               .getForEntity("/login", String.class);
       assertEquals(HttpStatus.OK, result.getStatusCode());

       // login controller returns jwt token and user type
       LoginDTO dto = fromJsonString(result.getBody(), LoginDTO.class);

       HttpHeaders headers = new HttpHeaders();
       headers.setBearerAuth(dto.jwt());
       HttpEntity<SectionDTO> entity = new HttpEntity<>(section, headers);

       // post to /sections to create new section
       result = template.exchange("/sections", HttpMethod.POST, entity, String.class);

       assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
       assertTrue(result.getBody().contains("course not found"));
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
