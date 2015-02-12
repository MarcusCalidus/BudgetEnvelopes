package com.marcuscalidus.budgetenvelopes.network;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.AnimationDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.plus.Plus;
import com.marcuscalidus.budgetenvelopes.BudgetEnvelopes;
import com.marcuscalidus.budgetenvelopes.R;
import com.marcuscalidus.budgetenvelopes.dataobjects.SettingsDataObject;
import com.marcuscalidus.budgetenvelopes.db.DBMain;
import com.marcuscalidus.budgetenvelopes.network.BudgetEnvelopesAsyncTask.OnLogMessageListener;
import com.marcuscalidus.budgetenvelopes.network.BudgetEnvelopesAsyncTask.OnNotificationListener;
import com.marcuscalidus.budgetenvelopes.widgets.TooltipHoverListener;

import java.util.ArrayList;

public class SyncActivity extends Activity 
							implements GoogleApiClient.ConnectionCallbacks,
			 							GoogleApiClient.OnConnectionFailedListener, OnClickListener, OnLogMessageListener{
	
	private static final int RESOLVE_CONNECTION_REQUEST_CODE = 1;
	
	private static final String EXTRA_CURRENT_ASYNC_REQUEST = "current_async_request";

	private static final int EXTRA_ASYNC_REQUEST_NONE = 0;	
	private static final int EXTRA_ASYNC_REQUEST_SYNC_N_BACKUP = 1;	
	private static final int EXTRA_ASYNC_REQUEST_RESTORE = 2;	
	
	private static final String KEY_LOG_MESSAGES = "LOG_MESSAGES";

	private GoogleApiClient mGoogleApiClient;

	private ArrayList<String> messages = null;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);      
        
        setContentView(R.layout.activity_sync);
        
        Button btn = (Button) findViewById(R.id.btnSync);
        btn.setOnClickListener(this);      
        btn.setOnHoverListener(TooltipHoverListener.getInstance());
        btn = (Button) findViewById(R.id.btnRestore);
        btn.setOnClickListener(this);      
        btn.setOnHoverListener(TooltipHoverListener.getInstance());
        

        this.getIntent().putExtra(EXTRA_CURRENT_ASYNC_REQUEST, EXTRA_ASYNC_REQUEST_NONE);
        
        SQLiteDatabase db = DBMain.getInstance().getReadableDatabase();
	    mGoogleApiClient = new GoogleApiClient.Builder(this)
	            .addApi(Drive.API)
	            .addApi(Plus.API)
	            .addScope(Drive.SCOPE_FILE)
	            .setAccountName(SettingsDataObject.getSetting(this, db, SettingsDataObject.UUID_SYNC_ACCOUNT).getValue()) 
	            .addConnectionCallbacks(this)
	            .addOnConnectionFailedListener(this).build();
	}
	    
	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		if (connectionResult.hasResolution()) {
	        try {
	            connectionResult.startResolutionForResult(this, RESOLVE_CONNECTION_REQUEST_CODE);
	        } catch (IntentSender.SendIntentException e) {
	            logMessage(e.getMessage());
	        }
	    } else {
	        GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), this, 0).show();
	    }		
	}
	
	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
	    switch (requestCode) {
	        case RESOLVE_CONNECTION_REQUEST_CODE:
	            if (resultCode == RESULT_OK) {
	                mGoogleApiClient.connect();
	            }
	            break;
	    }
	}

	@Override
	public void onConnected(Bundle bundle) {
		hideToolbar();
		
		logMessage(this.getResources().getString(R.string.msg_using_account) + " - " +  Plus.AccountApi.getAccountName(mGoogleApiClient));
		
		switch (this.getIntent().getExtras().getInt(EXTRA_CURRENT_ASYNC_REQUEST)) {
		case EXTRA_ASYNC_REQUEST_SYNC_N_BACKUP : 		
		 	DatabaseSyncAsyncTask dbSyncTask = BudgetEnvelopes.getInstance().getSyncTask(mGoogleApiClient);
		 	dbSyncTask.setOnLogMessageListener(this);
		 	dbSyncTask.setOnExecuteListener(new OnNotificationListener() {
				
				@Override
				public void notifyPreExecute(Context context) {
					// TODO Auto-generated method stub				
				}
				
				@Override
				public void notifyPostExecute(Context context, Boolean result) {
					mGoogleApiClient.disconnect();	
					showToolbar();
				}
			});
            try {
                dbSyncTask.execute();
            } catch (Exception e) {
                logMessage(e.getMessage());
            }

		 	break;
		case EXTRA_ASYNC_REQUEST_RESTORE : 
			DatabaseRestoreAsyncTask dbRestoreTask = BudgetEnvelopes.getInstance().getRestoreTask(mGoogleApiClient);
			dbRestoreTask.setOnLogMessageListener(this);
			dbRestoreTask.setOnExecuteListener(new OnNotificationListener() {
				
				@Override
				public void notifyPreExecute(Context context) {
					// TODO Auto-generated method stub				
				}
				
				@Override
				public void notifyPostExecute(Context context, Boolean result) {
					mGoogleApiClient.disconnect();
					showToolbar();
				}
			});
			dbRestoreTask.execute(this);
		 	break;
		case EXTRA_ASYNC_REQUEST_NONE : 
			logMessage("oops");
			break;
		}
	 	
	}

	@Override
	public void onConnectionSuspended(int cause) {
		showToolbar();
		
		switch (cause) {
		case ConnectionCallbacks.CAUSE_NETWORK_LOST:

			logMessage(this.getResources().getString(R.string.msg_network_lost));			
			break;

		case ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED:
			logMessage(this.getResources().getString(R.string.msg_service_disconnected));
			break;
		}		
	}
	
	@Override
	public void onBackPressed() {
		final BudgetEnvelopesAsyncTask beAsyncTask = BudgetEnvelopes.getInstance().getRunningTask();
		
		if ((beAsyncTask == null) || (beAsyncTask.getStatus() == AsyncTask.Status.FINISHED)) {
			super.onBackPressed();
		} else {
			Resources res = getResources();
			AlertDialog dialog = new AlertDialog.Builder(this).create();
		    dialog.setTitle(res.getString(R.string.cancel_operation));
		    dialog.setMessage(res.getString(R.string.cancel_operation_question));
	
		    dialog.setButton(DialogInterface.BUTTON_POSITIVE, res.getString(android.R.string.yes), new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int buttonId) {
		        	beAsyncTask.cancel(true);
		        }
		    });
		  			        
		    dialog.setButton(DialogInterface.BUTTON_NEGATIVE, res.getString(android.R.string.no), new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int buttonId) {
		        	return;
		        }
		    });
            
		    dialog.setIcon(android.R.drawable.ic_dialog_alert);
		    dialog.show();	
		}			
	}

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.btnSync :
			this.getIntent().putExtra(EXTRA_CURRENT_ASYNC_REQUEST, EXTRA_ASYNC_REQUEST_SYNC_N_BACKUP);
	        mGoogleApiClient.connect();
			break;
		case R.id.btnRestore :
			this.getIntent().putExtra(EXTRA_CURRENT_ASYNC_REQUEST, EXTRA_ASYNC_REQUEST_RESTORE);
	        mGoogleApiClient.connect();
			break;
		}		
	}

	@Override
	public void onPause() {
		super.onPause();
		this.getIntent().putExtra(KEY_LOG_MESSAGES, messages);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		Bundle args = this.getIntent().getExtras();
		
		if (args != null) {
			messages = args.getStringArrayList(KEY_LOG_MESSAGES);
		}
		
		if (messages == null) {
			messages = new ArrayList<String>(); 
		} else {
			for (String s:messages) {
				addMessageView(s);
			}
		}

	}
	
	@Override
	 public void onWindowFocusChanged(boolean hasFocus) {
	  super.onWindowFocusChanged(hasFocus);
	  
	  int screenWidth = getResources().getDisplayMetrics().widthPixels;
  	
	  View v = this.findViewById(R.id.btnSync);
	  	if ((v != null) && (2 * v.getWidth() > screenWidth))
	  	{
	  		LinearLayout ll = (LinearLayout) this.findViewById(R.id.toolbar);
	  		if (ll != null)
	  			ll.setOrientation(LinearLayout.VERTICAL);
	  	}
	 }
	
	public void hideToolbar() {
		View v = findViewById(R.id.toolbar);
		v.animate().translationY(-1 * v.getHeight()).start();

		ImageView iv = (ImageView) findViewById(R.id.waitImage);
		AnimationDrawable ani =  (AnimationDrawable) iv.getDrawable();
		ani.start();
		iv.animate().alpha(1).start();
	}
	
	public void showToolbar() {
		View v = findViewById(R.id.toolbar);
		v.animate().translationY(0).start();
		
		ImageView iv = (ImageView) findViewById(R.id.waitImage);
		AnimationDrawable ani =  (AnimationDrawable) iv.getDrawable();
		ani.stop(); 
		iv.animate().alpha(0).start();
	}
	
	@Override
	public void logMessage(String message) {
		messages.add(message);		
		addMessageView(message);
	}

	private void addMessageView(String message) {
		LinearLayout ll = (LinearLayout) findViewById(R.id.layoutMessages);
		TextView tv = new TextView(this);
		tv.setText(message);
		tv.setTextColor(Color.DKGRAY);
		ll.addView(tv);
	}

}
