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
        log.info("일반 INFO 로그가 생성되었습니다.");
        log.warn("경고 WARN 로그가 생성되었습니다.");
        log.error("!!! 에러 ERROR 로그가 생성되었습니다 !!!");
        return "redirect:/resume";
    }

    @RequestMapping("/resume")
    public String resume() {
        return "resume";
    }
}
