package com.arun.demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@AllArgsConstructor
@NoArgsConstructor
@Data
public class User {

    private Integer id;
    private String name;
    private int age;
    private double salary;
    private String department;
}
