package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.Models.Event;
import com.danielagapov.spawn.Models.User;
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
        u.setFirstName("Michael");
        u.setLastName("Michael");
        u.setBio("Michael");
        return u;
    }

    @GetMapping("/event")
    public Event getEvent() {
        return new Event(0L, "Cool Event", "10:00 AM", "12:00 PM", "Wesbrook Mall", "this is an event.");
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
