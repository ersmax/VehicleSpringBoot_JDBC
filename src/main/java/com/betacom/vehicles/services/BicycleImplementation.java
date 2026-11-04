package com.betacom.vehicles.services;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.betacom.vehicles.dao.BicycleDAO;
import com.betacom.vehicles.dao.VehiclesDAO;
import com.betacom.vehicles.exception.CustomException;
import com.betacom.vehicles.models.Bicycle;
import com.betacom.vehicles.utils.Validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class BicycleImplementation extends VehicleAbstract {

	private final VehiclesDAO vehiclesDAO;
	private final BicycleDAO bicycleDAO;

	@Transactional
	@Override
	public void add(Map<String, String> parameters) {
		Bicycle bicycle = build(parameters);
		bicycle.setId(vehiclesDAO.insert(bicycle));
		bicycleDAO.insert(bicycle);
		log.info("Bicycle added with id {}", bicycle.getId());
	}
	
	/**
	 * Update an existing bicycle by id 
	 * (parameters already merged with the current values)
	 */
	@Transactional
	@Override
	public void update(Map<String, String> parameters) {
		Integer id = Validation.getInt(parameters, "id");
		Bicycle bicycle = build(parameters);
		bicycle.setId(id);
		
		// Preliminary checks on plates for bicycle  (not needed now)
		
		vehiclesDAO.update(bicycle);
		
		// Did I update 1 row successfully? 
		// No rows updated (0) / id doesn't exist / not a bicycle -> rollami back 
		if (bicycleDAO.update(bicycle) == 0)
			throw new CustomException("Bicycle not found with id " + id);
		log.info("Bicycle updated with id {}", id);
	}
	
	@Transactional
	@Override
	public void delete(Map<String, String> parameters) {
		Integer id = Validation.getInt(parameters, "id");
		if (vehiclesDAO.delete(id) == 0)
			throw new CustomException("Bicycle not found with id " + id);
		log.info("Bicycle deleted with id {}", id);
	}
	
	private Bicycle build(Map<String,String> parameters) {
		Bicycle bicycle = new Bicycle();
		fillProperties(bicycle, "bicycle", parameters);
		bicycle.setNumberGears(Validation.getIntInRange(parameters, "gears", 1, 50));
		bicycle.setSuspensions(Validation.getBoolean(parameters, "suspension"));
		bicycle.setFoldable(Validation.getBoolean(parameters, "foldable"));

		if (!bicycle.getFuelType().equals("manual") && !bicycle.getFuelType().equals("electric"))
			throw new CustomException("A bicycle can only be electric or manual");
		if (bicycle.getNumberWheels() != 2)
			throw new CustomException("A bicycle can have only 2 wheels");
		return bicycle;
	}
}