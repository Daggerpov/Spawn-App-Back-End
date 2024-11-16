package com.danielagapov.spawn.Controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/")
    public String getHome(){
        return "Home";
    }

    @GetMapping("/test")
    public String getTest(){
        return "Test";
    }

    @GetMapping("/person")
    public Person getPerson(){

        Person p = new Person();
        p.setName("Michael");
        p.setAge(40);
        return p;

    }

    @GetMapping("/user")
    public User getUser(){

        User u = new User();
        u.setUsername("Michael");
        u.setFirstname("Michael");
        u.setLastname("Michael");
        u.setBio("Michael");
        return u;

    }

}

class Person{
    private String name;
    private Integer age;

    public String getName() {
        return this.name;
    }

    public Integer getAge() {
        return this.age;
    }

    public void setName(String name) {
        this.name = name;
    }
    public void setAge(Integer age) {
        this.age = age;
    }
}

class User {
    private String username;
    private String firstName;
    private String lastName;
    private String bio;
    
    public String getUsername() {
        return this.username;
    }

    public String getFirstName() {
        return this.firstName;
    }

    public String getLastName() {
        return this.lastName;
    }

    public String getBio() {
        return this.bio;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setFirstname(String firstName) {
        this.firstName = firstName;
    }

    public void setLastname(String lastName) {
        this.lastName = lastName;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }
}
