package com.example.neo4japi;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Node
public class Person {

    @Id @GeneratedValue private Long id;

    private String name;

    private int age;

    @JsonIgnore
    private String userupdate;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getUserupdate() {
        return userupdate;
    }

    public void setUserupdate(String userupdate) {
        this.userupdate = userupdate;
    } 

}
