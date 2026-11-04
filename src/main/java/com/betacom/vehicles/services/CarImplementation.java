package com.betacom.vehicles.services;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.betacom.vehicles.dao.CarDAO;
import com.betacom.vehicles.dao.VehiclesDAO;
import com.betacom.vehicles.exception.CustomException;
import com.betacom.vehicles.models.Car;
import com.betacom.vehicles.utils.Validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class CarImplementation extends VehicleAbstract {

	public static final String CAR_PLATE = "[A-Z]{2}[0-9]{3}[A-Z]{2}";

	private final VehiclesDAO vehiclesDAO;
	private final CarDAO carDAO;

	/**
	 * Add a new car: vehicles + cars: both saved, or neither (rollback)
	 */
	@Transactional		
	@Override
	public void add(Map<String, String> parameters) {
		
		Car car= build(parameters);
		if (vehiclesDAO.existsByPlate(car.getPlate()))
			throw new CustomException("Plate"+car.getPlate()+"already in use...");
		
			
		car.setId(vehiclesDAO.insert(car));		// parent row -> new id
		carDAO.insert(car);						// child row with the same id
		log.info("Car added with id {}", car.getId());
	}
	
	
	/**
	 * Update an existing car by id (parameters already merged with the current values)
	 * Plate is resolved to an id in ListImplementation
	 */
	@Transactional
	@Override
	public void update(Map<String, String> parameters) {
	  	Integer id = Validation.getInt(parameters, "id");
	  	Car car = build(parameters);
		car.setId(id);	
		
		// ABSOLUTELY stop an update that can change the plate that 
		// 			  belongs to another vehicle (car/bike)	
		Optional<Integer> idAnotherOwner = vehiclesDAO.findIdByPlate(car.getPlate());
		if (idAnotherOwner.isPresent() && !idAnotherOwner.get().equals(id)) 
			throw new CustomException("Plate: " + car.getPlate() + " already used by id " + idAnotherOwner.get());
		
		vehiclesDAO.update(car);
		
		// Did I update 1 row successfully? 
		// No rows updated (0) / id doesn't exist / not a car -> rollami back 
		if (carDAO.update(car) == 0)
			throw new CustomException("Car not found with id " + id);
		
		log.info("Car updated with id {}", id);
	}
	
	
	@Transactional
	@Override
	public void delete(Map<String, String> parameters) {
		Integer id = Validation.getInt(parameters, "id");
	  	if (vehiclesDAO.delete(id) == 0)
	  		throw new CustomException("Car not found with id " + id);
	  	log.info("Car deleted with id {}", id);
	}
	
	
	private Car build(Map<String,String> parameters) {
		Car car = new Car();
		fillProperties(car, "car", parameters);
		car.setCc(Validation.getIntInRange(parameters, "cc", 50, 8000));
		car.setNumberDoors(Validation.getIntInRange(parameters, "doors", 2, 7));
		car.setPlate(Validation.getPlate(parameters, "plate", CAR_PLATE));
		if (parameters.containsKey("gears"))
			car.setNumberGears(Validation.getIntInRange(parameters, "gears", 1, 10));

		if (car.getNumberWheels() < 3)
			throw new CustomException("A car must have at least 3 wheels");
		if (car.getFuelType().equalsIgnoreCase("manual"))
			throw new CustomException("A car cannot run without fuel");
		
		return car;
	}
	
}
