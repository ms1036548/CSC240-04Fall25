package team.phase2.data_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "team.phase2")
public class DataApiApplication {
  public static void main(String[] args) {
    SpringApplication.run(DataApiApplication.class, args);
  }
}

