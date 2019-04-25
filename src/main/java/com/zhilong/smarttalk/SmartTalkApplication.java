package com.zhilong.smarttalk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import tk.mybatis.spring.annotation.MapperScan;

@ComponentScan(basePackages = {"com.zhilong.smarttalk", "org.n3r.idworker"})
@MapperScan(basePackages = {"com.zhilong.smarttalk.mapper"})
@SpringBootApplication
public class SmartTalkApplication{

    @Bean
    public SpringUtil getSpringUtil() {
        return new SpringUtil();
    }

    public static void main(String[] args) {
        SpringApplication.run(SmartTalkApplication.class, args);
    }

}
