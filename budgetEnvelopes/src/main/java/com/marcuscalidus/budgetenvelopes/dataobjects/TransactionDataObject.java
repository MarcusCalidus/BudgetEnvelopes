package com.marcuscalidus.budgetenvelopes.dataobjects;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.SparseArray;
import android.app.Activity;

import com.marcuscalidus.budgetenvelopes.BudgetEnvelopes;
import com.marcuscalidus.budgetenvelopes.db.DBMain;
import com.marcuscalidus.budgetenvelopes.transactions.TransactionsMonth;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class TransactionDataObject extends BaseDataObject {

	public TransactionDataObject(Context context, Cursor c) {
		super(context, c);
	}

	public static String TABLENAME = "TRANSACTIONS";
	
	public static String FIELDNAME_TEXT = "TEXT";
	public static String FIELDNAME_AMOUNT = "AMOUNT";
	public static String FIELDNAME_TIMESTAMP = "TIMESTAMP";
	public static String FIELDNAME_FROM_ENVELOPE = "FROM_ENVELOPE";
	public static String FIELDNAME_TO_ENVELOPE = "TO_ENVELOPE";
    public static String FIELDNAME_PENDING = "PENDING";
    public static String FIELDNAME_ATTACHMENT = "ATTACHMENT";

    private String _Text;
	private Double _Amount;
	private Calendar _Timestamp;
	private UUID _FromEnvelope;
	private UUID _ToEnvelope;
	private boolean _Pending;
    private String _Attachment;
	
	@Override
	protected void initializeFromCursor(Cursor c) {
        _Text = c.getString(c.getColumnIndex(FIELDNAME_TEXT));
		_Amount = c.getInt(c.getColumnIndex(FIELDNAME_AMOUNT)) / 100.0;
		_Pending =  c.getInt(c.getColumnIndex(FIELDNAME_PENDING)) != 0;
        _Attachment = c.getString(c.getColumnIndex(FIELDNAME_ATTACHMENT));
		_FromEnvelope = castBlobAsUUID(c.getBlob(c.getColumnIndex(FIELDNAME_FROM_ENVELOPE)));
		_ToEnvelope = castBlobAsUUID(c.getBlob(c.getColumnIndex(FIELDNAME_TO_ENVELOPE)));
		_Timestamp = Calendar.getInstance();
		_Timestamp.setTimeInMillis(c.getLong(c.getColumnIndex(FIELDNAME_TIMESTAMP))); 
	}

	@Override
	public String getTableName() {
		return TABLENAME;
	}

	@Override
	protected String[] getFieldNames() {
		return new String[] {FIELDNAME_AMOUNT, FIELDNAME_TEXT, FIELDNAME_FROM_ENVELOPE, FIELDNAME_TO_ENVELOPE, FIELDNAME_TIMESTAMP, FIELDNAME_PENDING, FIELDNAME_ATTACHMENT};
	}

	@Override
	protected ContentValues getContentValues() {
		ContentValues vals = new ContentValues();
		vals.put(FIELDNAME_AMOUNT, Math.round(_Amount * 100));
		vals.put(FIELDNAME_FROM_ENVELOPE, castUUIDAsBlob(getFromEnvelope()));
		vals.put(FIELDNAME_TO_ENVELOPE, castUUIDAsBlob(getToEnvelope()));
		vals.put(FIELDNAME_PENDING, _Pending);
		vals.put(FIELDNAME_TEXT, _Text);
		vals.put(FIELDNAME_TIMESTAMP, _Timestamp.getTimeInMillis());
        vals.put(FIELDNAME_ATTACHMENT, _Attachment);
		return vals;
	}

	public String getText() {
		return _Text;
	}

	public void setText(String _Text) {
		this._Text = _Text;
	}

	public Double getAmount() {
		return _Amount;
	}

	public void setAmount(Double _Amount) {
		this._Amount = _Amount;
	}

	public Calendar getTimestamp() {
		return _Timestamp;
	}

	public void setTimestamp(Date _Timestamp) {
		if (this._Timestamp == null)
			this._Timestamp = Calendar.getInstance();
		
		this._Timestamp.setTime(_Timestamp);
	}

	public UUID getFromEnvelope() {
		return _FromEnvelope;
	}

	public void setFromEnvelope(UUID _FromEnvelope) {
		this._FromEnvelope = _FromEnvelope;
	}

	public UUID getToEnvelope() {
		return _ToEnvelope;
	}

	public void setToEnvelope(UUID _ToEnvelope) {
		this._ToEnvelope = _ToEnvelope;
	}

	public boolean isPending() {
		return _Pending;
	}

	public void setPending(boolean _Pending) {
		this._Pending = _Pending;
	}

    public boolean hasAttachment() { return _Attachment != null; }

    public void setAttachment(String fileName) { this._Attachment = fileName; }

    public String getAttachment() { return _Attachment; }

	public static Date getMinimumDate(SQLiteDatabase db) {
		String selectQuery = "SELECT min("+FIELDNAME_TIMESTAMP+") FROM " + TABLENAME +
				" where coalesce("+FIELDNAME_DELETED+",0)=0";
		 
	     Cursor c = db.rawQuery(selectQuery, null);
	 
	      if (c.moveToFirst()) {
	    	  if (!c.isNull(0)) {
	    		  Calendar cal = Calendar.getInstance();
	    		  cal.setTimeInMillis(c.getLong(0));
	    		  return cal.getTime();
	    	  }
	      }	    	  
	      
	      return new java.util.Date(); 
	}
	
	public static Date getMaximumDate(SQLiteDatabase db) {
		String selectQuery = "SELECT max("+FIELDNAME_TIMESTAMP+") FROM " + TABLENAME +
				" where coalesce("+FIELDNAME_DELETED+",0)=0";
		 
	     Cursor c = db.rawQuery(selectQuery, null);
	 
	      if (c.moveToFirst()) {
	    	  if (!c.isNull(0)) {
	    		  Calendar cal = Calendar.getInstance();
	    		  cal.setTimeInMillis(c.getLong(0));
	    		  return cal.getTime();
	    	  }
	      }	    	  
	      
	      return new java.util.Date(); 
	}
	
	public static TransactionDataObject getById(Context context, SQLiteDatabase db, UUID uuid) {
		 String selectQuery = "SELECT * FROM " + TABLENAME +" where hex(ID) = '"+castUUIDAsHexString(uuid)+"'";
		 
	     Cursor c = db.rawQuery(selectQuery, null);
	 
	      if (c.moveToFirst()) {
	    	  return new TransactionDataObject(context, c);
	      }	    	  
	      
	      return null; 
	}
	
	public static List<TransactionDataObject> getTransactionsBetween(Context context, SQLiteDatabase db, EnvelopeDataObject envelope, Calendar fromDate, Calendar toDate) {
        List<TransactionDataObject> transactions = new ArrayList<TransactionDataObject>();
        
      //  Calendar cal = Calendar.getInstance();
      //  cal.setTime(fromDate);
        String fromDateString = String.valueOf(fromDate.getTimeInMillis());
     //   cal.setTime(toDate);
        String toDateString = String.valueOf(toDate.getTimeInMillis());
        
        String selectQuery = "select * from "+TABLENAME+
        		             " where (      hex("+FIELDNAME_FROM_ENVELOPE+") = '"+castUUIDAsHexString(envelope.getId())+"'"+
        		             "   		 or hex("+FIELDNAME_TO_ENVELOPE+") = '"+castUUIDAsHexString(envelope.getId())+"')"+
        		             " and "+FIELDNAME_TIMESTAMP+" >= "+fromDateString+
        		             " and "+FIELDNAME_TIMESTAMP+" <= "+toDateString+
        		             " and coalesce("+FIELDNAME_FROM_ENVELOPE+",0)<>coalesce("+FIELDNAME_TO_ENVELOPE+",0)"+
        		             " and coalesce("+FIELDNAME_DELETED+", 0) = 0"+ 
        		             " order by "+FIELDNAME_TIMESTAMP;
        
        Cursor c = db.rawQuery(selectQuery, null);
 
        if (c.moveToFirst()) {
            do {
            	TransactionDataObject tdo = new TransactionDataObject(context, c);
                transactions.add(tdo);
            } while (c.moveToNext());
        }
 
        return transactions;
    }

	public static int KEY_EXPENSE = 0;
	public static int KEY_INCOME = 1;
	
	public static SparseArray<Double> getIncomeExpenseDetailBetween(Context context, SQLiteDatabase db, Calendar fromDate, Calendar toDate) {
		SparseArray<Double> result = new SparseArray<Double>();
		
        String fromDateString = String.valueOf(fromDate.getTimeInMillis());
        String toDateString = String.valueOf(toDate.getTimeInMillis());
        
        String selectQuery = "select sum("+FIELDNAME_AMOUNT+")/100.0 from "+TABLENAME+
        		             " where "+FIELDNAME_TIMESTAMP+" >= "+fromDateString+
        		             " and "+FIELDNAME_TIMESTAMP+" <= "+toDateString+
        		             " and coalesce("+FIELDNAME_FROM_ENVELOPE+",0)=0"+
        		             " and coalesce("+FIELDNAME_DELETED+", 0) = 0"+ 
        		             " order by "+FIELDNAME_TIMESTAMP;
        
        Cursor c = db.rawQuery(selectQuery, null);
 
        if (c.moveToFirst()) {
            result.put(KEY_INCOME, c.getDouble(0));
        }
		
        selectQuery = "select sum("+FIELDNAME_AMOUNT+")/100.0 from "+TABLENAME+
	             " where "+FIELDNAME_TIMESTAMP+" between "+fromDateString+" and "+toDateString+
	             " and coalesce("+FIELDNAME_TO_ENVELOPE+",0)=0"+
	             " and coalesce("+FIELDNAME_DELETED+", 0) = 0"+ 
	             " order by "+FIELDNAME_TIMESTAMP;

        c = db.rawQuery(selectQuery, null);

        if (c.moveToFirst()) {
        	result.put(KEY_EXPENSE, c.getDouble(0));
        }
		return result;
	}
	
	public static List<TransactionDataObject> getPending(Context context, SQLiteDatabase db) {
        List<TransactionDataObject> transactions = new ArrayList<TransactionDataObject>();
        
        String selectQuery = "select * from "+TABLENAME+
        		             " where coalesce("+FIELDNAME_PENDING+",0)=1"+
        		             " and coalesce("+FIELDNAME_DELETED+", 0) = 0"+ 
        		             " order by "+FIELDNAME_TIMESTAMP;
        
        Cursor c = db.rawQuery(selectQuery, null);
 
        if (c.moveToFirst()) {
            do {
            	TransactionDataObject tdo = new TransactionDataObject(context, c);
                transactions.add(tdo);
            } while (c.moveToNext());
        }
 
        return transactions;
    }
	
	@Override
	protected ArrayList<String> getTriggers() {
		ArrayList<String> result = super.getTriggers();

		String updateQueryToEnvelopeNew = " update "+EnvelopeDataObject.TABLENAME+
				" set "+EnvelopeDataObject.FIELDNAME_BUDGET+" =  " +
				        //all transactions that went into the envelope
						"(select coalesce(sum("+FIELDNAME_AMOUNT+"), 0) " +
								"from "+TABLENAME+
								" where "+FIELDNAME_TO_ENVELOPE+"=new."+FIELDNAME_TO_ENVELOPE+
								" and coalesce("+FIELDNAME_DELETED+",0)=0) " +
						" - " +
					    //all transactions that went out of that envelope
						"(select coalesce(sum("+FIELDNAME_AMOUNT+"), 0) " +
						"from "+TABLENAME+
						" where "+FIELDNAME_FROM_ENVELOPE+"=new."+FIELDNAME_TO_ENVELOPE+
						" and coalesce("+FIELDNAME_DELETED+",0)=0) " +
				" where ID=new."+FIELDNAME_TO_ENVELOPE+"; ";
		

		String updateQueryFromEnvelopeNew = " update "+EnvelopeDataObject.TABLENAME+
				" set "+EnvelopeDataObject.FIELDNAME_BUDGET+" =  " +
				        //all transactions that went into the envelope
						"(select coalesce(sum("+FIELDNAME_AMOUNT+"), 0) " +
								"from "+TABLENAME+
								" where "+FIELDNAME_TO_ENVELOPE+"=new."+FIELDNAME_FROM_ENVELOPE+
								" and coalesce("+FIELDNAME_DELETED+",0)=0) " +
						" - " +
					    //all transactions that went out of that envelope
						"(select coalesce(sum("+FIELDNAME_AMOUNT+"), 0) " +
						"from "+TABLENAME+
						" where "+FIELDNAME_FROM_ENVELOPE+"=new."+FIELDNAME_FROM_ENVELOPE+
						" and coalesce("+FIELDNAME_DELETED+",0)=0) " +
				" where ID=new."+FIELDNAME_FROM_ENVELOPE+"; ";
		
	
		String updateQueryToEnvelopeOld = " update "+EnvelopeDataObject.TABLENAME+
				" set "+EnvelopeDataObject.FIELDNAME_BUDGET+" =  " +
				        //all transactions that went into the envelope
						"(select coalesce(sum("+FIELDNAME_AMOUNT+"), 0) " +
								"from "+TABLENAME+
								" where "+FIELDNAME_TO_ENVELOPE+"=old."+FIELDNAME_TO_ENVELOPE+
								" and coalesce("+FIELDNAME_DELETED+",0)=0) " +
						" - " +
					    //all transactions that went out of that envelope
						"(select coalesce(sum("+FIELDNAME_AMOUNT+"), 0) " +
						"from "+TABLENAME+
						" where "+FIELDNAME_FROM_ENVELOPE+"=old."+FIELDNAME_TO_ENVELOPE+
						" and coalesce("+FIELDNAME_DELETED+",0)=0) " +
				" where ID=old."+FIELDNAME_TO_ENVELOPE+
				  " and old."+FIELDNAME_TO_ENVELOPE+"<>new."+FIELDNAME_TO_ENVELOPE+"; ";
		

		String updateQueryFromEnvelopeOld = " update "+EnvelopeDataObject.TABLENAME+
				" set "+EnvelopeDataObject.FIELDNAME_BUDGET+" =  " +
				        //all transactions that went into the envelope
						"(select coalesce(sum("+FIELDNAME_AMOUNT+"), 0) " +
								"from "+TABLENAME+
								" where "+FIELDNAME_TO_ENVELOPE+"=old."+FIELDNAME_FROM_ENVELOPE+
								" and coalesce("+FIELDNAME_DELETED+",0)=0) " +
						" - " +
					    //all transactions that went out of that envelope
						"(select coalesce(sum("+FIELDNAME_AMOUNT+"), 0) " +
						"from "+TABLENAME+
						" where "+FIELDNAME_FROM_ENVELOPE+"=old."+FIELDNAME_FROM_ENVELOPE+
						" and coalesce("+FIELDNAME_DELETED+",0)=0) " +
				" where ID=old."+FIELDNAME_FROM_ENVELOPE+
				  " and old."+FIELDNAME_FROM_ENVELOPE+"<>new."+FIELDNAME_FROM_ENVELOPE+"; ";
		

		result.add("drop trigger if exists INSERT_"+getTableName()+"_CALC_ENVELOPE_BUDGETS");
		result.add("drop trigger if exists UPDATE_"+getTableName()+"_CALC_ENVELOPE_BUDGETS");
		
		result.add("create trigger if not exists UPDATE_"+getTableName()+"_CALC_ENVELOPE_BUDGETS " +
				"after update of "+FIELDNAME_AMOUNT+" on " + getTableName() + " for each row " +
				"begin" +
				updateQueryFromEnvelopeNew +
				updateQueryFromEnvelopeOld + 
				updateQueryToEnvelopeNew + 
				updateQueryToEnvelopeOld +
				"end");
		
		result.add("create trigger if not exists INSERT_"+getTableName()+"_CALC_ENVELOPE_BUDGETS " +
				"after insert on " + getTableName() + " for each row " +
				"begin" +
				updateQueryFromEnvelopeNew +
				updateQueryToEnvelopeNew +
				"end");
		
		return result;
	}

	@Override
	public List<BaseDataObject> getChangeset(Context context, SQLiteDatabase db) {
		List<BaseDataObject> lst = new ArrayList<BaseDataObject>();
        String selectQuery = "SELECT * FROM " + TABLENAME +" where "+FIELDNAME_CHANGED+" IS NOT NULL";
 
        Cursor c = db.rawQuery(selectQuery, null);
 
        // looping through all rows and adding to list
        if (c.moveToFirst()) {
            do {
            	TransactionDataObject fdo = new TransactionDataObject(context, c);
            	lst.add(fdo);
            } while (c.moveToNext());
        }
 
        return lst;
	}
	
	public static TransactionsMonth[] getMonthsList() {
		int currentYear, currentMonth, year, month; 
		DBMain db = DBMain.getInstance();
		Date date = TransactionDataObject.getMinimumDate(db.getReadableDatabase());
		Date maxDate = TransactionDataObject.getMaximumDate(db.getReadableDatabase());
		Calendar cal = Calendar.getInstance();
		cal.setTime(maxDate);
		currentYear = cal.get(Calendar.YEAR);
		currentMonth = cal.get(Calendar.MONTH);
		cal.setTime(date);
		ArrayList<TransactionsMonth> array = new ArrayList<TransactionsMonth>();
		
		do {
			year = cal.get(Calendar.YEAR);
			month = cal.get(Calendar.MONTH);
			
			array.add(new TransactionsMonth(year, month));
			cal.add(Calendar.MONTH, 1);		
		} while (!((month == currentMonth) && (year == currentYear)));
		
		return array.toArray(new TransactionsMonth[array.size()]);
	}
	
}
