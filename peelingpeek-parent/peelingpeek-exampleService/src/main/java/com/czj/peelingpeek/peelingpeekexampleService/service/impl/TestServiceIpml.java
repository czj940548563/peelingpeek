package com.czj.peelingpeek.peelingpeekexampleService.service.impl;

import com.czj.peelingpeek.peelingpeekexampleService.entity.TestBean;
import com.czj.peelingpeek.peelingpeekexampleService.service.TestService;
import org.springframework.stereotype.Service;

/**
 * @Author: clownc
 * @Date: 2019-06-12 11:01
 */
@Service
public class TestServiceIpml implements TestService {
    @Override
    public String operation(TestBean testBean) {
        return testBean.getAccount();
    }
}
