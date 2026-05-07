package com.Zx1nggg.FAMS.modules.test.controller;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
public class TestController {

    @GetMapping("/hello")
    public Map<String, Object> hello() {
        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", "你好 Vue，我是 Spring Boot！GET 通信成功。");
        return response;
    }

    // 2. POST 接口：接收 Vue 传来的 JSON 数据
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        System.out.println("Spring Boot 收到了 Vue 发来的名字: " + username);

        Map<String, Object> response = new HashMap<>();
        response.put("code", 200);
        response.put("message", username + " 登录成功！POST 通信成功。");
        return response;
    }

}
