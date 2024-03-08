package com.cst438.domain;

import jakarta.persistence.*;

@Entity
public class Course {
    @Id
    @Column(name="course_id")
    private String courseId;
    private String title;
    private int credits;

    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public int getCredits() {
        return credits;
    }
    public void setCredits(int credits) {
        this.credits = credits;
    }
    public String getCourseId() {
        return courseId;
    }
    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

}
