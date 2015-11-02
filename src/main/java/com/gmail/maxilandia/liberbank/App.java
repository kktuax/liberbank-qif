package com.gmail.maxilandia.liberbank;

import gnu.qif.BankTransaction;
import gnu.qif.OpeningBalanceRecord;
import gnu.qif.RecordArray;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.google.common.base.Charsets;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

@SpringBootApplication
@EnableBatchProcessing
public class App {

	@Bean
	protected Tasklet tasklet() {
		return new Tasklet() {
			@Override
			public RepeatStatus execute(StepContribution contribution, ChunkContext context) throws Exception {
				File inFolderFile = new File(inFolder);
				if(!inFolderFile.isDirectory()){
					throw new FileNotFoundException();	
				}
				String[] inPaths = inFolderFile.list(new RegexFileFilter(inRegexp));
				for(String inFileName : inPaths){
					RecordArray recordArray = new RecordArray();
					LOGGER.info("Processing file {}", inFileName);
					List<String> lines = FluentIterable
						.from(Files.readLines(new File(inFolderFile, inFileName), Charsets.UTF_8))
						.skip(3)
						.toList();
					boolean firstLine = true;
					for(String line : Lists.reverse(lines)){
						List<String> lineEls = Lists.newArrayList(line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1));
						Date date = DATE_FORMAT.parse(removeQuotes(lineEls.get(0)));
						String subject = removeQuotes(lineEls.get(2));
						Double amount = DECIMAL_FORMAT.parse(removeQuotes(lineEls.get(3))).doubleValue();
						Double balance = DECIMAL_FORMAT.parse(removeQuotes(lineEls.get(4))).doubleValue();
						if(firstLine){
							OpeningBalanceRecord record = new OpeningBalanceRecord(
								accountName, 
								date, 
								Double.valueOf(balance-amount).floatValue()
							);
							recordArray.addRecord(record);
							firstLine = false;
						}
						recordArray.addRecord(createBankTransaction(date, subject, amount));
						
					}
					File outFile = new File(outFolder, FilenameUtils.removeExtension(inFileName) + ".qif");
					LOGGER.info("Dumping result to {}", outFile);
					Files.write(recordArray.toString(), outFile, Charsets.UTF_8);
				}
				return RepeatStatus.FINISHED;
			}
		};
	}

	private BankTransaction createBankTransaction(Date date, String subject, Number amount){
		BankTransaction record = new BankTransaction();
		record.setAccount(accountName);
		record.setCategory(subject);
		record.setDate(date);
		record.setTotal(amount.floatValue());
		return record;
	}
	
	private static String removeQuotes(String in){
		if(in.startsWith("\"") && in.endsWith("\"")){
			return in.substring(1, in.length()-1);
		}else{
			return in;
		}
	}
	
	@Bean
	public Job job() throws Exception {
		return this.jobs.get("job").start(step1()).build();
	}

	@Bean
	protected Step step1() throws Exception {
		return this.steps.get("step1").tasklet(tasklet()).build();
	}

	public static void main(String[] args) throws Exception {
		System.exit(SpringApplication.exit(SpringApplication.run(App.class, args)));
	}
	
	@Value("${in.folder}") private String inFolder;
	@Value("${in.regexp}") private String inRegexp;
	@Value("${out.folder}") private String outFolder;
	@Value("${account.name}") private String accountName;
	
	@Autowired private JobBuilderFactory jobs;
	@Autowired private StepBuilderFactory steps;
	
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");
	private static final DecimalFormat DECIMAL_FORMAT = (DecimalFormat) NumberFormat.getNumberInstance(new Locale("Es"));

	private static final Logger LOGGER = LoggerFactory.getLogger(App.class);
	
}
