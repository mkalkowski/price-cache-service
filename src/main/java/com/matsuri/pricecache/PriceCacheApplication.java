
package com.matsuri.pricecache;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class PriceCacheApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(PriceCacheApplication.class, args);
    }
}