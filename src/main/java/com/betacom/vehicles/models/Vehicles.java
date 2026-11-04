package com.betacom.vehicles.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@ToString
@SuperBuilder
@NoArgsConstructor
public class Vehicles {
	private Integer id;				// unique id of object
	private String 	vehicleType;	// car, bike, bicycle
	private Integer numberWheels;	// depends on the vehicle
	private String 	fuelType;		// petrol, diesel, lpg, methane, electric, hybrid, manual
	private String 	category;		// road, citycar, utility, sedan, suv, sports, offroad, racing
	private String 	color;
	private String 	brand;
	private Integer productionYear;
	private String 	model;
	private Integer numberGears;
}
