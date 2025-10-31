package com.hduce.eagle_bank_api;

import org.springframework.boot.SpringApplication;

public class TestEagleBankApiApplication {

  public static void main(String[] args) {
    SpringApplication.from(EagleBankApiApplication::main)
        .with(TestcontainersConfiguration.class)
        .run(args);
  }
}
