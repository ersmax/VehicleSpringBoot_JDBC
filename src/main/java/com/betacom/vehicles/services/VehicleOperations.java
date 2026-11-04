package com.betacom.vehicles.services;

import java.util.Map;

public interface VehicleOperations {
	void add(Map<String, String> parameters);
	void update(Map<String, String> parameters);	// parameters contain "id"
	void delete(Map<String, String> parameters);	// parameters contain "id"
}
