package com.betacom.vehicles.services;

import java.time.Year;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.betacom.vehicles.dao.LookupDAO;
import com.betacom.vehicles.models.Vehicles;
import com.betacom.vehicles.utils.Validation;

public abstract class VehicleAbstract implements VehicleOperations {

	// injected in every subclass (CarImplementation, BikeImplementation, BicycleImplementation)
	@Autowired
	protected LookupDAO lookupDAO;
	
	/**
	 * Fill in common attributes for all vehicles type. Allowed values are read from the lookup tables
	 * @param vehicle	 : the specific object of vehicle for which we add common attributes
	 * @param type		 : the type of vehicle
	 * @param parameters : the list of attributes for the object
	 */
	protected void fillProperties(Vehicles vehicle, String type, Map<String, String> parameters) {

		vehicle.setVehicleType(type);
		vehicle.setBrand(Validation.getOneOf(parameters, "brand", lookupDAO.findBrands()));
		vehicle.setCategory(Validation.getOneOf(parameters, "category", lookupDAO.findCategories()));
		vehicle.setColor(Validation.getOneOf(parameters, "color", lookupDAO.findColors()));
		vehicle.setFuelType(Validation.getOneOf(parameters, "fuel", lookupDAO.findFuelTypes()));
		vehicle.setModel(Validation.getString(parameters, "model"));
		vehicle.setNumberWheels(Validation.getInt(parameters, "wheels"));
		vehicle.setProductionYear(Validation.getIntInRange(parameters, "year", Year.now().getValue() - 50, Year.now().getValue()));
	}
}
