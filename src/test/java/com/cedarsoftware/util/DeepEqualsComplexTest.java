package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class DeepEqualsComplexTest {
    enum AcademicRank { ASSISTANT, ASSOCIATE, FULL }

    static class University {
        String name;
        Map<String, Department> departmentsByCode;
        Address location;
    }

    static class Department {
        String code;
        String name;
        List<Program> programs;
        Faculty departmentHead;
        List<Faculty> facultyMembers;  // New field that can hold both Faculty and Professor
    }
    
    static class Program {
        String programName;
        int durationYears;
        Course[] requiredCourses;
        Professor programCoordinator;
    }

    static class Course {
        String courseCode;
        int creditHours;
        Set<Student> enrolledStudents;
        Syllabus syllabus;
        Faculty instructor;
    }

    static class Syllabus {
        String description;
        double passingGrade;
        Map<String, Assessment> assessments;
        TextBook recommendedBook;
    }

    static class Assessment {
        String name;
        int weightage;
        Date dueDate;
        GradingCriteria criteria;
    }

    static class GradingCriteria {
        String[] rubricPoints;
        int maxScore;
        Map<String, Double> componentWeights;
    }

    static class Person {
        String id;
        String name;
        Address address;
    }

    static class Faculty extends Person {
        String department;
        List<Course> teachingCourses;
        AcademicRank rank;
    }

    static class Professor extends Faculty {
        String specialization;
        List<Student> advisees;
        ResearchLab lab;
    }

    static class Student extends Person {
        double gpa;
        Program enrolledProgram;
        Map<Course, Grade> courseGrades;

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Student student = (Student) o;
            return Double.compare(gpa, student.gpa) == 0 && Objects.equals(enrolledProgram, student.enrolledProgram) && Objects.equals(courseGrades, student.courseGrades);
        }

        @Override
        public int hashCode() {
            return Objects.hash(gpa, enrolledProgram, courseGrades);
        }
    }

    static class Address {
        String street;
        String city;
        String postalCode;
        GeoLocation coordinates;
    }

    static class GeoLocation {
        double latitude;
        double longitude;
    }

    static class TextBook {
        String title;
        String[] authors;
        String isbn;
        Publisher publisher;
    }

    static class Publisher {
        String name;
        String country;
    }

    static class Grade {
        double score;
        String letterGrade;
    }

    static class ResearchLab {
        String name;
        Equipment[] equipment;
        List<Project> activeProjects;
    }

    static class Equipment {
        String name;
        String serialNumber;
    }

    static class Project {
        String name;
        Date startDate;
        List<String> objectives;
    }

    String getDiff(Map<String, Object> options) {
        return (String) options.get(DeepEquals.DIFF);
    }

    @Test
    void testIdentity() {
        University university1 = buildComplexUniversity();
        University university2 = buildComplexUniversity();
        Map<String, Object> options = new HashMap<>();

        assertTrue(DeepEquals.deepEquals(university1, university2, options));
    }

    @Test
    void testArrayElementMismatch() {
        Student[] array1 = new Student[1];
        Student[] array2 = new Student[1];

        Student student1 = new Student();
        student1.id = "TEST-ID";
        student1.gpa = 3.5;

        Student student2 = new Student();
        student2.id = "TEST-ID";
        student2.gpa = 4.0;

        array1[0] = student1;
        array2[0] = student2;

        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(array1, array2, options));
        assertTrue(getDiff(options).contains("field value mismatch")); // Changed expectation
    }

    @Test
    void testListElementMismatch() {
        List<Student> list1 = new ArrayList<>();
        List<Student> list2 = new ArrayList<>();

        Student student1 = new Student();
        student1.id = "TEST-ID";
        student1.gpa = 3.5;

        Student student2 = new Student();
        student2.id = "TEST-ID";
        student2.gpa = 4.0;

        list1.add(student1);
        list2.add(student2);

        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(list1, list2, options));
        assertTrue(getDiff(options).contains("field value mismatch")); // Changed expectation
    }

    @Test
    void testSetElementMissing() {
        Set<Student> set1 = new LinkedHashSet<>();
        Set<Student> set2 = new LinkedHashSet<>();

        Student student1 = new Student();
        student1.id = "TEST-ID";
        student1.gpa = 3.5;

        Student student2 = new Student();
        student2.id = "TEST-ID";
        student2.gpa = 4.0;

        set1.add(student1);
        set2.add(student2);

        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(set1, set2, options));
        assertTrue(getDiff(options).contains("missing collection element"));
    }
    
    @Test
    void testSimpleValueMismatch() {
        University university1 = buildComplexUniversity();
        University university2 = buildComplexUniversity();

        // Modify a deep string value
        university2.departmentsByCode.get("CS").programs.get(0)
                .requiredCourses[0].syllabus.description = "Different description";

        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(university1, university2, options));
        assertTrue(getDiff(options).contains("value mismatch"));
    }
    
    @Test
    void testArrayLengthMismatch() {
        University university1 = buildComplexUniversity();
        University university2 = buildComplexUniversity();

        // Change array length
        university2.departmentsByCode.get("CS").programs.get(0)
                .requiredCourses = new Course[3];  // Original was length 2

        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(university1, university2, options));
        assertTrue(getDiff(options).contains("array length mismatch"));
    }

    @Test
    void testCollectionSizeMismatch() {
        University university1 = buildComplexUniversity();
        University university2 = buildComplexUniversity();

        // Add an extra program to department
        Department dept2 = university2.departmentsByCode.get("CS");
        dept2.programs.add(createProgram("CS-ExtraProgram", dept2));

        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(university1, university2, options));
        assertTrue(getDiff(options).contains("collection size mismatch"));
    }

    @Test
    void testMapMissingKey() {
        University university1 = buildComplexUniversity();
        University university2 = buildComplexUniversity();

        // Remove a key from assessments map
        university2.departmentsByCode.get("CS").programs.get(0)
                .requiredCourses[0].syllabus.assessments.remove("Midterm");

        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(university1, university2, options));
        assertTrue(getDiff(options).contains("map size mismatch")); // Changed expectation
    }

    @Test
    void testMapValueMismatch() {
        University university1 = buildComplexUniversity();
        University university2 = buildComplexUniversity();

        // Modify a map value
        university2.departmentsByCode.get("CS").programs.get(0)
                .requiredCourses[0].syllabus.assessments.get("Midterm").weightage = 50;

        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(university1, university2, options));
        assertTrue(getDiff(options).contains("field value mismatch")); // Changed expectation
    }

    @Test
    void testTypeMismatch() {
        University university1 = buildComplexUniversity();
        University university2 = buildComplexUniversity();

        // Add a List<Faculty> to Department that can hold either Professor or Faculty
        Department dept1 = university1.departmentsByCode.get("CS");
        Department dept2 = university2.departmentsByCode.get("CS");

        dept1.facultyMembers = new ArrayList<>();
        dept2.facultyMembers = new ArrayList<>();

        // Add different types to each
        dept1.facultyMembers.add(createProfessor("CS"));
        dept2.facultyMembers.add(createFaculty("CS"));

        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(university1, university2, options));
        assertTrue(getDiff(options).contains("collection element mismatch")); // Changed expectation
    }

    @Test
    void testCollectionElementMismatch() {
        Set<Student> set1 = new LinkedHashSet<>();
        Set<Student> set2 = new LinkedHashSet<>();

        Student student1 = createStudent("TEST-STUDENT");
        student1.gpa = 3.5;

        Student student2 = createStudent("TEST-STUDENT");
        student2.gpa = 4.0;

        set1.add(student1);
        set2.add(student2);

        Course course1 = new Course();
        Course course2 = new Course();

        course1.enrolledStudents = set1;
        course2.enrolledStudents = set2;

        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(course1, course2, options));
        assertTrue(getDiff(options).contains("missing collection element")); // Changed expectation
    }

    @Test
    void testSetElementValueMismatch() {
        Set<Student> set1 = new LinkedHashSet<>();
        Set<Student> set2 = new LinkedHashSet<>();

        // Create two students with identical IDs but different GPAs
        Student student1 = new Student();
        student1.id = "TEST-ID";
        student1.name = "Test Student";
        student1.gpa = 3.5;

        Student student2 = new Student();
        student2.id = "TEST-ID";
        student2.name = "Test Student";
        student2.gpa = 4.0;

        set1.add(student1);
        set2.add(student2);

        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(set1, set2, options));
        assertTrue(getDiff(options).contains("missing collection element"));  // This is the correct expectation
    }

    @Test
    void testCompositeObjectFieldMismatch() {
        Student student1 = new Student();
        student1.id = "TEST-ID";
        student1.gpa = 3.5;

        Student student2 = new Student();
        student2.id = "TEST-ID";
        student2.gpa = 4.0;

        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(student1, student2, options));
        assertTrue(getDiff(options).contains("field value mismatch")); // Reports from Student's perspective
    }

    @Test
    void testMapSimpleValueMismatch() {
        Map<String, String> map1 = new LinkedHashMap<>();
        Map<String, String> map2 = new LinkedHashMap<>();

        map1.put("key", "value1");
        map2.put("key", "value2");

        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(map1, map2, options));
        assertTrue(getDiff(options).contains("map value mismatch")); // Reports from Map's perspective
    }
    
    @Test
    void testMapCompositeValueMismatch() {
        Map<String, Student> map1 = new LinkedHashMap<>();
        Map<String, Student> map2 = new LinkedHashMap<>();

        Student student1 = new Student();
        student1.id = "TEST-ID";
        student1.gpa = 3.5;

        Student student2 = new Student();
        student2.id = "TEST-ID";
        student2.gpa = 4.0;

        map1.put("student", student1);
        map2.put("student", student2);

        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(map1, map2, options));
        assertTrue(getDiff(options).contains("field value mismatch")); // Reports from Student's perspective
    }

    @Test
    void testListSimpleTypeMismatch() {
        List<String> list1 = new ArrayList<>();
        List<String> list2 = new ArrayList<>();

        list1.add("value1");
        list2.add("value2");

        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(list1, list2, options));
        assertTrue(getDiff(options).contains("collection element mismatch")); // This one should report at collection level
    }

    @Test
    void testSetSimpleTypeMismatch() {
        Set<String> set1 = new LinkedHashSet<>();
        Set<String> set2 = new LinkedHashSet<>();

        set1.add("value1");
        set2.add("value2");

        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(set1, set2, options));
        assertTrue(getDiff(options).contains("missing collection element"));
    }

    @Test
    void testArraySimpleTypeMismatch() {
        String[] array1 = new String[] { "value1" };
        String[] array2 = new String[] { "value2" };

        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(array1, array2, options));
        assertTrue(getDiff(options).contains("array element mismatch")); // This should work for simple types
    }

    @Test
    void testMapDifferentKey() {
        Map<String, String> map1 = new HashMap<>();
        Map<String, String> map2 = new HashMap<>();

        map1.put("key1", "value");
        map2.put("key2", "value");

        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(map1, map2, options));
        assertTrue(getDiff(options).contains("missing map key"));
    }

    @Test
    void testNullVsEmptyCollection() {
        University university1 = buildComplexUniversity();
        University university2 = buildComplexUniversity();

        Department dept2 = university2.departmentsByCode.get("CS");
        dept2.programs = new ArrayList<>();  // Empty vs non-empty

        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(university1, university2, options));
        assertTrue(getDiff(options).contains("collection size mismatch"));
    }

    @Test
    void testCollectionInterfaceTypes() {
        // Lists - order matters
        List<String> list1 = Arrays.asList("a", "b");
        List<String> list2 = new LinkedList<>(Arrays.asList("b", "a")); // Same elements, different order
        assertFalse(DeepEquals.deepEquals(list1, list2));

        // Sets - order doesn't matter
        Set<String> set1 = new HashSet<>(Arrays.asList("a", "b"));
        Set<String> set2 = new LinkedHashSet<>(Arrays.asList("b", "a")); // Same elements, different order
        assertTrue(DeepEquals.deepEquals(set1, set2));

        // Different collection interfaces aren't equal
        List<String> asList = Arrays.asList("a", "b");
        Set<String> asSet = new LinkedHashSet<>(Arrays.asList("a", "b"));
        assertTrue(DeepEquals.deepEquals(asList, asSet));
    }

    @Test
    void testCircularReference() {
        University university1 = buildComplexUniversity();
        University university2 = buildComplexUniversity();

        // Create circular reference
        Professor prof1 = university1.departmentsByCode.get("CS").programs.get(0).programCoordinator;
        Course course1 = university1.departmentsByCode.get("CS").programs.get(0).requiredCourses[0];
        prof1.teachingCourses.add(course1);
        course1.instructor = prof1;  // Add this field to Course

        // Different circular reference in university2
        Professor prof2 = university2.departmentsByCode.get("CS").programs.get(0).programCoordinator;
        Course course2 = university2.departmentsByCode.get("CS").programs.get(0).requiredCourses[1]; // Different course
        prof2.teachingCourses.add(course2);
        course2.instructor = prof2;

        Map<String, Object> options = new HashMap<>();
        assertFalse(DeepEquals.deepEquals(university1, university2, options));
    }

    @Test
    void testMapTypes() {
        // Different concrete types but same interface - should be equal
        Map<String, String> map1 = new HashMap<>();
        Map<String, String> map2 = new LinkedHashMap<>();

        map1.put("key1", "value1");
        map2.put("key1", "value1");

        assertTrue(DeepEquals.deepEquals(map1, map2));  // Concrete type doesn't matter

        // Different order but same content - should be equal
        Map<String, String> map3 = new LinkedHashMap<>();
        map3.put("key2", "value2");
        map3.put("key1", "value1");

        Map<String, String> map4 = new LinkedHashMap<>();
        map4.put("key1", "value1");
        map4.put("key2", "value2");

        assertTrue(DeepEquals.deepEquals(map3, map4));  // Order doesn't matter
    }
    
    private University buildComplexUniversity() {
        // Create the main university
        University university = new University();
        university.name = "Test University";
        university.location = createAddress("123 Main St", "College Town", "12345");
        university.departmentsByCode = new HashMap<>();

        // Create departments (2 departments)
        Department csDept = createDepartment("CS", "Computer Science", 3);  // 3 programs
        Department mathDept = createDepartment("MATH", "Mathematics", 2);   // 2 programs
        university.departmentsByCode.put(csDept.code, csDept);
        university.departmentsByCode.put(mathDept.code, mathDept);

        return university;
    }

    private Department createDepartment(String code, String name, int programCount) {
        Department dept = new Department();
        dept.code = code;
        dept.name = name;
        dept.programs = new ArrayList<>();
        dept.departmentHead = createFaculty(code);

        // Create programs
        for (int i = 0; i < programCount; i++) {
            dept.programs.add(createProgram(code + "-Program" + i, dept));
        }

        return dept;
    }

    private Program createProgram(String name, Department dept) {
        Program program = new Program();
        program.programName = name;
        program.durationYears = 4;
        program.programCoordinator = createProfessor(dept.code);

        // Create 2 required courses
        program.requiredCourses = new Course[2];
        program.requiredCourses[0] = createCourse(dept.code + "101", dept);
        program.requiredCourses[1] = createCourse(dept.code + "102", dept);

        return program;
    }

    private Course createCourse(String code, Department dept) {
        Course course = new Course();
        course.courseCode = code;
        course.creditHours = 3;
        course.enrolledStudents = new LinkedHashSet<>();  // Changed from HashSet

        // Add 3 students in deterministic order
        for (int i = 0; i < 3; i++) {
            course.enrolledStudents.add(createStudent(dept.code + "-STU" + i));
        }

        course.syllabus = createSyllabus();
        return course;
    }

    private Syllabus createSyllabus() {
        Syllabus syllabus = new Syllabus();
        syllabus.description = "Course syllabus description";
        syllabus.passingGrade = 60.0;
        syllabus.recommendedBook = createTextBook();

        // Create 2 assessments with deterministic order
        syllabus.assessments = new LinkedHashMap<>();  // Changed from HashMap
        syllabus.assessments.put("Midterm", createAssessment("Midterm", 30));
        syllabus.assessments.put("Final", createAssessment("Final", 40));

        return syllabus;
    }

    private Assessment createAssessment(String name, int weightage) {
        Assessment assessment = new Assessment();
        assessment.name = name;
        assessment.weightage = weightage;
        assessment.dueDate = Converter.convert("2025/01/05 19:43:00 EST", Date.class);
        assessment.criteria = createGradingCriteria();
        return assessment;
    }

    private GradingCriteria createGradingCriteria() {
        GradingCriteria criteria = new GradingCriteria();
        criteria.rubricPoints = new String[]{"Excellent", "Good", "Fair"};
        criteria.maxScore = 100;
        criteria.componentWeights = new HashMap<>();
        criteria.componentWeights.put("Content", 70.0);
        criteria.componentWeights.put("Presentation", 30.0);
        return criteria;
    }

    private Professor createProfessor(String deptCode) {
        Professor prof = new Professor();
        prof.id = "PROF-" + deptCode;
        prof.name = "Professor " + deptCode;
        prof.address = createAddress("456 Prof St", "Faculty Town", "67890");
        prof.department = deptCode;
        prof.rank = AcademicRank.ASSOCIATE;
        prof.specialization = "Specialization " + deptCode;
        prof.teachingCourses = new ArrayList<>();  // Will be populated later
        prof.advisees = new ArrayList<>();         // Will be populated later
        prof.lab = createResearchLab(deptCode);
        return prof;
    }

    private ResearchLab createResearchLab(String deptCode) {
        ResearchLab lab = new ResearchLab();
        lab.name = deptCode + " Research Lab";
        lab.equipment = new Equipment[]{
                createEquipment("Equipment1"),
                createEquipment("Equipment2")
        };
        lab.activeProjects = new ArrayList<>();
        lab.activeProjects.add(createProject("Project1"));
        return lab;
    }

    private Equipment createEquipment(String name) {
        Equipment equipment = new Equipment();
        equipment.name = name;
        equipment.serialNumber = "SN-" + name;
        return equipment;
    }

    private Project createProject(String name) {
        Project project = new Project();
        project.name = name;
        // Use a fixed date instead of new Date()
        project.startDate = new Date(1704495545000L); // Some fixed timestamp
        project.objectives = Arrays.asList("Objective1", "Objective2", "Objective3");
        return project;
    }
    
    private Student createStudent(String id) {
        Student student = new Student();
        student.id = id;
        student.name = "Student " + id;
        student.address = createAddress("789 Student St", "Student Town", "13579");
        student.gpa = 3.5;
        student.courseGrades = new HashMap<>();  // Will be populated later
        return student;
    }

    private Faculty createFaculty(String deptCode) {
        Faculty faculty = new Faculty();
        faculty.id = "FAC-" + deptCode;
        faculty.name = "Faculty " + deptCode;
        faculty.address = createAddress("321 Faculty St", "Faculty Town", "24680");
        faculty.department = deptCode;
        faculty.rank = AcademicRank.ASSISTANT;
        faculty.teachingCourses = new ArrayList<>();  // Will be populated later
        return faculty;
    }

    private Address createAddress(String street, String city, String postal) {
        Address address = new Address();
        address.street = street;
        address.city = city;
        address.postalCode = postal;
        address.coordinates = createGeoLocation();
        return address;
    }

    private GeoLocation createGeoLocation() {
        GeoLocation location = new GeoLocation();
        location.latitude = 40.7128;
        location.longitude = -74.0060;
        return location;
    }

    private TextBook createTextBook() {
        TextBook book = new TextBook();
        book.title = "Sample TextBook";
        book.authors = new String[]{"Author1", "Author2"};
        book.isbn = "123-456-789";
        book.publisher = createPublisher();
        return book;
    }

    private Publisher createPublisher() {
        Publisher publisher = new Publisher();
        publisher.name = "Test Publisher";
        publisher.country = "Test Country";
        return publisher;
    }
}