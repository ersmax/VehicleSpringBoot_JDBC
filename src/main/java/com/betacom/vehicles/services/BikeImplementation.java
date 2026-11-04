package com.betacom.vehicles.services;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.betacom.vehicles.dao.BikeDAO;
import com.betacom.vehicles.dao.VehiclesDAO;
import com.betacom.vehicles.exception.CustomException;
import com.betacom.vehicles.models.Bike;
import com.betacom.vehicles.utils.Validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class BikeImplementation extends VehicleAbstract {

	public static final String BIKE_PLATE = "[A-Z]{2}[0-9]{5}";

	private final VehiclesDAO vehiclesDAO;
	private final BikeDAO bikeDAO;

	/**
	 * add a new bike: vehicles + bikes in one transaction
	 */
	@Transactional
	@Override
	public void add(Map<String, String> parameters) {
		Bike bike = build(parameters);
		if (vehiclesDAO.existsByPlate(bike.getPlate()))
			throw new CustomException("Plate " + bike.getPlate() + " already used");
		
		bike.setId(vehiclesDAO.insert(bike));
		bikeDAO.insert(bike);
		log.info("Bike added with id {}", bike.getId());
	}
	
	/**
	 * Update an existing bike by id (parameters already merged with the current values)
	 * Plate is resolved to an id in ListImplementation
	 */
	@Transactional
	@Override
	public void update(Map<String, String> parameters) {
		Integer id = Validation.getInt(parameters, "id");
		Bike bike = build(parameters);
		bike.setId(id);	
		
		// ABSOLUTELY stop an update that can change the plate that 
		// 			  belongs to another vehicle (car/bike)	
		Optional<Integer> owner = vehiclesDAO.findIdByPlate(bike.getPlate());
		if (owner.isPresent() && !owner.get().equals(id))
			throw new CustomException("Plate " + bike.getPlate() + " already used by id " + owner.get());
		
		vehiclesDAO.update(bike);
		
		// Did I update 1 row successfully? 
		// No rows updated (0) / id doesn't exist / not a bike -> rollami back 
		if (bikeDAO.update(bike) == 0)
			throw new CustomException("Bike not found with id " + id);
		
		log.info("Bike updated with id {}", id);
	}
	
	@Transactional
	@Override
	public void delete(Map<String, String> parameters) {
		Integer id = Validation.getInt(parameters, "id");
		if (vehiclesDAO.delete(id) == 0)
			throw new CustomException("Bike not found with id " + id);
		log.info("Bike deleted with id {}", id);
	}
	
	private Bike build(Map<String,String> parameters) {
		Bike bike = new Bike();
		fillProperties(bike, "bike", parameters);
		bike.setPlate(Validation.getPlate(parameters, "plate", BIKE_PLATE));
		bike.setCc(Validation.getIntInRange(parameters, "cc", 50, 3000));
		if (parameters.containsKey("gears"))
			bike.setNumberGears(Validation.getIntInRange(parameters, "gears", 1, 10));

		if (bike.getNumberWheels() != 2)
			throw new CustomException("A bike must have 2 wheels");
		if (bike.getFuelType().equals("manual"))
			throw new CustomException("A bike cannot run without fuel");

		return bike;
	}
}
