package com.marcuscalidus.budgetenvelopes.db;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.media.audiofx.BassBoost;
import android.preference.PreferenceManager;

import com.marcuscalidus.budgetenvelopes.BudgetEnvelopes;
import com.marcuscalidus.budgetenvelopes.dataobjects.SettingsDataObject;
public class DBMain extends DatabaseDefinition {

	public static final String DATABASE_NAME = "dbMain";

	private static DBMain mInstance;
	
	private DBMain(Context context) {
		super(context, DATABASE_NAME);
	}

	public static void initPreferencesFromDb() {
		SQLiteDatabase db = DBMain.getInstance().getReadableDatabase();
				
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(BudgetEnvelopes.getAppContext());
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(SettingsDataObject.UUID_SYNC_ACCOUNT.toString(), SettingsDataObject.getSetting(BudgetEnvelopes.getAppContext(), db, SettingsDataObject.UUID_SYNC_ACCOUNT).getValue());
		editor.putString(SettingsDataObject.UUID_CURRENCY_SYMBOL.toString(), SettingsDataObject.getSetting(BudgetEnvelopes.getAppContext(), db, SettingsDataObject.UUID_CURRENCY_SYMBOL).getValue());
		editor.putString(SettingsDataObject.UUID_BACKUP_COUNT.toString(), SettingsDataObject.getSetting(BudgetEnvelopes.getAppContext(), db, SettingsDataObject.UUID_BACKUP_COUNT).getValue());
        editor.putBoolean(SettingsDataObject.UUID_SYNC_ON_START.toString(), Boolean.parseBoolean(SettingsDataObject.getSetting(BudgetEnvelopes.getAppContext(), db, SettingsDataObject.UUID_SYNC_ON_START).getValue()));
        editor.commit();
	}
	
	public static DBMain getInstance() {
		if (mInstance == null) {
			mInstance = new DBMain(BudgetEnvelopes.getAppContext());
		}
		return mInstance;
	}
}
