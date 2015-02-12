package com.marcuscalidus.budgetenvelopes.dataobjects;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.CursorAdapter;
import android.widget.FilterQueryProvider;
import android.widget.SimpleCursorAdapter;

import com.google.android.gms.internal.ig;
import com.marcuscalidus.budgetenvelopes.R;
import com.marcuscalidus.budgetenvelopes.transactions.TransactionDialogFragment;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@SuppressLint("DefaultLocale")
public class EnvelopeDataObject extends BaseDataObject {

	private String _Title;
	private int _TabColor;
	private boolean _Space_After;
	private int _Position;
	private double _Expenses;
	private double _Budget;
	private String _Stamp;
    private boolean _Ignore_Reset;
		
	public static String TABLENAME = "ENVELOPES";
	
	private static String FIELDNAME_TITLE = "TITLE";
	private static String FIELDNAME_TABCOLOR = "TABCOLOR";
	private static String FIELDNAME_SPACE_AFTER = "SPC_AFTER";
	private static String FIELDNAME_POSITION = "POSITION";
	public static String  FIELDNAME_EXPENSES = "EXPENSES";
	public static String  FIELDNAME_BUDGET = "BUDGET";
	public static String  FIELDNAME_STAMP = "STAMP";
    public static String  FIELDNAME_IGNORE_RESET = "INGORE_RESET";
	
	public static final UUID baseEnvelopeID = UUID.fromString("00000000-0000-0000-0000-000000000000");
	
	public EnvelopeDataObject(Context context, Cursor c) {
		super(context, c);
	}
	
	public EnvelopeDataObject(Context context, String title, int color) {
		this(context, null);
		_Title = title;
		_TabColor = color;
		_Space_After = false;
		_Position = 0;
		_Expenses = 0;
		_Stamp = "";
        _Ignore_Reset = false;
	}

	@Override
	public String getTableName() {
		return TABLENAME;
	}

	@Override
	protected void initializeFromCursor(Cursor c) {
		_Title = c.getString(c.getColumnIndex(FIELDNAME_TITLE));	
		_TabColor = c.getInt(c.getColumnIndex(FIELDNAME_TABCOLOR));
		_Position = c.getInt(c.getColumnIndex(FIELDNAME_POSITION));	
		_Space_After = c.getInt(c.getColumnIndex(FIELDNAME_SPACE_AFTER)) != 0;
		_Expenses = c.getInt(c.getColumnIndex(FIELDNAME_EXPENSES))/100.0;
		_Budget = c.getInt(c.getColumnIndex(FIELDNAME_BUDGET))/100.0;
		_Stamp = c.getString(c.getColumnIndex(FIELDNAME_STAMP));
        _Ignore_Reset = c.getInt(c.getColumnIndex(FIELDNAME_IGNORE_RESET)) != 0;
	}
	
	@Override
	public String toString() {
		return getTitle();
	}
	
	public boolean isBaseEnvelope() {
		return getId().compareTo(baseEnvelopeID) == 0;
	}
	
	public String getTitle() {
		if (!isBaseEnvelope())
			return _Title;
		else {
			return context.getResources().getString(R.string.undistributed);
		}
	}
	
	public double getBudget() { return _Budget; }
	
	public double getExpenses() {
		return _Expenses;
	}
	
	public void setTitle(String title) {
		_Title = title;
	}
	
	public String getStamp() {
		if ("_blank_".equals(_Stamp)) 
			return "";
		else				
			return _Stamp;
	}
	
	public void setStamp(String stamp) {
		_Stamp = stamp;
	}
	
