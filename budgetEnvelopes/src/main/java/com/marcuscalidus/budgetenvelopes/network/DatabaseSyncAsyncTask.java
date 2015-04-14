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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class DatabaseSyncAsyncTask extends BudgetEnvelopesAsyncTask {

	static final String TAG = "DatabaseSyncAsyncTask";
	
	public DatabaseSyncAsyncTask(Context context,
			GoogleApiClient googleApiClient) {
		super(context, googleApiClient);		
	}
	
	@Override
	protected void onCancelled (Boolean result) {
        logMessageCancelable(TAG, mContext.getResources().getString(R.string.operation_cancelled), true);
	}
	
	protected void cleanupBackups(int backupCount) {
        logMessageCancelable(TAG, String.format(mContext.getResources().getString(R.string.msg_attempt_deleting_outdated_backups), backupCount), true);
		
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
                    logMessage(TAG, String.format(mContext.getResources().getString(R.string.msg_file_delete_success), s), true);
				} else {
                    logMessageCancelable(TAG, String.format(mContext.getResources().getString(R.string.msg_file_delete_fail), s), true);
				}					
			}
		}			

		mbr.close();	
	}

    protected Boolean isSyncNecessary() throws Exception {
        char[] buffer = new char[1024];

        String remoteTokenName = "remoteSyncToken";
        String tokenPath = mContext.getFileStreamPath(remoteTokenName).toString();

        String tokenFileName = downloadFile(tokenPath, remoteTokenName, "text/plain");

        if (tokenFileName == null)
            return true;

        FileInputStream tokenStream = new FileInputStream(tokenFileName);
        InputStreamReader tokenReader = new InputStreamReader(tokenStream);

        StringBuffer remoteToken = new StringBuffer("");

        int n;
        while ((n = tokenReader.read(buffer)) != -1)
        {
            remoteToken.append(buffer, 0, n);
        }

        File f = mContext.getFileStreamPath("localSyncToken");
        if (f.exists()) {
            tokenFileName = f.toString();

            tokenStream = new FileInputStream(tokenFileName);
            tokenReader = new InputStreamReader(tokenStream);

            StringBuffer localToken = new StringBuffer("");

            while ((n = tokenReader.read(buffer)) != -1) {
                localToken.append(new String(buffer, 0, n));
            }

            return !remoteToken.toString().contentEquals(localToken.toString());
        }
        else
        {
            return true;
        }
    }


    protected void updateSyncToken() throws Exception {
        byte[] buffer = new byte[1024];
        String remoteTokenName = "remoteSyncToken";

        logMessage(TAG, "updating sync token.", true);

        String tokenFileName = mContext.getFileStreamPath("localSyncToken").toString();
        FileOutputStream tokenStream = new FileOutputStream(tokenFileName);

        tokenStream.write(UUID.randomUUID().toString().getBytes());
        tokenStream.close();

        uploadFile(tokenFileName, remoteTokenName, "text/plain");
    }

	@Override
	protected Boolean doInBackgroundConnected(Void... params) {
		try {
			if (!syncDrive()) {
				return false;
			}

            if (!isSyncNecessary()) {
                logMessage(TAG, mContext.getResources().getString(R.string.msg_sync_success), true);
                logMessage(TAG, mContext.getResources().getString(R.string.msg_ready_go_back), true);
                mNotificationManager.cancel(0);
                return true;
            }

			String syncDbName = "dbSync";
			String syncDbPath = mContext.getDatabasePath(syncDbName).toString();
			String mainDbPath = mContext.getDatabasePath(DBMain.DATABASE_NAME).toString();
			
			logMessage(TAG, mContext.getResources().getString(R.string.msg_attempt_file_download)+" - "+syncDbName, true);
			
			String localFile = downloadFile(syncDbPath, syncDbName, "application/x-sqlite3");
			
			if (localFile == null) {
				try {
					copyFile(mainDbPath, syncDbPath);
					logMessage(TAG, mContext.getResources().getString(R.string.msg_copied_file)+" - "+DBMain.DATABASE_NAME+" -> "+syncDbName, true);
				} catch (IOException e) {
					logMessageCancelable(TAG, mContext.getResources().getString(R.string.msg_error_copy_file)+" - "+DBMain.DATABASE_NAME+" -> "+syncDbName, true);
					logMessageCancelable(TAG, e.getMessage(), true);
					return false;
				}
			} else {
                logMessage(TAG, mContext.getResources().getString(R.string.msg_downloaded_file)+" - "+syncDbName, true);
			}
			
			if (isCancelled()) {
                logMessageCancelable(TAG, "", true);
				return false;
			}
						
			List<BaseDataObject> changeSet = DBMain.getInstance().getChangeset(mContext);
			if (changeSet.size() == 0) {
				logMessage(TAG, mContext.getResources().getString(R.string.msg_no_local_sync_data), true);
			} else {
				logMessage(TAG, mContext.getResources().getString(R.string.msg_attempt_apply_local_data), true);
				
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

				logMessage(TAG, mContext.getResources().getString(R.string.msg_local_data_update_count)+ ": "+changeSet.size(), true);
				
				logMessage(TAG, mContext.getResources().getString(R.string.msg_attempt_db_cleanup)+" - "+syncDbName, true);
				
				if (!dbSync.cleanup()) {
                    logMessageCancelable(TAG, mContext.getResources().getString(R.string.msg_error_db_broken)+" - "+syncDbName, true);
					return false;
				}		
			}
			
			if (isCancelled()) {
				return false;
			}
				
			logMessage(TAG, mContext.getResources().getString(R.string.msg_attempt_file_upload)+" - "+syncDbName, true);
				
			if (uploadFile(syncDbPath, syncDbName, "application/x-sqlite3") == null) {
                logMessageCancelable(TAG, mContext.getResources().getString(R.string.msg_error_upload_file)+" - "+syncDbName, true);
				return false;
			}	
			logMessage(TAG, mContext.getResources().getString(R.string.msg_uploaded_file)+" - "+syncDbName, true);
			
			Calendar cal = Calendar.getInstance();
			String backupName = String.format(Locale.GERMAN, "BACKUP_%04d%02d%02d_%02d%02d%02d", 
					cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH),
					cal.get(Calendar.HOUR_OF_DAY),cal.get(Calendar.MINUTE),cal.get(Calendar.SECOND));
			
			logMessage(TAG, mContext.getResources().getString(R.string.msg_attempt_file_upload)+" - "+backupName, true);
			 
			if (uploadFile(syncDbPath, backupName, "application/x-sqlite3") == null) {
                logMessageCancelable(TAG, mContext.getResources().getString(R.string.msg_error_upload_file)+" - "+backupName, true);
				return false;
			}	
			logMessage(TAG, mContext.getResources().getString(R.string.msg_uploaded_file)+" - "+backupName, true);
			
			cleanupBackups(Integer.parseInt(BudgetEnvelopes.getCurrentSettingValue(SettingsDataObject.UUID_BACKUP_COUNT, "5")));			
			
			DBMain.getInstance().getWritableDatabase().execSQL("vacuum"); //cleanup possible journal data
			DBMain.getInstance().getWritableDatabase().close();
					
			logMessage(TAG, mContext.getResources().getString(R.string.msg_attempt_file_cleanup), true);
			
			File f = new File(mainDbPath);
			if (f.exists())
				f.delete();
			f=new File(mainDbPath+"-journal");
			if (f.exists())
				f.delete();
			

			File newFile = new File(mainDbPath);
			
			f = new File(syncDbPath);
				f.renameTo(newFile);

			logMessage(TAG, mContext.getResources().getString(R.string.msg_copied_file)+" - "+syncDbName+" -> "+DBMain.DATABASE_NAME, true);
				
			f=new File(syncDbPath+"-journal");
			if (f.exists())
				f.delete();
			
			logMessage(TAG, mContext.getResources().getString(R.string.msg_attempt_db_cleanup)+" - "+DBMain.DATABASE_NAME, true);
			
			DBMain.getInstance().getWritableDatabase().beginTransaction();
			DBMain.getInstance().getWritableDatabase().execSQL("update "+EnvelopeDataObject.TABLENAME+" set "+EnvelopeDataObject.FIELDNAME_EXPENSES+"=null, "+EnvelopeDataObject.FIELDNAME_EXPENSES+"=null");
			DBMain.getInstance().getWritableDatabase().execSQL("update "+EnvelopeDataObject.TABLENAME+" set "+EnvelopeDataObject.FIELDNAME_CHANGED+"=null");
			DBMain.getInstance().getWritableDatabase().setTransactionSuccessful();
			DBMain.getInstance().getWritableDatabase().endTransaction();

            updateSyncToken();

            logMessageCancelable(TAG, mContext.getResources().getString(R.string.msg_sync_success), true);
            logMessageCancelable(TAG, mContext.getResources().getString(R.string.msg_ready_go_back), false);
			return true;
		} catch (Exception e) {
            logMessageCancelable(TAG, mContext.getResources().getString(R.string.msg_error_general), true);
            logMessageCancelable(TAG, e.getMessage(), true);
			return false;
		}
	}

}
