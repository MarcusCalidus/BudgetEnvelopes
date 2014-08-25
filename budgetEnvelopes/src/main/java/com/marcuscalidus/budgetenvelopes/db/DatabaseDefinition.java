package com.marcuscalidus.budgetenvelopes.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.marcuscalidus.budgetenvelopes.dataobjects.BaseDataObject;
import com.marcuscalidus.budgetenvelopes.dataobjects.EnvelopeDataObject;
import com.marcuscalidus.budgetenvelopes.dataobjects.ExpenseDataObject;
import com.marcuscalidus.budgetenvelopes.dataobjects.SettingsDataObject;
import com.marcuscalidus.budgetenvelopes.dataobjects.TransactionDataObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DatabaseDefinition extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 3;
	
	private final BaseDataObject[] dataObjects = { 
			new SettingsDataObject(null, null),
			new EnvelopeDataObject(null, null),
			new ExpenseDataObject(null, new UUID(0, 0)),
			new TransactionDataObject(null, null) };
	
	public DatabaseDefinition(Context context, String databaseName) {
		super(context, databaseName, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		for (int i = 0; i < dataObjects.length; i++) {
			dataObjects[i].createTableInDb(db);
			dataObjects[i].createFieldsInDb(db);
		}
		
		for (int i = 0; i < dataObjects.length; i++) {
			dataObjects[i].createTriggersInDb(db);
		}		
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		onCreate(db);
	}
	
	public Boolean cleanup() {
		SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();
		
		for (int i = dataObjects.length - 1; i >= 0 ; i--) {
			db.execSQL("update "+dataObjects[i].getTableName()+
					   " set "+BaseDataObject.FIELDNAME_CHANGED+"=NULL"+
					   " where "+BaseDataObject.FIELDNAME_CHANGED+" IS NOT NULL;");
			dataObjects[i].createFieldsInDb(db);
		}
		
		db.setTransactionSuccessful();
		db.endTransaction();
		
		db.execSQL("vacuum;");
		
		Boolean result = db.isDatabaseIntegrityOk();
		
		db.close();
		
		return result;
	}
	
	public List<BaseDataObject> getChangeset(Context context) {
		List<BaseDataObject> resultSet = new ArrayList<BaseDataObject>();
		
		List<BaseDataObject> subSet;
		
		for (int i = 0; i < dataObjects.length; i++) {
			subSet = dataObjects[i].getChangeset(context, getReadableDatabase());
			
			for (int j = 0; j < subSet.size(); j++) 
				resultSet.add(subSet.get(j));
		}
		
		return resultSet;
	}
}
