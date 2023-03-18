package com.tba.terminal_simulation;

import com.tba.terminal_simulation.service.TruckService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TerminalSimulationApplication {

	public static void main(String[] args) {
		SpringApplication.run(TerminalSimulationApplication.class, args);
	}

}
