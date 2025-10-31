package com.hduce.bank_api;

import org.springframework.boot.SpringApplication;

public class TestBankApiApplication {

  public static void main(String[] args) {
    SpringApplication.from(BankApiApplication::main)
        .with(TestcontainersConfiguration.class)
        .run(args);
  }
}
