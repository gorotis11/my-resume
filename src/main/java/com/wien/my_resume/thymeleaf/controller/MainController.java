package com.wien.my_resume.thymeleaf.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class MainController {

    @RequestMapping("/")
    public String root() {
        return "redirect:/resume";
    }

    @RequestMapping("/resume")
    public String resume(Model model) {
        return "resume";
    }
}
