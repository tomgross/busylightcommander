package com.itconsense.busylightcommander.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.itconsense.busylightcommander.api.BusyLightAPI;

@SpringBootApplication
public class Application {

    @RestController
    public static class HelloWorldController {

        @GetMapping("/hello")
        public String helloWorld(
                @RequestParam final String name) {
            BusyLightAPI light = new BusyLightAPI();
            return "Hello, " + name + "\n";


        }
    }

    public static void main(String... args) {
        SpringApplication.run(Application.class, args);
    }
}