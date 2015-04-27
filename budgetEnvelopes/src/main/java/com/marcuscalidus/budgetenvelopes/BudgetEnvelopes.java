package com.marcuscalidus.budgetenvelopes;

import android.app.Application;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask.Status;
import android.preference.PreferenceManager;

import com.google.android.gms.common.api.GoogleApiClient;
import com.marcuscalidus.budgetenvelopes.dataobjects.SettingsDataObject;
import com.marcuscalidus.budgetenvelopes.db.DBMain;
import com.marcuscalidus.budgetenvelopes.network.BudgetEnvelopesAsyncTask;
import com.marcuscalidus.budgetenvelopes.network.BudgetEnvelopesSyncService;
import com.marcuscalidus.budgetenvelopes.network.DatabaseRestoreAsyncTask;
import com.marcuscalidus.budgetenvelopes.network.DatabaseSyncAsyncTask;
import com.marcuscalidus.budgetenvelopes.transactions.TransactionDialogFragment.OnTransactionUpdateListener;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class BudgetEnvelopes extends Application {
	
	private static FragmentManager fragmentManager;
	private static OnTransactionUpdateListener onTransactionUpdateListener;
    private static Context context;
    private static BudgetEnvelopes instance;
    
    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);
    
    private static HashMap<String, Drawable> stampAssets = new HashMap<String, Drawable>();
    
    private DatabaseSyncAsyncTask syncTask = null;
    private DatabaseRestoreAsyncTask restoreTask = null;
    
	@Override
	public void onCreate() {
		super.onCreate();
		BudgetEnvelopes.context = getApplicationContext();
		BudgetEnvelopes.instance = this;	
		DBMain.initPreferencesFromDb();

        if (getCurrentSettingValue(SettingsDataObject.UUID_SYNC_ON_START, false))
        {
            startService(new Intent(this, BudgetEnvelopesSyncService.class));
        }
    }
	
	public static BudgetEnvelopes getInstance() {
		return BudgetEnvelopes.instance;
	}
	
	public static Drawable getStampAsset(String stampName) {
		try {
			if (!stampAssets.containsKey(stampName)) {
				Drawable d = Drawable.createFromStream(BudgetEnvelopes.context.getAssets().open("stamps/"+stampName), null);
				BudgetEnvelopes.stampAssets.put(stampName, d);		
			}
	
			return BudgetEnvelopes.stampAssets.get(stampName);
		}
		catch (Exception e) {
			return null;
		}
	}
	
	public static int generateViewId() {
	    for (;;) {
	        final int result = sNextGeneratedId.get();
	        // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
	        int newValue = result + 1;
	        if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
	        if (sNextGeneratedId.compareAndSet(result, newValue)) {
	            return result;
	        }
	    }
	}
	
	public static Double parseDoubleSafe(String s){
        Double val=Double.valueOf(0);
        try{
            val=Double.valueOf(s);
        }catch(NumberFormatException ex){
            DecimalFormat df=new DecimalFormat();
            Number n=null;
            try {
                  n=df.parse(s);
            } catch (ParseException e) {
            }
            if(n!=null)
                val=n.doubleValue();
        }
        return val;
    }
 
    public static Context getAppContext() {
        return BudgetEnvelopes.context;
    }
    
    public static FragmentManager getFragmentManager() {
    	return BudgetEnvelopes.fragmentManager;
    }
    
    public static void setFragmentManager(FragmentManager fragmentManager) {
    	BudgetEnvelopes.fragmentManager = fragmentManager;
    }

	public static OnTransactionUpdateListener getOnTransactionUpdateListener() {
		return onTransactionUpdateListener;
	}

    public static String getCurrentSettingValue(UUID setting, String defaultValue) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(BudgetEnvelopes.getAppContext());
        return prefs.getString(setting.toString(), defaultValue);
    }

    public static boolean getCurrentSettingValue(UUID setting, Boolean defaultValue) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(BudgetEnvelopes.getAppContext());
        return prefs.getBoolean(setting.toString(), defaultValue);
    }

	public static void setOnTransactionUpdateListener(
			OnTransactionUpdateListener onTransactionUpdateListener) {
		BudgetEnvelopes.onTransactionUpdateListener = onTransactionUpdateListener;
	}
	
	public BudgetEnvelopesAsyncTask getRunningTask() {
		return syncTask;
	}
	
	public DatabaseSyncAsyncTask getSyncTask(GoogleApiClient googleApiClient) {
		if ((syncTask == null) || (syncTask.getStatus() == Status.FINISHED)) {
			syncTask = new DatabaseSyncAsyncTask(this, googleApiClient);
		}	
		return syncTask;
	}
	
	public DatabaseRestoreAsyncTask getRestoreTask(GoogleApiClient googleApiClient) {
		if ((restoreTask == null) || (restoreTask.getStatus() == Status.FINISHED)) {
			restoreTask = new DatabaseRestoreAsyncTask(this, googleApiClient);
		}	
		return restoreTask;
	}
}
