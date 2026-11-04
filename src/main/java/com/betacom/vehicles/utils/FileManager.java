package com.betacom.vehicles.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

public class FileManager {

	public static List<String> readFile(String path) throws IOException {
		
		try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
			Stream<String> streamBuilder = reader.lines();
			return streamBuilder.filter(line -> !line.isBlank())
								.filter(line -> !line.trim().startsWith("/"))		// drop comment lines
								.toList();
			}
		}

	/**
	 * Replace or add new rows to the File
	 * @param path	: the file path where to write
	 * @param input	: the list of new rows
	 * @param append	: true = extend file | false = replace
	 * @return
	 */
	public static int writeFile(String path, List<String> input, boolean append) throws IOException {
		int rows = 0;
		try (FileWriter writer = new FileWriter(path, append)) {
			for (String record : input) {
				writer.write(record + '\n');
				rows++;
			}
		}
		return rows;
	}

}
