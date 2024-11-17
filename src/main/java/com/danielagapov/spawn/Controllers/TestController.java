package com.danielagapov.spawn.Controllers;

import com.danielagapov.spawn.Models.Event.Event;
import com.danielagapov.spawn.Models.User.MockUser;
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
    public MockPerson getPerson(){

        MockPerson p = new MockPerson();
        p.setName("Michael");
        p.setAge(40);
        return p;

    }

    @GetMapping("/mock_user")
    public MockUser getUser(){
        MockUser u = new MockUser();

        u.setUsername("mtham");
        u.setFirstName("Michael");
        u.setLastName("Tham");
        u.setBio("This is my bio - " + u.getUsername());

        return u;
    }

    @GetMapping("/event")
    public Event getEvent() {
        return new Event(0L, "Cool Event", "10:00 AM", "12:00 PM", "Wesbrook Mall", "this is an event.");
    }

}

class MockPerson{
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
