package org.example.demo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class testcontroller {

    @GetMapping("/test")
    public Map<String, Object> testBackend() {
        return Map.of(
                "success", true,
                "message", "Backend connected successfully!"
        );
    }
}