	public Cursor getDescriptionAutocomplete(SQLiteDatabase db, int transactionType, String filter) {
		String sql = "select max(ROWID) _id, "+TransactionDataObject.FIELDNAME_TEXT+
			     " from "+TransactionDataObject.TABLENAME;
	
		switch (transactionType) {
		case TransactionDialogFragment.TYPE_DEPOSIT : 
			sql = sql + " where hex("+TransactionDataObject.FIELDNAME_TO_ENVELOPE+") = '"+castUUIDAsHexString(this.getId())+"' and "+TransactionDataObject.FIELDNAME_FROM_ENVELOPE+" is null";
			break;
		case TransactionDialogFragment.TYPE_WITHDRAWAL : 
			sql = sql + " where hex("+TransactionDataObject.FIELDNAME_FROM_ENVELOPE+") = '"+castUUIDAsHexString(this.getId())+"' and "+TransactionDataObject.FIELDNAME_TO_ENVELOPE+" is null";
			break;
		case TransactionDialogFragment.TYPE_TRANSFER : 
			sql = sql + " where hex("+TransactionDataObject.FIELDNAME_FROM_ENVELOPE+") = '"+castUUIDAsHexString(this.getId())+"' and "+TransactionDataObject.FIELDNAME_TO_ENVELOPE+" is not null";
			break;			
		}
		
		if (filter != null);
		  sql = sql + " and upper("+TransactionDataObject.FIELDNAME_TEXT+") like upper('%"+filter+"%')"; 
		
		sql = sql + " group by "+TransactionDataObject.FIELDNAME_TEXT;
		
		return db.rawQuery(sql, null);	
	}
	
	public CursorAdapter getDescriptionsAdapter(Context context, final SQLiteDatabase db, final int transactionType) {
		
		String[] from = new String[] {TransactionDataObject.FIELDNAME_TEXT};
		int[] _to = new int[] {android.R.id.text1};
		
		final EnvelopeDataObject thisEnvelope = this;
		
		Cursor cursor = this.getDescriptionAutocomplete(db, transactionType, null);
		
		SimpleCursorAdapter itemNameAdapter = new SimpleCursorAdapter(context, android.R.layout.simple_dropdown_item_1line, cursor, from, _to, 0);
		itemNameAdapter.setStringConversionColumn(
		        cursor.getColumnIndexOrThrow(TransactionDataObject.FIELDNAME_TEXT));
		
		itemNameAdapter.setFilterQueryProvider(new FilterQueryProvider() {

	        public Cursor runQuery(CharSequence constraint) {
	            String filter = null;
	            if (constraint != null) {
	            	filter = constraint.toString();
	            }
	            return thisEnvelope.getDescriptionAutocomplete(db, transactionType, filter);
	        }
	    });
		
		return itemNameAdapter;
		
	}
	
	
	@Override
	protected ContentValues getContentValues() {
		ContentValues vals = new ContentValues();
		vals.put(FIELDNAME_TITLE, _Title);
		vals.put(FIELDNAME_TABCOLOR, _TabColor);
		vals.put(FIELDNAME_SPACE_AFTER, _Space_After);
		vals.put(FIELDNAME_POSITION, _Position);
		vals.put(FIELDNAME_STAMP, _Stamp);
        vals.put(FIELDNAME_IGNORE_RESET, _Ignore_Reset);
		return vals;
	}

	public int getTabColor() {
		return _TabColor;
	}

	public void setTabColor(int tabColor) {
		this._TabColor = tabColor;
	}

    public boolean getIgnoreOnReset() {return _Ignore_Reset; }

    public void setIgnoreOnReset(boolean ignoreOnReset) { this._Ignore_Reset = ignoreOnReset; }

	@Override
	protected String[] getFieldNames() {
		return new String[] {FIELDNAME_TITLE,
				             FIELDNAME_TABCOLOR,
				             FIELDNAME_SPACE_AFTER,
				             FIELDNAME_POSITION,
				             FIELDNAME_EXPENSES,
				             FIELDNAME_BUDGET,
				             FIELDNAME_STAMP,
                             FIELDNAME_IGNORE_RESET};
	}

	public boolean isSpace_After() {
		return _Space_After;
	}

	public void setSpace_After(boolean _Space_After) {
		this._Space_After = _Space_After;
	}

	public int getPosition() {
		return _Position;
	}

	public void setPosition(int _Position) {
		this._Position = _Position;
	}
	
