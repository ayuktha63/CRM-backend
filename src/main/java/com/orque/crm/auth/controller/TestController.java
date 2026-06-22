package com.orque.crm.auth.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/api/v1/test/protected")
    public String protectedApi() {
        return "JWT is working. You accessed a protected API.";
    }
}