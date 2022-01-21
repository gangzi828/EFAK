package org.smartloli.kafka.eagle.web;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author gangzi
 */
@MapperScan("org.smartloli.kafka.eagle.web.dao")
@SpringBootApplication
public class EFAKApplication {

    public static void main(String[] args) {
        SpringApplication.run(EFAKApplication.class, args);
    }

}