	public static EnvelopeDataObject getById(Context context, SQLiteDatabase db, UUID uuid) {
		 String selectQuery = "SELECT * FROM " + TABLENAME +" where hex(ID) = '"+castUUIDAsHexString(uuid)+"'";
		 
	     Cursor c = db.rawQuery(selectQuery, null);
	 
	      if (c.moveToFirst()) {
	    	  return new EnvelopeDataObject(context, c);
	      }	    	  
	      
	      return null; 
	}
	
	public static EnvelopeDataObject getBaseEnvelope(Context context, SQLiteDatabase db) {
		EnvelopeDataObject fdo = getById(context, db, baseEnvelopeID);
		if (fdo == null) {
			ContentValues values = new ContentValues();
			values.put("ID", castUUIDAsBlob(baseEnvelopeID));
			db.replace(TABLENAME, null, values);
			
			fdo = getById(context, db, baseEnvelopeID);
		}
		return fdo;
	}
	
	public static List<EnvelopeDataObject> getAllEnvelopes(Context context, SQLiteDatabase db, boolean honourSpaceAfterFlag, boolean includeBaseEnvelope) {
        List<EnvelopeDataObject> envelope = new ArrayList<EnvelopeDataObject>();
        String selectQuery;
        if (includeBaseEnvelope) {
        	selectQuery = "SELECT  * FROM " + TABLENAME +" where coalesce("+FIELDNAME_DELETED+", 0) = 0 order by POSITION";
        } else {
        	selectQuery = "SELECT  * FROM " + TABLENAME +" where hex(ID)<>'"+castUUIDAsHexString(baseEnvelopeID)+"' and coalesce("+FIELDNAME_DELETED+", 0) = 0 order by POSITION";
        }
        
        Cursor c = db.rawQuery(selectQuery, null);
 
        // looping through all rows and adding to list
        if (c.moveToFirst()) {
            do {
                EnvelopeDataObject fdo = new EnvelopeDataObject(context, c);
                envelope.add(fdo);
                if (honourSpaceAfterFlag && fdo.isSpace_After()) 
                	envelope.add(null);
            } while (c.moveToNext());
        }
 
        return envelope;
    }
	
	public static Double queryTotalCredit(Context context, SQLiteDatabase db) {
		Double result = (double) 0;
		String selectQuery = "select sum("+FIELDNAME_BUDGET+") from "+TABLENAME+" where coalesce("+FIELDNAME_DELETED+",0)=0;";
		Cursor c = db.rawQuery(selectQuery, null);
		
		if (c.moveToFirst()) {
			result = c.getDouble(0) / 100.0;
		}
		return result;		
	}
	
