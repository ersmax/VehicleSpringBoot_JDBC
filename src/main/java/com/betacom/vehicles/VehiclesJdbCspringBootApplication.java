package com.betacom.vehicles;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.betacom.vehicles.process.StartVehicle;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
public class VehiclesJdbCspringBootApplication {

	// files 
	public static final String INPUT_FILE 	= "data/readData.txt";
	public static final String OUTPUT_FILE 	= "data/writeData.txt";
	
	// Spring creates StartVehicle and injects it here
	@Autowired
	private StartVehicle startVehicle;
	
	public static void main(String[] args) {
		SpringApplication.run(VehiclesJdbCspringBootApplication.class, args);
	}
	
	@Bean
	CommandLineRunner startup() {
		return args -> {
			log.info("Start jdbc managed by Springboot ***");
			startVehicle.execute();
			log.info("End Vehicle");
		};
	}

	
}


/* alternative to lambda:
return new CommandLineRunner() {
	@Override
	public void run(String... args) throws Exception {
		log.info("Start jdbc managed by Springboot ***");	
		startVehicle.execute();
		log.info("End Vehicle");
	}
};
*/
