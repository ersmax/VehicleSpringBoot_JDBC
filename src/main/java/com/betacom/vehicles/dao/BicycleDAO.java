package com.betacom.vehicles.dao;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import com.betacom.vehicles.models.Bicycle;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Repository
public class BicycleDAO {

	private final NamedParameterJdbcTemplate jdbcTemplate;

	public int insert(Bicycle bicycle) {
		String sql = """
				INSERT INTO bicycles
				(id_vehicle, suspension, foldable)
				VALUES(:idVehicle, :suspension, :foldable)
				""";
		SqlParameterSource param = new MapSqlParameterSource()
				.addValue("idVehicle", bicycle.getId())
				.addValue("suspension", bicycle.getSuspensions())
				.addValue("foldable", bicycle.getFoldable());
		return jdbcTemplate.update(sql, param);
	}
	
	public int update(Bicycle bicycle) {
		String sql = """
				UPDATE bicycles
				SET 
					suspension		 = :suspensions,
					foldable		 = :foldable
					
				WHERE id_vehicle	 = :idVehicle
				""";
		SqlParameterSource param = new MapSqlParameterSource()
				.addValue("suspensions",	bicycle.getSuspensions())
				.addValue("foldable",		bicycle.getFoldable())
				.addValue("idVehicle",		bicycle.getId());
		return jdbcTemplate.update(sql, param);
	}
}
