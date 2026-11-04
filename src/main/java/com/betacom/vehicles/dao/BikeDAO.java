package com.betacom.vehicles.dao;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import com.betacom.vehicles.models.Bike;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Repository
public class BikeDAO {

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public int insert(Bike bike) {
		String sql = """
				INSERT INTO bikes
				(id_vehicle, plate, cc)
				VALUES(:idVehicle, :plate, :cc)
				""";
		SqlParameterSource param = new MapSqlParameterSource()
				.addValue("idVehicle", bike.getId())
				.addValue("plate", bike.getPlate())
				.addValue("cc", bike.getCc());
		return jdbcTemplate.update(sql, param);
	}
	
	public int update(Bike bike) {
		String sql = """
				UPDATE bikes
				SET 
					plate			 = :plate,
					cc				 = :cc
									
				WHERE id_vehicle	 = :idVehicle
				""";
		SqlParameterSource param = new MapSqlParameterSource()
				.addValue("plate",			bike.getPlate())
				.addValue("cc",				bike.getCc())
				.addValue("idVehicle",		bike.getId());
		return jdbcTemplate.update(sql, param);
	}
}
