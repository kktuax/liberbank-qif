package com.gmail.maxilandia.liberbank;

import gnu.qif.OpeningBalanceRecord;
import gnu.qif.RecordArray;

import java.io.File;
import java.io.FileNotFoundException;

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

import com.gmail.maxilandia.liberbank.Record.RecordParser;
import com.google.common.base.Charsets;
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
					File file = new File(inFolderFile, inFileName);
					RecordParser recordParser = createRecordParser(file);
					boolean firstLine = true;
					for(Record lineRecord : recordParser.parseRecords(file)){
						if(lineRecord.getSubject().startsWith("REFUND.")){
							continue;
						}
						if(firstLine){
							OpeningBalanceRecord record = new OpeningBalanceRecord(
								accountName, 
								lineRecord.getDate(), 
								Double.valueOf(lineRecord.getBalance()-lineRecord.getAmount()).floatValue()
							);
							recordArray.addRecord(record);
							firstLine = false;
						}
						recordArray.addRecord(lineRecord.asBankTransaction(accountName));
					}
					File outFile = new File(outFolder, FilenameUtils.removeExtension(inFileName) + ".qif");
					LOGGER.info("Dumping result to {}", outFile);
					Files.write(recordArray.toString(), outFile, Charsets.UTF_8);
				}
				return RepeatStatus.FINISHED;
			}
		};
	}
	
	private Record.RecordParser createRecordParser(File file){
		if("csv".equalsIgnoreCase(FilenameUtils.getExtension(file.getAbsolutePath()))){
			return new CsvRecordParser();
		}else{
			return new HtmlRecordParser();
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
	
	private static final Logger LOGGER = LoggerFactory.getLogger(App.class);
	
}
