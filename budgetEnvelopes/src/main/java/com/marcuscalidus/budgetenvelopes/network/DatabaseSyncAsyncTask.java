package com.marcuscalidus.budgetenvelopes.network;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.marcuscalidus.budgetenvelopes.BudgetEnvelopes;
import com.marcuscalidus.budgetenvelopes.R;
import com.marcuscalidus.budgetenvelopes.dataobjects.BaseDataObject;
import com.marcuscalidus.budgetenvelopes.dataobjects.EnvelopeDataObject;
import com.marcuscalidus.budgetenvelopes.dataobjects.SettingsDataObject;
import com.marcuscalidus.budgetenvelopes.db.DBMain;
import com.marcuscalidus.budgetenvelopes.db.DatabaseDefinition;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class DatabaseSyncAsyncTask extends BudgetEnvelopesAsyncTask {

	static final String TAG = "DatabaseSyncAsyncTask";
	
	public DatabaseSyncAsyncTask(Context context,
			GoogleApiClient googleApiClient) {
		super(context, googleApiClient);		
	}
	
	@Override
	protected void onCancelled (Boolean result) {
		logMessage(TAG, mContext.getResources().getString(R.string.operation_cancelled));
	}
	
	protected void cleanupBackups(int backupCount) {
		logMessage(TAG, String.format(mContext.getResources().getString(R.string.msg_attempt_deleting_outdated_backups), backupCount));
		
		MetadataBuffer mbr = Drive.DriveApi
				.getFolder(getGoogleApiClient(), getFolderDriveID())
					.queryChildren(getGoogleApiClient(), new Query.Builder()
																.addFilter(Filters.contains(SearchableField.TITLE, "BACKUP_"))
																.build()).await().getMetadataBuffer();
		
		ArrayList<Metadata> alm = new ArrayList<Metadata>();
			
		for (int i = 0; i < mbr.getCount(); i++) {			
			alm.add(mbr.get(i));
		}
		
		
		Collections.sort(alm, new Comparator<Metadata>() {
            @Override
            public int compare(Metadata md1, Metadata md2) {
                return (md2.getModifiedDate()).compareTo(md1.getModifiedDate());
            }
        });
		
		for (int i=0; i < alm.size(); i++) {
			if (i>backupCount - 1) {
				String s = alm.get(i).getTitle();
				
				if (deleteContents(Drive.DriveApi.getFile(getGoogleApiClient(), alm.get(i).getDriveId()))) {
					logMessage(TAG, String.format(mContext.getResources().getString(R.string.msg_file_delete_success), s));
				} else {
					logMessage(TAG, String.format(mContext.getResources().getString(R.string.msg_file_delete_fail), s));
				}					
			}
		}			

		mbr.close();	
	}	

	@Override
	protected Boolean doInBackgroundConnected(Void... params) {
		try {
			if (!syncDrive()) {
				return false;
			}
			
			String syncDbName = "dbSync";
			String syncDbPath = mContext.getDatabasePath(syncDbName).toString();
			String mainDbPath = mContext.getDatabasePath(DBMain.DATABASE_NAME).toString();
			
			logMessage(TAG, mContext.getResources().getString(R.string.msg_attempt_file_download)+" - "+syncDbName);
			
			String localFile = downloadFile(syncDbPath, syncDbName, "application/x-sqlite3");
			
			if (localFile == null) {
				try {
					copyFile(mainDbPath, syncDbPath);
					logMessage(TAG, mContext.getResources().getString(R.string.msg_copied_file)+" - "+DBMain.DATABASE_NAME+" -> "+syncDbName);
				} catch (IOException e) {
					logMessage(TAG, mContext.getResources().getString(R.string.msg_error_copy_file)+" - "+DBMain.DATABASE_NAME+" -> "+syncDbName);
					logMessage(TAG, e.getMessage());
					return false;
				}
			} else {
				logMessage(TAG, mContext.getResources().getString(R.string.msg_downloaded_file)+" - "+syncDbName);
			}
			
			if (isCancelled()) {
				return false;
			}
						
			List<BaseDataObject> changeSet = DBMain.getInstance().getChangeset(mContext);
			if (changeSet.size() == 0) {
				logMessage(TAG, mContext.getResources().getString(R.string.msg_no_local_sync_data));
			} else {
				logMessage(TAG, mContext.getResources().getString(R.string.msg_attempt_apply_local_data));				
				
				DatabaseDefinition dbSync = new DatabaseDefinition(mContext, syncDbName);
				SQLiteDatabase dbwrite = dbSync.getWritableDatabase();
				dbwrite.beginTransaction();
				
				for (int i = 0; i < changeSet.size(); i++) {
					changeSet.get(i).insertOrReplaceIntoDb(dbwrite, false);
					//publishProgress(changeSet.get(i).toString());
				}
				
				dbwrite.setTransactionSuccessful();
				dbwrite.endTransaction();
				
				dbwrite.close();

				logMessage(TAG, mContext.getResources().getString(R.string.msg_local_data_update_count)+ ": "+changeSet.size());
				
				logMessage(TAG, mContext.getResources().getString(R.string.msg_attempt_db_cleanup)+" - "+syncDbName);
				
				if (!dbSync.cleanup()) {
					logMessage(TAG, mContext.getResources().getString(R.string.msg_error_db_broken)+" - "+syncDbName);
					return false;
				}		
			}
			
			if (isCancelled()) {
				return false;
			}
				
			logMessage(TAG, mContext.getResources().getString(R.string.msg_attempt_file_upload)+" - "+syncDbName);
				
			if (uploadFile(syncDbPath, syncDbName, "application/x-sqlite3") == null) {
				logMessage(TAG, mContext.getResources().getString(R.string.msg_error_upload_file)+" - "+syncDbName);
				return false;
			}	
			logMessage(TAG, mContext.getResources().getString(R.string.msg_uploaded_file)+" - "+syncDbName);
			
			Calendar cal = Calendar.getInstance();
			String backupName = String.format(Locale.GERMAN, "BACKUP_%04d%02d%02d_%02d%02d%02d", 
					cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH),
					cal.get(Calendar.HOUR_OF_DAY),cal.get(Calendar.MINUTE),cal.get(Calendar.SECOND));
			
			logMessage(TAG, mContext.getResources().getString(R.string.msg_attempt_file_upload)+" - "+backupName);
			 
			if (uploadFile(syncDbPath, backupName, "application/x-sqlite3") == null) {
				logMessage(TAG, mContext.getResources().getString(R.string.msg_error_upload_file)+" - "+backupName);
				return false;
			}	
			logMessage(TAG, mContext.getResources().getString(R.string.msg_uploaded_file)+" - "+backupName);
			
			cleanupBackups(Integer.parseInt(BudgetEnvelopes.getCurrentSettingValue(SettingsDataObject.UUID_BACKUP_COUNT, "5")));			
			
			DBMain.getInstance().getWritableDatabase().execSQL("vacuum"); //cleanup possible journal data
			DBMain.getInstance().getWritableDatabase().close();
					
			logMessage(TAG, mContext.getResources().getString(R.string.msg_attempt_file_cleanup));
			
			File f = new File(mainDbPath);
			if (f.exists())
				f.delete();
			f=new File(mainDbPath+"-journal");
			if (f.exists())
				f.delete();
			

			File newFile = new File(mainDbPath);
			
			f = new File(syncDbPath);
				f.renameTo(newFile);

			logMessage(TAG, mContext.getResources().getString(R.string.msg_copied_file)+" - "+syncDbName+" -> "+DBMain.DATABASE_NAME);		
				
			f=new File(syncDbPath+"-journal");
			if (f.exists())
				f.delete();
			
			logMessage(TAG, mContext.getResources().getString(R.string.msg_attempt_db_cleanup)+" - "+DBMain.DATABASE_NAME);
			
			DBMain.getInstance().getWritableDatabase().beginTransaction();
			DBMain.getInstance().getWritableDatabase().execSQL("update "+EnvelopeDataObject.TABLENAME+" set "+EnvelopeDataObject.FIELDNAME_EXPENSES+"=null, "+EnvelopeDataObject.FIELDNAME_EXPENSES+"=null");
			DBMain.getInstance().getWritableDatabase().execSQL("update "+EnvelopeDataObject.TABLENAME+" set "+EnvelopeDataObject.FIELDNAME_CHANGED+"=null");
			DBMain.getInstance().getWritableDatabase().setTransactionSuccessful();
			DBMain.getInstance().getWritableDatabase().endTransaction();	
			
			logMessage(TAG, mContext.getResources().getString(R.string.msg_sync_success));
			logMessage(TAG, mContext.getResources().getString(R.string.msg_ready_go_back));
			return true;
		} catch (Exception e) {
			logMessage(TAG, mContext.getResources().getString(R.string.msg_error_general));
			logMessage(TAG, e.getMessage());
			return false;
		}
	}

}
