package com.marcuscalidus.budgetenvelopes.network;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.marcuscalidus.budgetenvelopes.R;
import com.marcuscalidus.budgetenvelopes.dataobjects.EnvelopeDataObject;
import com.marcuscalidus.budgetenvelopes.db.DBMain;

import java.io.File;

public class DatabaseRestoreAsyncTask extends BudgetEnvelopesAsyncTask implements OnClickListener {

	static final String TAG = "DatabaseRestoreAsyncTask";
	
	private String backupDbName = "";
	private Context mContext;
	
	public DatabaseRestoreAsyncTask(Context context,
			GoogleApiClient googleApiClient) {
		super(context, googleApiClient);		
	}
	
	@Override
	protected void onCancelled (Boolean result) {
		logMessage(TAG, mContext.getResources().getString(R.string.operation_cancelled), true);
	}
	
	
	private class asyncFileChooser extends BudgetEnvelopesAsyncTask {

		private OnClickListener mOnClickListener;
				
		public ArrayAdapter<String> fileList = null;
		
		public asyncFileChooser(Context context, GoogleApiClient googleApiClient, OnClickListener onClickListener) {
			super(context, googleApiClient);
			mOnClickListener = onClickListener;
			fileList = new  ArrayAdapter<String>(mContext, android.R.layout.simple_list_item_single_choice);
		}
		
		
		@Override
		protected Boolean doInBackgroundConnected(Void... params) {
			MetadataBuffer mbr = Drive.DriveApi
				.getFolder(getGoogleApiClient(), getFolderDriveID())
					.queryChildren(getGoogleApiClient(), new Query.Builder()
																.addFilter(Filters.contains(SearchableField.TITLE, "BACKUP_"))
																//.addFilter(Filters.eq(SearchableField.MIME_TYPE, mimeType))
																.build()).await().getMetadataBuffer();
			
			for (int i = 0; i < mbr.getCount(); i++) {
				Metadata md = mbr.get(i);
				 
			    if (md.isEditable() && !md.isFolder() && !md.isTrashed()) {
			    	fileList.add(md.getTitle());
				}
			}
			mbr.close();		
			
			return true;
		}	
	    
	 	@Override
	    protected void onPostExecute(Boolean result) {
	        super.onPostExecute(result);
	    	if (result) {	  
	    		AlertDialog.Builder adb = new AlertDialog.Builder(mContext);	    		
				adb.setNegativeButton(mContext.getResources().getString(android.R.string.cancel), this.mOnClickListener);
				adb.setTitle(mContext.getResources().getString(R.string.title_restore));
	    		
	    		if (fileList.getCount() == 0) {
	    			adb.setMessage(mContext.getResources().getString(R.string.desc_no_backups));
	    		} else {	    		
		    		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
		    		View view = inflater.inflate(R.layout.alert_dialog_backup_select, null);
		    		ListView lw = (ListView) view.findViewById(R.id.listView);
					lw.setAdapter(fileList);	
					lw.setChoiceMode(ListView.CHOICE_MODE_SINGLE);	    
					
					adb.setPositiveButton(mContext.getResources().getString(android.R.string.ok), this.mOnClickListener);	
					adb.setView(view);
	    		}
				adb.show();

	    	}
	    }	
	}	

	public void execute(Context context) {
		mContext = context;
		
		new asyncFileChooser(context, getGoogleApiClient(), this).execute();		
	}
	
