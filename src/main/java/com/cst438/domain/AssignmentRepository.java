package com.cst438.domain;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface AssignmentRepository extends CrudRepository<Assignment, Integer> {

    // TODO uncomment the following lines as needed

    @Query("select a from Assignment a where a.section.sectionNo=:sectionNo order by a.dueDate")
    List<Assignment> findBySectionNoOrderByDueDate(int sectionNo);

    @Query("select a from Assignment a join a.section.enrollments e " +
            "where a.section.term.year=:year and a.section.term.semester=:semester and" +
            " e.student.id=:studentId order by a.dueDate")
    List<Assignment> findByStudentIdAndYearAndSemesterOrderByDueDate(int studentId, int year, String semester);

}
