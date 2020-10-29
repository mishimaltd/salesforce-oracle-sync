package com.mishima.sync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SyncApplication {

  public static void main(String[] args) {
    SpringApplication.run(SyncApplication.class, args);
  }

}
