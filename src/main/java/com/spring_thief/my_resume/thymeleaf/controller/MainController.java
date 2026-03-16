package com.spring_thief.my_resume.thymeleaf.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class MainController {

    private final Logger log = LoggerFactory.getLogger(MainController.class);

    @RequestMapping("/")
    public String root() {
        return "redirect:/resume";
    }

    @RequestMapping("/resume")
    public String resume() {
        return "resume";
    }
}
