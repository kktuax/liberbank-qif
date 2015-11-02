package com.gmail.maxilandia.liberbank;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.common.base.Charsets;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

public class CsvRecordParser implements Record.RecordParser {

	@Override
	public List<Record> parseRecords(File ifile) throws ParseException, IOException{
		List<Record> records = new ArrayList<Record>();
		List<String> lines = FluentIterable
			.from(Files.readLines(ifile, Charsets.UTF_8))
			.skip(3)
			.toList();
		for(String line : Lists.reverse(lines)){
			records.add(fromCsvLine(line));
		}
		return records;
	}
	
	private static Record fromCsvLine(String line) throws ParseException{
		List<String> lineEls = Lists.newArrayList(line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1));
		Record record = new Record();
		record.setDate(DATE_FORMAT.parse(removeQuotes(lineEls.get(0))));
		record.setSubject(removeQuotes(lineEls.get(2)));
		record.setAmount(DECIMAL_FORMAT.parse(removeQuotes(lineEls.get(3))).doubleValue());
		record.setBalance(DECIMAL_FORMAT.parse(removeQuotes(lineEls.get(4))).doubleValue());
		return record;
	}

	private static String removeQuotes(String in){
		if(in.startsWith("\"") && in.endsWith("\"")){
			return in.substring(1, in.length()-1);
		}else{
			return in;
		}
	}
	
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");
	private static final DecimalFormat DECIMAL_FORMAT = (DecimalFormat) NumberFormat.getNumberInstance(new Locale("Es"));
	
}
