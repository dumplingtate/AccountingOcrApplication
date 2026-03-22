package com.accounting.ocr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AccountingOcrApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountingOcrApplication.class, args);
    }

}
