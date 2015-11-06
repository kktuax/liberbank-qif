package com.gmail.maxilandia.liberbank;

import gnu.qif.AccountRecord;
import gnu.qif.BankTransaction;
import gnu.qif.QIFRecord;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

public class Record extends QIFRecord {

	private Date date;
	private String subject;
	private Double amount, balance;
	
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	public String getSubject() {
		return subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}
	public Double getAmount() {
		return amount;
	}
	public void setAmount(Double amount) {
		this.amount = amount;
	}
	public Double getBalance() {
		return balance;
	}
	public void setBalance(Double balance) {
		this.balance = balance;
	}
	
	public AccountRecord asAccountRecord(String accountName){
		AccountRecord record = new AccountRecord(accountName);
		record.setDescription(subject);
		record.setStatementBalanceDate(encodeDate(date));
		record.setStatementBalanceAmount(balance.toString());
		return record;
	}
	
	public BankTransaction asBankTransaction(String accountName){
		BankTransaction record = new BankTransaction();
		record.setCategory(subject);
		record.setDate(date);
		record.setTotal(amount.floatValue());
		return record;
	}
	
	public interface RecordParser{
		
		public List<Record> parseRecords(File ifile) throws ParseException, IOException;
		
	}
	
}
