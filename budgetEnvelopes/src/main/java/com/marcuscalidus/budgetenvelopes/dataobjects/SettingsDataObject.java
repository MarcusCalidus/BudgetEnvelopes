package com.marcuscalidus.budgetenvelopes.dataobjects;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SettingsDataObject extends BaseDataObject {

	public SettingsDataObject(Context context, Cursor c) {
		super(context, c);
	}
	
	public SettingsDataObject(Context context, UUID uuid, boolean deleted) {
		super(context, uuid, deleted);
	}

	private String _Value;	

	public static String TABLENAME = "SETTINGS";
	
	public static final UUID UUID_SYNC_ACCOUNT = UUID.fromString("00000000-0000-0000-0000-000000000001");	
	public static final UUID UUID_CURRENCY_SYMBOL = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final UUID UUID_BACKUP_COUNT = UUID.fromString("00000000-0000-0000-0000-000000000003");
    public static final UUID UUID_SYNC_ON_START = UUID.fromString("00000000-0000-0000-0000-000000000004");
	
	private final String FIELDNAME_VALUE = "VALUE";

	@Override 
	protected void initializeFromCursor(Cursor c) {
		_Value = c.getString(c.getColumnIndex(FIELDNAME_VALUE));
	}

	@Override
	public String getTableName() {
		return TABLENAME;
	}

	@Override
	protected String[] getFieldNames() {
		return new String[] {FIELDNAME_VALUE};
	}

	@Override
	protected ContentValues getContentValues() {
		ContentValues vals = new ContentValues();
		vals.put(FIELDNAME_VALUE, _Value);
		return vals;
	}

	public String getValue() {
		return _Value;
	}
	
	public void setValue(String _Value) {
		this._Value = _Value;
	}
	
	public static SettingsDataObject getSetting(Context context, SQLiteDatabase db, UUID uuid) {
		String selectQuery = "SELECT * FROM " + TABLENAME +" where hex(ID) = '"+castUUIDAsHexString(uuid)+"';";
		 
        Cursor c = db.rawQuery(selectQuery, null);
 
        if (c.moveToFirst()) {
        	return new SettingsDataObject(context, c); 
        } else {
        	return new SettingsDataObject(context, uuid, false);
        }
	}	
	
	@Override
	public List<BaseDataObject> getChangeset(Context context, SQLiteDatabase db) {
		List<BaseDataObject> lst = new ArrayList<BaseDataObject>();
        String selectQuery = "SELECT * FROM " + TABLENAME +" where "+FIELDNAME_CHANGED+" IS NOT NULL";
 
        Cursor c = db.rawQuery(selectQuery, null);
 
        // looping through all rows and adding to list
        if (c.moveToFirst()) {
            do {
            	SettingsDataObject fdo = new SettingsDataObject(context, c);
            	lst.add(fdo);
            } while (c.moveToNext());
        }
 
        return lst;
	}

}
