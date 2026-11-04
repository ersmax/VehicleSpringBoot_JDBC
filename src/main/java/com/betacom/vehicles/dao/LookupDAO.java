package com.betacom.vehicles.dao;

import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Repository
public class LookupDAO {

	private final NamedParameterJdbcTemplate jdbcTemplate;
	
	/*
	 * allowed values read from lookup tables
	 */
	public List<String> findVehicleTypes() {
		String sql = "select description from vehicle_types order by id_vehicle_type";
		return jdbcTemplate.queryForList(sql, new MapSqlParameterSource(), String.class);
	}
	
	public List<String> findFuelTypes() {
		String sql = "select description from fuel_types order by id_fuel_type";
		return jdbcTemplate.queryForList(sql, new MapSqlParameterSource(), String.class);
	}
	
	public List<String> findCategories() {
		String sql = "select description from categories order by id_category";
		return jdbcTemplate.queryForList(sql, new MapSqlParameterSource(), String.class);
	}
	
	public List<String> findColors() {
		String sql = "select description from colors order by id_color";
		return jdbcTemplate.queryForList(sql, new MapSqlParameterSource(), String.class);
	}
	
	public List<String> findBrands() {
		String sql = "select description from brand_types order by id_brand_type";
		return jdbcTemplate.queryForList(sql, new MapSqlParameterSource(), String.class);
	}
}
