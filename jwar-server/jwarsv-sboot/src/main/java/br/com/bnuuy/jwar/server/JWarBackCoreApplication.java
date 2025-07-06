package br.com.bnuuy.jwar.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "br.com.jwar.server")
public class JWarBackCoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(br.com.bnuuy.jwar.core.JWarBackCoreApplication.class, args);
	}

}
