package com.spring_thief.my_resume.thymeleaf.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class MainController {

    @RequestMapping("/")
    public String root() {
        return "redirect:/resume";
    }

    @RequestMapping("/resume")
    public String resume() {
        return "resume";
    }
}