	@Override
	protected Boolean doInBackgroundConnected(Void... params) {
		try {		
			String syncDbName = "dbSync";
			String backupDbPath = mContext.getDatabasePath(backupDbName).toString();
			String mainDbPath = mContext.getDatabasePath(DBMain.DATABASE_NAME).toString();
			
			logMessage(TAG, mContext.getResources().getString(R.string.msg_attempt_file_download)+" - "+backupDbName, false);
			
			String localFile = downloadFile(backupDbPath, backupDbName, "application/x-sqlite3");
			
			if (localFile == null) {
				logMessage(TAG, mContext.getResources().getString(R.string.msg_error_db_broken)+" - "+backupDbName, false);
			} else {
				logMessage(TAG, mContext.getResources().getString(R.string.msg_downloaded_file)+" - "+backupDbName, false);
			}
			
			if (isCancelled()) {
				return false;
			}
									
			logMessage(TAG, mContext.getResources().getString(R.string.msg_attempt_file_upload)+" - "+backupDbName+" -> "+syncDbName, false);
				
			if (uploadFile(backupDbPath, syncDbName, "application/x-sqlite3") == null) {
				logMessage(TAG, mContext.getResources().getString(R.string.msg_error_upload_file)+" - "+backupDbName+" -> "+syncDbName, false);
				return false;
			}	
			logMessage(TAG, mContext.getResources().getString(R.string.msg_uploaded_file)+" - "+backupDbName+" -> "+syncDbName, false);
									
			DBMain.getInstance().getWritableDatabase().execSQL("vacuum"); 
			DBMain.getInstance().getWritableDatabase().close();
					
			logMessage(TAG, mContext.getResources().getString(R.string.msg_attempt_file_cleanup), false);
			
			File f = new File(mainDbPath);
			if (f.exists())
				f.delete();
			f=new File(mainDbPath+"-journal");
			if (f.exists())
				f.delete();
			
			File newFile = new File(mainDbPath);
			
			f = new File(backupDbPath);
				f.renameTo(newFile);

			logMessage(TAG, mContext.getResources().getString(R.string.msg_copied_file)+" - "+syncDbName+" -> "+DBMain.DATABASE_NAME, false);
				
			f=new File(backupDbPath+"-journal");
			if (f.exists())
				f.delete();
			
			logMessage(TAG, mContext.getResources().getString(R.string.msg_attempt_db_cleanup)+" - "+DBMain.DATABASE_NAME, false);
			
			DBMain.getInstance().getWritableDatabase().beginTransaction();
			DBMain.getInstance().getWritableDatabase().execSQL("update "+EnvelopeDataObject.TABLENAME+" set "+EnvelopeDataObject.FIELDNAME_EXPENSES+"=null, "+EnvelopeDataObject.FIELDNAME_EXPENSES+"=null");
			DBMain.getInstance().getWritableDatabase().execSQL("update "+EnvelopeDataObject.TABLENAME+" set "+EnvelopeDataObject.FIELDNAME_CHANGED+"=null");
			DBMain.getInstance().getWritableDatabase().setTransactionSuccessful();
			DBMain.getInstance().getWritableDatabase().endTransaction();			
			
			logMessage(TAG, mContext.getResources().getString(R.string.msg_restore_success), false);
			logMessage(TAG, mContext.getResources().getString(R.string.msg_ready_go_back), false);
			return true;
		} catch (Exception e) {
			logMessage(TAG, mContext.getResources().getString(R.string.msg_error_general), false);
			logMessage(TAG, e.getMessage(), false);
			return false;
		}
	}

	@Override
	public void onClick(DialogInterface dialog, int n) {
		switch (n) {
		case DialogInterface.BUTTON_POSITIVE :
			ListView lw = (ListView) ((AlertDialog)dialog).findViewById(R.id.listView);
			if (lw.getCheckedItemPosition() >= 0) {
				backupDbName = (String) lw.getAdapter().getItem(lw.getCheckedItemPosition());
				super.execute();		
			} else {
				logMessage(TAG, mContext.getResources().getString(R.string.msg_error_no_selection), false);
				getGoogleApiClient().disconnect();
				getOnExecuteListener().notifyPostExecute(mContext, false);
			}
			break;
		case DialogInterface.BUTTON_NEGATIVE :
			getGoogleApiClient().disconnect();
			if (getOnExecuteListener() != null) {
				getOnExecuteListener().notifyPostExecute(mContext, false);
			}
			break;
		}
	}

}
