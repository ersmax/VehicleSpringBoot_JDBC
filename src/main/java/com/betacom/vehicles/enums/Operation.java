package com.betacom.vehicles.enums;

public enum Operation {
	ADD("addVehicle"),
	UPDATE("updateVehicle"),
	DELETE("deleteVehicle"),
	LIST("listVehicles");

	private final String methodName;

	private Operation(String methodName) {
		this.methodName = methodName;
	}

	public String getMethodName() {
		return methodName;
	}
}