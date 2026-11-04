package com.betacom.vehicles.dao;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import com.betacom.vehicles.models.Car;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Repository
public class CarDAO {

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public int update(Car car) {
		String sql = """
		        UPDATE cars
		        SET
		            plate = :plate,
		            cc = :cc,
		            number_doors = :numberDoors
		            
		        WHERE id_vehicle = :idVehicle
		        """;
		SqlParameterSource param = new MapSqlParameterSource()
				.addValue("idVehicle", car.getId())
				.addValue("plate", car.getPlate())
				.addValue("cc", car.getCc())
				.addValue("numberDoors", car.getNumberDoors());
		
		return jdbcTemplate.update(sql, param);
	}

	
	/*
	 * id_vehicle must already exist in vehicles (same id as the parent)
	 */
	public int insert(Car car) {
		String sql = """
				INSERT INTO cars
				(id_vehicle, plate, cc, number_doors)
				VALUES(:idVehicle, :plate, :cc, :numberDoors)
				""";
		SqlParameterSource param = new MapSqlParameterSource()
				.addValue("idVehicle", car.getId())
				.addValue("plate", car.getPlate())
				.addValue("cc", car.getCc())
				.addValue("numberDoors", car.getNumberDoors());
		return jdbcTemplate.update(sql, param);
	}
}