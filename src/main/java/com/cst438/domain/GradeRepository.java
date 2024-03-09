package com.cst438.domain;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface GradeRepository extends CrudRepository<Grade, Integer> {

    // TODO uncomment the following lines as needed

    @Query("select g from Grade g where g.assignment.assignmentId=:assignmentId and g.enrollment.enrollmentId=:enrollmentId")
    Grade findByEnrollmentIdAndAssignmentId(int enrollmentId, int assignmentId);
}
