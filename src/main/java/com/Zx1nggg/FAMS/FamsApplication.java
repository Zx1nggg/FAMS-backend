package com.Zx1nggg.FAMS;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.Zx1nggg.FAMS.modules.**.mapper")
public class FamsApplication {

	public static void main(String[] args) {
		SpringApplication.run(FamsApplication.class, args);
	}

}
