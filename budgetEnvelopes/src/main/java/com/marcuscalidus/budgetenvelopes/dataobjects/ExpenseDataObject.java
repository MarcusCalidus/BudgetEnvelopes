package com.marcuscalidus.budgetenvelopes.dataobjects;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ExpenseDataObject extends BaseDataObject {

	public ExpenseDataObject(Context context, Cursor c) {
		super(context, c);
	}
	
	public ExpenseDataObject(Context context, UUID envelopeId) {
		super(context, null);
		_Frequency = 1;
		_Envelope = envelopeId;
	}

	public static String TABLENAME = "EXPENSES";
	
	public static String FIELDNAME_LABEL = "LABEL";
	public static String FIELDNAME_AMOUNT = "AMOUNT";
	public static String FIELDNAME_FREQUENCY = "FREQUENCY";
	public static String FIELDNAME_ENVELOPE = "ENVELOPE";
	

	private UUID _Envelope;
	private String _Label;
	private double _Amount;
	private int _Frequency;
	
	@Override
	protected void initializeFromCursor(Cursor c) {
		_Label = c.getString(c.getColumnIndex(FIELDNAME_LABEL));
		_Amount =  c.getInt(c.getColumnIndex(FIELDNAME_AMOUNT)) / 100.0;
		_Frequency = c.getInt(c.getColumnIndex(FIELDNAME_FREQUENCY));
		_Envelope = castBlobAsUUID(c.getBlob(c.getColumnIndex(FIELDNAME_ENVELOPE)));
	}

	@Override
	public String getTableName() {
		return TABLENAME;
	}

	@Override
	protected String[] getFieldNames() {
		return new String[] {FIELDNAME_LABEL, FIELDNAME_ENVELOPE, FIELDNAME_FREQUENCY, FIELDNAME_AMOUNT};
	}

	@Override
	protected ContentValues getContentValues() {
		ContentValues vals = new ContentValues();
		vals.put(FIELDNAME_AMOUNT, Math.round(_Amount * 100));
		vals.put(FIELDNAME_ENVELOPE, castUUIDAsBlob(getEnvelope()));
		vals.put(FIELDNAME_FREQUENCY, _Frequency);
		vals.put(FIELDNAME_LABEL, _Label);
		return vals;
	}

	public UUID getEnvelope() {
		return _Envelope;
	}

	public void setEnvelope(UUID _Envelope) {
		this._Envelope = _Envelope;
	}

	public double getAmount() {
		return _Amount;
	}

	public void setAmount(double _Amount) {
		this._Amount = _Amount;
	}

	public int getFrequency() {
		return _Frequency;
	}

	public void setFrequency(int _Frequency) {
		if (_Frequency <= 0)
			this._Frequency = 1;
		else
			this._Frequency = _Frequency;
	}

	public String getLabel() {
		return _Label;
	}

	public void setLabel(String _Label) {
		this._Label = _Label;
	}
	
	@Override
	protected ArrayList<String> getTriggers() {
		ArrayList<String> result = super.getTriggers();
		
		String updateQueryEnvelope = " update "+EnvelopeDataObject.TABLENAME+" set "+EnvelopeDataObject.FIELDNAME_EXPENSES+" = (select round(sum("+FIELDNAME_AMOUNT+")/"+FIELDNAME_FREQUENCY+") from "+TABLENAME+" where "+FIELDNAME_ENVELOPE+"=new."+FIELDNAME_ENVELOPE+" and coalesce("+FIELDNAME_DELETED+",0)=0)  where ID=new."+FIELDNAME_ENVELOPE+"; ";
		String updateQueryBaseEnvelope = " update "+EnvelopeDataObject.TABLENAME+
                                        " set "+EnvelopeDataObject.FIELDNAME_EXPENSES+" = "+
                                            "(select round(sum("+ExpenseDataObject.FIELDNAME_AMOUNT+")/"+ExpenseDataObject.FIELDNAME_FREQUENCY+")"+
                                            " from "+ExpenseDataObject.TABLENAME+
                                            " inner join "+EnvelopeDataObject.TABLENAME+" on "+ExpenseDataObject.TABLENAME+"."+ExpenseDataObject.FIELDNAME_ENVELOPE+" = "+EnvelopeDataObject.TABLENAME+".ID"+
                                            " where coalesce("+ExpenseDataObject.TABLENAME+"."+ExpenseDataObject.FIELDNAME_DELETED+",0)=0"+
                                            "   and coalesce("+EnvelopeDataObject.TABLENAME+"."+EnvelopeDataObject.FIELDNAME_DELETED+",0)=0)"+
                                        " where hex(ID)='"+EnvelopeDataObject.castUUIDAsHexString(EnvelopeDataObject.baseEnvelopeID)+"'; ";

		result.add("drop trigger if exists INSERT_"+getTableName()+"_CALC_ENVELOPE_EXPENSES");
		result.add("drop trigger if exists UPDATE_"+getTableName()+"_CALC_ENVELOPE_EXPENSES");
		
		result.add("create trigger if not exists UPDATE_"+getTableName()+"_CALC_ENVELOPE_EXPENSES " +
				"after update of "+FIELDNAME_DELETED+","+FIELDNAME_AMOUNT+" on " + getTableName() + " for each row " +
				"begin" +
				updateQueryEnvelope +
				updateQueryBaseEnvelope +
				"end");
		
		result.add("create trigger if not exists INSERT_"+getTableName()+"_CALC_ENVELOPE_EXPENSES " +
				"after insert on " + getTableName() + " for each row " +
				"begin" +
				updateQueryEnvelope +
				updateQueryBaseEnvelope +
				"end");
		
		return result;
	}

	public static List<ExpenseDataObject> getExpensesForEnvelope(Context context, 
		SQLiteDatabase db, EnvelopeDataObject envelope) {
	    List<ExpenseDataObject> expenses = new ArrayList<ExpenseDataObject>();
        String selectQuery = "SELECT * FROM " + TABLENAME +" where hex("+FIELDNAME_ENVELOPE+")='"+castUUIDAsHexString(envelope.getId())+"' and coalesce(DELETED, 0) = 0 order by "+FIELDNAME_LABEL;
 
        Cursor c = db.rawQuery(selectQuery, null);
 
        // looping through all rows and adding to list
        if (c.moveToFirst()) {
            do {
            	ExpenseDataObject fdo = new ExpenseDataObject(context, c);
                expenses.add(fdo);
            } while (c.moveToNext());
        }
 
        return expenses;
	}

	@Override
	public List<BaseDataObject> getChangeset(Context context, SQLiteDatabase db) {
		List<BaseDataObject> expenses = new ArrayList<BaseDataObject>();
        String selectQuery = "SELECT * FROM " + TABLENAME +" where "+FIELDNAME_CHANGED+" IS NOT NULL";
 
        Cursor c = db.rawQuery(selectQuery, null);
 
        // looping through all rows and adding to list
        if (c.moveToFirst()) {
            do {
            	ExpenseDataObject fdo = new ExpenseDataObject(context, c);
                expenses.add(fdo);
            } while (c.moveToNext());
        }
 
        return expenses;
	}
}
