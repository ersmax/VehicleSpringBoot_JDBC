package com.betacom.vehicles.process;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import com.betacom.vehicles.exception.CustomException;

public class StringParsing {
	
	/**
	 * Separate a string of elements, using "," as separator 
	 * @param row	: a single row of elements
	 * @return		: an array of String elements 
	 */
	public static String[] rowParser(String row) {
		
		return 	Arrays.stream(row.split(","))
				      .map(String::trim)
					  .toArray(String[]::new);
	}
	
	/**
	 * Turns the array (e.g. [color=yellow], [cc=1000], [year=2020]...) into pairs Key-Value
	 * @param parameters : the array of items to be transformed into Key-Value pairs
	 * @return			 : the map of key-value attributes 
	 */
	public static Map<String, String> mapKeyValue(String[] parameters) {
		try {
			// turns the stream into a map
			return Arrays.stream(parameters)
						 .map(StringParsing::splitKeyValue)						// "a=b"-> {"a","b"}
						 .collect(Collectors.toMap(keyValue -> keyValue[0], 	// key
								 				   keyValue -> keyValue[1]));	// value
		} catch (IllegalStateException e) {
			throw new CustomException("Duplicate parameter "
									+ "(attribute or key already present): " 
									+ e.getMessage());
		}	
	}
	
	/**
	 * Helper function that split single attribute and value (e.g. color=yellow)
	 * @param parameter	: the single pair attribute and value
	 * @return			: an array of size 2 of Strings
	 */
	public static String[] splitKeyValue(String parameter) {
		String[] keyValue = parameter.split("=", 2);
		if (keyValue.length < 2 || keyValue[0].trim().isEmpty())
			throw new CustomException("Invalid parameter (expected Key=Value). "
									+ "Found instead: '" + parameter + "'");
		
		String key = keyValue[0].trim().toLowerCase();
		String value = keyValue[1].trim();
		return new String[] {key, value};
	}

}
