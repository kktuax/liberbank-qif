package com.gmail.maxilandia.liberbank;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.google.common.base.Charsets;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;

public class HtmlRecordParser implements Record.RecordParser {

	@Override
	public List<Record> parseRecords(File ifile) throws ParseException, IOException{
		List<Record> records = new ArrayList<Record>();
		Document doc = Jsoup.parse(ifile, Charsets.ISO_8859_1.displayName());
		Element table = Iterators.getLast(doc.select("table").listIterator());	
		ListIterator<Element> rowIt = table.select("tbody").select("tr").listIterator();
		while(rowIt.hasNext()){
			Element row = rowIt.next();
			ListIterator<Element> cellIt = row.select("td").listIterator();
			int i = 0;
			Record record = new Record();
			while(cellIt.hasNext()){
				Element cell = cellIt.next();
				if(i == 0){
					record.setDate(DATE_FORMAT.parse(cell.text()));
				}else if(i == 2){
					record.setSubject(cell.text());
				}else if(i == 3){
					record.setAmount(DECIMAL_FORMAT.parse(cell.text()).doubleValue());
				}else if(i == 4){
					record.setBalance(DECIMAL_FORMAT.parse(cell.text()).doubleValue());
				}
				i++;
			}
			records.add(record);
		}
		return Lists.reverse(records);
	}
	
	private static final DecimalFormat DECIMAL_FORMAT = (DecimalFormat) NumberFormat.getNumberInstance(new Locale("Es"));
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");
	
}
