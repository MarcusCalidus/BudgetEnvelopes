package com.marcuscalidus.budgetenvelopes.dataobjects;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.Toast;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public abstract class BaseDataObject {
	
	private final UUID _Id;
	private String Tag;
	
	private boolean _Deleted;

	public static String FIELDNAME_DELETED = "DELETED";
	public static String FIELDNAME_CHANGED = "CHANGED";
		
	protected abstract void initializeFromCursor(Cursor c);
	protected abstract String[] getFieldNames();
	protected abstract ContentValues getContentValues();
	public abstract String getTableName();
	
	protected Context context;

	
	public BaseDataObject(Context context, Cursor c) {
		this.context = context;
		if (c != null) {
			_Id = castBlobAsUUID(c.getBlob(c.getColumnIndex("ID")));
			_Deleted = c.getInt(c.getColumnIndex(FIELDNAME_DELETED)) != 0;
			
			initializeFromCursor(c);
		} 
		else {
			_Id = UUID.randomUUID();
			_Deleted = false;
		}
	}
	
	public BaseDataObject(Context context, UUID uuid, boolean deleted) {
		this.context = context;
		_Id = uuid;
        _Deleted = deleted;
	}
	
	public UUID getId() {
		return _Id;
	}
		
	protected String getCreateTableStmt() {
		String fieldlist = "";
		String[] fields = getFieldNames();
		for (int i = 0; i<fields.length; i++) {fieldlist += "," + fields[i];}
		return "create table if not exists "+getTableName()+" (ID PRIMARY KEY "+fieldlist+")";	
	}
	
	protected ArrayList<String> getTriggers() {
		String fieldlist = "DELETED";
		String[] fields = getFieldNames();
		for (int i = 0; i<fields.length; i++) {fieldlist += "," + fields[i];}
		
		ArrayList<String> result = new ArrayList<String>();
		result.add("drop trigger if exists UPDATE_"+getTableName()+"_SETCHANGED");
		result.add("drop trigger if exists INSERT_"+getTableName()+"_SETCHANGED");

        result.add("create trigger if not exists UPDATE_" + getTableName() + "_SETCHANGED " +
                    "after update of " + fieldlist + " on " + getTableName() + " for each row " +
                    "begin" +
                    " update " + getTableName() + " set CHANGED=datetime('now') where ID=new.ID; " +
                    "end");

        result.add("create trigger if not exists INSERT_" + getTableName() + "_SETCHANGED " +
                    "after insert on " + getTableName() + " for each row " +
                    "begin " +
                    " update " + getTableName() + " set CHANGED=datetime('now') where ID=new.ID; " +
                    "end");
		
		return result;
	}
	
	protected UUID castBlobAsUUID(byte[] blob) {
		if (blob == null) 
			return null;
		
		ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
		bb.put(blob);
		bb.position(0);
		return new UUID(bb.getLong(), bb.getLong());
	}
	
	public static String castUUIDAsHexString(UUID uuid) {
		if (uuid != null)
			return uuid.toString().replaceAll("-", "").toUpperCase(Locale.ENGLISH);
		else
			return "";
	} 
	
	protected static byte[] castUUIDAsBlob(UUID uuid) {
		if (uuid == null)
			return null;
		
		ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
		bb.putLong(uuid.getMostSignificantBits());
		bb.putLong(uuid.getLeastSignificantBits());
		return bb.array();
	}
	
	public void createTableInDb(SQLiteDatabase db) {
		db.execSQL(getCreateTableStmt());
	}

    public void createTriggersInDb(SQLiteDatabase db) {
        ArrayList<String> triggers = getTriggers();
        for (int i = 0; i < triggers.size(); i++) {
            db.execSQL(triggers.get(i));
        }
    }

    public void deleteChangeTriggersInDb(SQLiteDatabase db) {
        db.execSQL("drop trigger if exists UPDATE_"+getTableName()+"_SETCHANGED");
        db.execSQL("drop trigger if exists INSERT_"+getTableName()+"_SETCHANGED");
    }

    public void createFieldsInDb(SQLiteDatabase db) {
		Cursor c = db.rawQuery("pragma table_info("+getTableName()+")", null);
		  
		ArrayList<String> fields = new ArrayList<String>();
		Collections.addAll(fields, getFieldNames());
		fields.add(FIELDNAME_DELETED);
		fields.add(FIELDNAME_CHANGED);
		  
	    if (c.moveToFirst()) {
	    	do {
	    		fields.remove(c.getString(c.getColumnIndex("name")));
            } while (c.moveToNext());
	    }
	     	     
	    for (int i = 0; i < fields.size(); i++) {
	    	db.execSQL("alter table " + getTableName() + " add column " + fields.get(i) + ";");
	    }
	}
	
/*	public void deleteFromDb(SQLiteDatabase db) {	
		ContentValues values = new ContentValues();
		values.put("ID", castUUIDAsBlob(getId()));
		values.put(FIELDNAME_DELETED, true);
		
		if (db != null)
			db.replace(getTableName(), null, values);	
		
		//dbMain.delete(getTableName(), "hex(ID) = ?", new String[] {castUUIDAsHexString(getId())});
	}
*/
	public void insertOrReplaceIntoDb(SQLiteDatabase db, Boolean useTransaction){
		if (useTransaction)
			db.beginTransaction();
		
		ContentValues values = getContentValues();
		
		values.put("ID", castUUIDAsBlob(getId()));
		values.put(FIELDNAME_DELETED, _Deleted);
		
		if (db != null)
			db.replace(getTableName(), null, values);		
		
		 if (useTransaction) {
	    	 db.setTransactionSuccessful();
	    	 db.endTransaction();
	     }

        File f = context.getFileStreamPath("localSyncToken");
        if (f.exists()) {
            if (!f.delete()) {
                Toast.makeText(context, "could not be deleted", Toast.LENGTH_LONG).show();
            }
        }
	}
	
	public String getTag() {
		return Tag;
	}
	
	public void setTag(String tag) {
		Tag = tag;
	}
	public boolean isDeleted() {
		return _Deleted;
	}
	public void setDeleted(boolean _Deleted) {
		this._Deleted = _Deleted;
	}
	
	public abstract List<BaseDataObject> getChangeset(Context context, SQLiteDatabase db);
}
