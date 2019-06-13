package com.czj.peelingpeek.peelingpeekexampleService.controller;

import com.czj.peelingpeek.peelingpeekexampleService.entity.TestBean;
import com.czj.peelingpeek.peelingpeekexampleService.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @Author: clownc
 * @Date: 2019-05-14 14:08
 * 流量没有削峰则请求直接到controller里
 */
@RestController
public class TestController {

    @Autowired
    private TestService testService;
    @PostMapping("/getAccount")
    public String getAccount(@RequestBody TestBean testBean){
        return testService.operation(testBean);
    }

}
