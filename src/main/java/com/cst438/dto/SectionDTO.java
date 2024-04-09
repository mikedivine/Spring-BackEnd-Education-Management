package com.cst438.dto;


/*
 * Data Transfer Object for data for a section of a course
 */
public record SectionDTO(
        int secNo,
        int year,
        String semester,
        String courseId,
        String title,
        int secId,
        String building,
        String room,
        String times,
        String instructorName,
        String instructorEmail

       ) {
}
