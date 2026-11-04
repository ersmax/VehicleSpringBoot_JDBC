package com.betacom.vehicles.services;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.betacom.vehicles.VehiclesJdbCspringBootApplication;
import com.betacom.vehicles.dao.LookupDAO;
import com.betacom.vehicles.dao.VehiclesDAO;
import com.betacom.vehicles.exception.CustomException;
import com.betacom.vehicles.models.Bicycle;
import com.betacom.vehicles.models.Bike;
import com.betacom.vehicles.models.Car;
import com.betacom.vehicles.models.Vehicles;
import com.betacom.vehicles.process.StartVehicle;
import com.betacom.vehicles.process.StringParsing;
import com.betacom.vehicles.utils.FileManager;
import com.betacom.vehicles.utils.Validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class ListImplementation {
	
	
	// valid values for the categories. Injected with constructor injection
	private final LookupDAO lookupDAO;
	
	// DAO that runs the SQL on the vehicles tables (findAll, FindIdByPlate, delete, etc.)
	private final VehiclesDAO vehiclesDAO;
	
	// Dynamic call: Holds the beans that extend VehicleAbstract (e.g. car -> CarImplementation)
	private final ApplicationContext context;
	
	/**
	 * Add a vehicle: parse the parameters and send them to the proper vehicle type
	 * e.g. add,car,brand=fiat,...
	 * @param parameterList : the parameters list for a row representing the vehicle
	 */
	public void addVehicle(String[] parameterList) {

		if (parameterList.length < StartVehicle.PARAMETERS) 
			throw new CustomException("Missing vehicle type");

		String[] parameters = Arrays.stream(parameterList)
								    .skip(StartVehicle.PARAMETERS)
								    .toArray(String[]::new);

		Map<String, String> keysValues =  StringParsing.mapKeyValue(parameters);
		String vehicle = parameterList[StartVehicle.VEHICLE_TYPE].toLowerCase();

		// add dynamic call + polymorphism
		if (!lookupDAO.findVehicleTypes().contains(vehicle))
			throw new CustomException("Vehicle type not in database: " + vehicle
									+ ". Allowed: " + lookupDAO.findVehicleTypes());
		executeOperation("add", vehicle, keysValues);
	}
	
	/**
	 * update,id=1,color=red,cc=1400   
	 * update,plate=FL234GX,color=red
	 * the vehicle is found by id or plate; only the given fields change
	 */
	public void updateVehicle(String[] parameterList) {
		String[] parameters = Arrays.stream(parameterList)
			    .skip(1)						// drop "update"
			    .toArray(String[]::new);
		
		Map<String, String> newValues = StringParsing.mapKeyValue(parameters);
		if (!newValues.containsKey("id") && !newValues.containsKey("plate"))
			throw new CustomException("Missing parameter: plate or id");
		
		// id from plate, or id given directly
		Integer id = newValues.containsKey("id")
					? Validation.getInt(newValues, "id")
					: vehiclesDAO.findIdByPlate(newValues.get("plate").toUpperCase())
								 .orElseThrow(() -> new CustomException("Vehicle not found: " + newValues));

		Vehicles current = vehiclesDAO.findById(id)
									  .orElseThrow(() -> new CustomException("Vehicle not found with id " + id));

		// current values from db as key-value param (e.g. color=white, cc=1300)
		Map<String, String> merged = toParameters(current);
		merged.putAll(newValues);	// overwrite new values to old ones

		// dynamic call
		merged.put("id", String.valueOf(id));
		executeOperation("update", current.getVehicleType(), merged);
	}
	
	/**
	 * Map the Java Object to Key-Value parameters
	 * Convert a vehicle from database into a map of parameters
	 * with same keys used in "readData.txt" (e.g. color, cc, doors, plate).
	 * Current values are merged with the new ones,
	 * so the update row only needs the fields to change.
	 * @param vehicle	: the vehicle as it is in the database (Car, Bike or Bicycle)
	 * @return			: key-value map ready for build()
	 */
	private Map<String, String> toParameters(Vehicles vehicle) {
		Map<String, String> map = new HashMap<>();
		map.put("wheels",   String.valueOf(vehicle.getNumberWheels()));
		map.put("fuel",     vehicle.getFuelType());
		map.put("category", vehicle.getCategory());
		map.put("color",    vehicle.getColor());
		map.put("brand",    vehicle.getBrand());
		map.put("year",     String.valueOf(vehicle.getProductionYear()));
		map.put("model",    vehicle.getModel());
		
		if (vehicle.getNumberGears() != null)
			map.put("gears", String.valueOf(vehicle.getNumberGears()));

		if (vehicle instanceof Car car) {
			map.put("plate", car.getPlate());
			map.put("cc",    String.valueOf(car.getCc()));
			map.put("doors", String.valueOf(car.getNumberDoors()));
		} else if (vehicle instanceof Bike bike) {
			map.put("plate", bike.getPlate());
			map.put("cc",    String.valueOf(bike.getCc()));
		} else if (vehicle instanceof Bicycle bicycle) {
			map.put("suspension", bicycle.getSuspensions() ? "yes" : "no");
			map.put("foldable",   bicycle.getFoldable() ? "yes" : "no");
		}
		return map;
	}

	/**
	 * Dynamic call: bean by name (car -> carImplementation), method by name (add, update, delete)
	 */
	private void executeOperation(String operation, String vehicleType, Map<String, String> parameters) {
		String componentName = vehicleType + "Implementation";
		if (!context.containsBean(componentName))
			throw new CustomException("Vehicle type " + vehicleType + " not supported");
		
		// VehicleOperations.class tells Spring the type to return, 
		// so we get a VehicleOperations without a cast. 
		// Spring also checks that the bean really implements that interface, 
		// and throws an error if it doesn’t.
		VehicleOperations component = context.getBean(componentName, VehicleOperations.class);
		try {
			Method method = component.getClass().getMethod(operation, Map.class);
			method.invoke(component, parameters);
		} catch (NoSuchMethodException e) {
			throw new CustomException("Operation not found: " + operation);
		} catch (IllegalAccessException e) {
			throw new CustomException("Illegal access: " + e.getMessage());
		} catch (InvocationTargetException e) {
			throw new CustomException(e.getCause().getMessage());
		}
	}
	
	/**
	 * Delete a vehicle by plate or by id, whatever its type
	 * e.g. delete,plate=EL234GX  |  delete,id=4
	 * @param parameterList : "delete" followed by plate=... or id=...
	 */
	public void deleteVehicle(String[] parameterList) {
		String[] parameters = Arrays.stream(parameterList)
									.skip(1)					// drop "delete"
									.toArray(String[]::new);
		Map<String, String> keysValues = StringParsing.mapKeyValue(parameters);

		if (keysValues.isEmpty())
			throw new CustomException("Missing parameter: plate or id");

		
		Integer id = keysValues.containsKey("plate")
					? vehiclesDAO.findIdByPlate(keysValues.get("plate").toUpperCase())
								 .orElseThrow(() -> new CustomException("Vehicle not found: " + keysValues))
					: Validation.getInt(keysValues, "id");
		
		// dynamic call
		Vehicles current = vehiclesDAO.findById(id)
				  .orElseThrow(() -> new CustomException("Vehicle not found with id " + id));
		executeOperation("delete", current.getVehicleType(), Map.of("id", String.valueOf(id)));
	}
	
	/**
	 * Print all vehicles, or only those matching the filters
	 * e.g. list  |  list,color=white  |  list,color=white,brand=fiat
	 */
	public void listVehicles(String[] parameterList) throws IOException {
		String[] rawFilters = Arrays.stream(parameterList)
									 .skip(1)					// everything after "list" is filter
									 .toArray(String[]::new);

		Map<String, String> filters = StringParsing.mapKeyValue(rawFilters);
		validateFilters(filters);
		
		List<String> vehicles = vehiclesDAO.findByFilters(filters)
								 .stream()
								 .map(Vehicles::toString)
								 .toList();
		saveVehicles(vehicles, parameterList);
	}
	
	/**
	 * Check the validity of filters before querying the database.
	 * Names and types come from VehiclesDAO.FILTERS, so each filter is defined only once.
	 * @param filters	: key-value filters from the list row
	 */
	private void validateFilters(Map<String, String> filters) {
		for (String key : filters.keySet()) {
			String type = VehiclesDAO.FILTERS.get(key);
			if (type == null)
				throw new CustomException("Unknown filter: " + key
										+ ". Allowed: " + VehiclesDAO.FILTERS.keySet());
			switch (type) {
				case "I" -> Validation.getInt(filters, key);		// "Parameter year must be a number"
				case "B" -> Validation.getBoolean(filters, key);	// "Parameter foldable must be yes/no"
				default  -> Validation.getString(filters, key);		// "Missing parameter" (e.g. color=)
			}
		}
	}
	
	/**
	 * Save in the external file the vehicles that satisfy the filter
	 * @param vehicleList	: vehicles that satisfy the filter
	 * @param parameterList	: the only attribute to filter the list
	 * @throws IOException	: if failed to write on a file
	 */
	private void saveVehicles(List<String> vehicleList, String[] parameterList) throws IOException {
		String title = String.join(",", parameterList);

		List<String> output = new ArrayList<>();
		output.add("### " + title + " (" + vehicleList.size() + " found) ###");
		output.addAll(vehicleList);
		output.add("\n");

		FileManager.writeFile(VehiclesJdbCspringBootApplication.OUTPUT_FILE, output, true);
		log.info("Number of written record(s): {}", vehicleList.size());
	}
	
}
