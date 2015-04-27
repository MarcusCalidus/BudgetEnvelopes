package com.marcuscalidus.budgetenvelopes;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.marcuscalidus.budgetenvelopes.dataobjects.SettingsDataObject;
import com.marcuscalidus.budgetenvelopes.db.DBMain;
import com.yasesprox.android.transcommusdk.TransCommuActivity;


public class SettingsFragment extends PreferenceFragment {
	
	public static void savePreferencesToDb() {
		SQLiteDatabase db = DBMain.getInstance().getWritableDatabase();
				
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(BudgetEnvelopes.getAppContext());

		SettingsDataObject sdo = SettingsDataObject.getSetting(BudgetEnvelopes.getAppContext(), db, SettingsDataObject.UUID_SYNC_ACCOUNT);
		sdo.setValue(prefs.getString(SettingsDataObject.UUID_SYNC_ACCOUNT.toString(), ""));
		sdo.insertOrReplaceIntoDb(db, true);
		
		sdo = SettingsDataObject.getSetting(BudgetEnvelopes.getAppContext(), db, SettingsDataObject.UUID_CURRENCY_SYMBOL);
		sdo.setValue(prefs.getString(SettingsDataObject.UUID_CURRENCY_SYMBOL.toString(), ""));
		sdo.insertOrReplaceIntoDb(db, true);

        sdo = SettingsDataObject.getSetting(BudgetEnvelopes.getAppContext(), db, SettingsDataObject.UUID_BACKUP_COUNT);
        sdo.setValue(prefs.getString(SettingsDataObject.UUID_BACKUP_COUNT.toString(), "5"));
        sdo.insertOrReplaceIntoDb(db, true);

        sdo = SettingsDataObject.getSetting(BudgetEnvelopes.getAppContext(), db, SettingsDataObject.UUID_SYNC_ON_START);
        sdo.setValue(Boolean.toString(prefs.getBoolean(SettingsDataObject.UUID_SYNC_ON_START.toString(), false)));
        sdo.insertOrReplaceIntoDb(db, true);
	}
		
	@Override 
	public void onStop() {
		super.onStop();
		savePreferencesToDb();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {		
		DBMain.initPreferencesFromDb();
		
		super.onCreate(savedInstanceState);
		  
		// Load the preferences from an XML resource
		addPreferencesFromResource(R.xml.preferences);
		
		Preference button = (Preference)findPreference("manual");
		button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
		                @Override
		                public boolean onPreferenceClick(Preference pref) { 
		                	startActivity(new Intent(Intent.ACTION_VIEW, 
		                		    Uri.parse(getActivity().getResources().getString(R.string.pref_link_manual))));
		                    return true;
		                }
		            });

		button = (Preference)findPreference("privacy");
		button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
		                @Override
		                public boolean onPreferenceClick(Preference pref) {
		                	startActivity(new Intent(Intent.ACTION_VIEW,
		                		    Uri.parse(getActivity().getResources().getString(R.string.pref_link_privacy))));
		                    return true;
		                }
		            });

        button = (Preference)findPreference("translate");
        button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference pref) {
                Intent intent = new Intent(BudgetEnvelopes.getAppContext(), TransCommuActivity.class);
                intent.putExtra(TransCommuActivity.APPLICATION_CODE_EXTRA, "LZToVmhhWy");
                startActivity(intent);
                return true;
            }
        });

		EditTextPreference editTextPreference = (EditTextPreference) findPreference(SettingsDataObject.UUID_CURRENCY_SYMBOL.toString());
		if (editTextPreference != null) {
			editTextPreference.setSummary(editTextPreference.getSummary()+" "+BudgetEnvelopes.getCurrentSettingValue(SettingsDataObject.UUID_CURRENCY_SYMBOL, ""));
		}
		
		ListPreference listPreferenceCategory = (ListPreference) findPreference(SettingsDataObject.UUID_SYNC_ACCOUNT.toString());
		if (listPreferenceCategory != null) {
			AccountManager accountManager = AccountManager.get(BudgetEnvelopes.getAppContext());
			Account[] accounts = accountManager.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
		 
		   
			CharSequence entries[] = new String[accounts.length];
			CharSequence entryValues[] = new String[accounts.length];
			int i = 0;
			for (Account account : accounts) {
				entries[i] = account.name;
				entryValues[i] = account.name;
				i++;
			}
			listPreferenceCategory.setEntries(entries);
			listPreferenceCategory.setEntryValues(entryValues);
		}
	}
}
