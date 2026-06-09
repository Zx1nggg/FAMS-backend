package com.Zx1nggg.FAMS.modules.test.controller;

import com.Zx1nggg.FAMS.common.api.Result;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test")
@CrossOrigin(origins = "*")
public class TestController {

    @GetMapping("/hello")
    public Map<String, Object> hello() {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "你好 Vue，我是 Spring Boot！GET 通信成功。");
        return response;
    }

    @GetMapping("/health")
    public Result<String> health() {
        // 只要能调通这个接口，说明 SpringBoot 跑得好好的
        return Result.success("System is running normally");
    }



}
