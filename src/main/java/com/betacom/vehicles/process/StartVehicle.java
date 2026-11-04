package com.betacom.vehicles.process;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.betacom.vehicles.VehiclesJdbCspringBootApplication;
import com.betacom.vehicles.enums.Operation;
import com.betacom.vehicles.exception.CustomException;
import com.betacom.vehicles.services.ListImplementation;
import com.betacom.vehicles.utils.FileManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class StartVehicle {
	
	
	public static final int COMMAND = 0;
	public static final int VEHICLE_TYPE = 1;
	public static final int PARAMETERS = 2; 
	
	// Service injected with constructor injection
	private final ListImplementation service;
	
	/**
	 * Clear writing file, Read rows and process each single row
	 */
	public void execute() {
		try {
			FileManager.writeFile(VehiclesJdbCspringBootApplication.OUTPUT_FILE, new ArrayList<>(), false);
			List<String> rows = FileManager.readFile(VehiclesJdbCspringBootApplication.INPUT_FILE);
			processRows(rows);
		} catch (IOException e) {
			log.error("Error reading the input file: {}", e.getMessage());
		}
	}
	
	/**
	 * Decode the elements list, row by row,
	 * get the operation (COMMAND), 
	 * then execute each service 
	 */
	private void processRows(List<String> rows) {		
		int idxRow = 0;
		for (String row : rows) {
			idxRow++;
			try {
				String[] elementsRow = StringParsing.rowParser(row);
				Operation operation = getOperation(elementsRow[COMMAND]);
				handleOperation(operation, elementsRow);
			} catch (CustomException e) {
				log.error("Row {} not added due to: {}", idxRow, e.getMessage());
			}
		}
	}
	
	/**
	 * Transform the String parameter into enum
	 * @param operation	: the operation passed as a string
	 * @return			: the Enum of the operation
	 */
	private Operation getOperation(String operation) {
		try {
			return Operation.valueOf(operation.trim().toUpperCase());
		} catch (NullPointerException e) {
			throw new CustomException("Missing operation");
		} catch (IllegalArgumentException e) {
			throw new CustomException("Invalid operation: '" + operation + "'");
		}
	}
	
	/**
	 * Dynamic calls (no switch for ADD -> addVehicle, UPDATE -> updateVehicle, 
	 * 								DELETE -> deleteVehicle, LIST -> listVehicles)
	 * Also dynamic: CarImplementation, BikeImplementation, BicycleImplementation
	 * 	- for update, delete, insert
	 * Dispatch to the correct operation handling (etc addVehicle), or handle errors.
	 * The operation is in ListImplementation
	 * @param operation			: the operation code in Enum format
	 * @param parametersList	: the list of parameters in a row
	 */
	private void handleOperation(Operation operation, String[] parametersList) {
		try {
			Method method = service.getClass().getMethod(operation.getMethodName(), String[].class);
			method.invoke(service, (Object) parametersList);
		} catch (SecurityException e) {
			throw new CustomException("Security error: " + e.getMessage());
		} catch (NoSuchMethodException e) {
			throw new CustomException("Operation not supported: " + operation);
		} catch (IllegalAccessException e) {
			throw new CustomException("Illegal access: " + e.getMessage());
		} catch (IllegalArgumentException e) {
			throw new CustomException("Illegal argument: " + e.getMessage());
		} catch (InvocationTargetException e) {
			throw new CustomException(e.getCause().getMessage());
		}
	}
}

/*
 * Clarification on InvocationTargetException:
	 method.invoke can’t know at compile time what the method throws
	 so if the method invoked started, the call succeed
	 but if the method fails, it throws an exception.
	 It might throw IOException, SQLException, nothing, or anything else. 
	 invoke can’t declare all possible exceptions. 
	 So it declares one fixed type, InvocationTargetException, 
	 which means “the method ran and threw something, and here it is inside”. 
	 That keeps checked exceptions consistent: 
	 you’re still forced to handle a possible error, just under one name.
	 So, strictly speaking, InvocationTargetException doesn’t catch the IOException. 
	 It carries it: invoke catches whatever the method throws, puts it inside 
	 a new InvocationTargetException, and throws that. 
	 The original is still there, in getCause().
	 getCause() gives back the original IOException or CustomException, 
	 so the log shows the real message and not “InvocationTargetException”.
 */
