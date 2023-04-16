package com.arun.demo.controller;

import com.arun.demo.model.User;
import com.arun.demo.service.HttpWebClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/client")
public class WebClientController {

    @Autowired
    private HttpWebClient httpWebClient;


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<User> create(@RequestBody User user) {
        return httpWebClient.postUser(user);
    }

    @GetMapping
    public Flux<User> getAllUsers() {
        return httpWebClient.listUsers();
    }

    @GetMapping("/error")
    public Mono<User> error() {
        return httpWebClient.error();
    }

}
