package com.zenobase.io;

import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.csv.CsvGenerator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.List;

/** RFC 4180 CSV with every field quoted and {@code \n} line endings. */
public final class Csv {

	private static final CsvMapper MAPPER = new CsvMapper();

	private static final ObjectWriter WRITER = MAPPER.writerFor(String[].class)
		.with(CsvSchema.emptySchema())
		.withFeatures(CsvGenerator.Feature.ALWAYS_QUOTE_STRINGS, CsvGenerator.Feature.ALWAYS_QUOTE_EMPTY_STRINGS)
		.without(SerializationFeature.FLUSH_AFTER_WRITE_VALUE);

	private static final ObjectReader READER = MAPPER.readerFor(String[].class).with(CsvParser.Feature.WRAP_AS_ARRAY);

	private Csv() {}

	public static SequenceWriter newWriter(Writer out) throws IOException {
		return WRITER.writeValues(out);
	}

	public static List<String[]> readAll(Reader in) throws IOException {
		try (var rows = READER.<String[]>readValues(in)) {
			return rows.readAll();
		}
	}
}