	@Override
	protected ArrayList<String> getTriggers() {
		ArrayList<String> result = super.getTriggers();

		String updateQueryEnvelopeBudget = " update "+TABLENAME+
				" set "+FIELDNAME_BUDGET+" = " +
				        //all transactions that went into the envelope
						"(select coalesce(sum("+TransactionDataObject.FIELDNAME_AMOUNT+"), 0) " +
								"from "+TransactionDataObject.TABLENAME+
								" where "+TransactionDataObject.FIELDNAME_TO_ENVELOPE+"=new.ID"+
								" and coalesce("+TransactionDataObject.FIELDNAME_DELETED+",0)=0) " +
						" - " +
					    //all transactions that went out of that envelope
						"(select coalesce(sum("+TransactionDataObject.FIELDNAME_AMOUNT+"), 0) " +
						"from "+TransactionDataObject.TABLENAME+
						" where "+TransactionDataObject.FIELDNAME_FROM_ENVELOPE+"=new.ID"+
						" and coalesce("+TransactionDataObject.FIELDNAME_DELETED+",0)=0) " +
				" where ID=new.ID and coalesce("+FIELDNAME_DELETED+", 0)=0; "; 
		
		String updateQueryEnvelopeExpense = " update "+TABLENAME+
				" set "+FIELDNAME_EXPENSES+" = " + 
					"(select round(sum("+ExpenseDataObject.FIELDNAME_AMOUNT+")/"+ExpenseDataObject.FIELDNAME_FREQUENCY+") "+
						"from "+ExpenseDataObject.TABLENAME+
						" where "+ExpenseDataObject.FIELDNAME_ENVELOPE+"=new.ID"+
						" and coalesce("+ExpenseDataObject.FIELDNAME_DELETED+",0)=0) "+
					" where ID=new.ID and coalesce("+FIELDNAME_DELETED+", 0)=0; ";
		
		String updateQueryBaseEnvelope = " update "+TABLENAME+
				" set "+FIELDNAME_EXPENSES+" = "+
						"(select round(sum("+ExpenseDataObject.FIELDNAME_AMOUNT+")/"+ExpenseDataObject.FIELDNAME_FREQUENCY+")"+
						" from "+ExpenseDataObject.TABLENAME+
                        " inner join "+EnvelopeDataObject.TABLENAME+" on "+ExpenseDataObject.TABLENAME+"."+ExpenseDataObject.FIELDNAME_ENVELOPE+" = "+EnvelopeDataObject.TABLENAME+".ID"+
						" where coalesce("+ExpenseDataObject.TABLENAME+"."+ExpenseDataObject.FIELDNAME_DELETED+",0)=0"+
                        "   and coalesce("+EnvelopeDataObject.TABLENAME+"."+EnvelopeDataObject.FIELDNAME_DELETED+",0)=0)"+
					" where hex(ID)='"+EnvelopeDataObject.castUUIDAsHexString(EnvelopeDataObject.baseEnvelopeID)+"'; ";

		result.add("drop trigger if exists UPDATE_"+getTableName()+"_CALC_ENVELOPE");

        result.add("create trigger if not exists UPDATE_" + getTableName() + "_CALC_ENVELOPE " +
                    "after update  of " + FIELDNAME_DELETED + "," + FIELDNAME_EXPENSES + "," + FIELDNAME_BUDGET + " on " + getTableName() + " for each row " +
                    "begin" +
                    updateQueryEnvelopeBudget +
                    updateQueryEnvelopeExpense +
                    updateQueryBaseEnvelope +
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
            	EnvelopeDataObject fdo = new EnvelopeDataObject(context, c);
            	lst.add(fdo);
            } while (c.moveToNext());
        }
 
        return lst;
	}

	@Override
	public void insertOrReplaceIntoDb(SQLiteDatabase db, Boolean useTransaction){
		if (useTransaction)
			db.beginTransaction();
		
		super.insertOrReplaceIntoDb(db, false);
		
		db.execSQL("UPDATE "+TABLENAME+
				     " SET "+FIELDNAME_BUDGET+"=null, "+
				             FIELDNAME_EXPENSES+"=null"+
				     " WHERE hex(ID)='"+castUUIDAsHexString(this.getId())+"';");
		
		 if (useTransaction) {
	    	 db.setTransactionSuccessful();
	    	 db.endTransaction();
	     }
	}

    public void emptyEnvelope(SQLiteDatabase db) {
        if (!this._Ignore_Reset) {
            Resources r = context.getResources();
            r.getText(R.string.empty_envelopes);

            TransactionDataObject transaction = new TransactionDataObject(context, null);
            transaction.setAmount(this.getBudget());
            transaction.setFromEnvelope(this.getId());
            transaction.setToEnvelope(baseEnvelopeID);
            transaction.setTimestamp(new Date());
            transaction.setText(r.getText(R.string.empty_envelopes).toString());

            transaction.insertOrReplaceIntoDb(db, true);
        }
    }

    public static void emptyAllEnvelopes(Context context, SQLiteDatabase db) {
        List<EnvelopeDataObject> envelopes = getAllEnvelopes(context, db, false, false);

        for (EnvelopeDataObject envelope : envelopes) {
            envelope.emptyEnvelope(db);
        }
    }

}
