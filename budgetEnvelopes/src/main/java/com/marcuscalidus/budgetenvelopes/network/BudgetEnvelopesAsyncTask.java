package com.marcuscalidus.budgetenvelopes.network;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Contents;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi.ContentsResult;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder.DriveFileResult;
import com.google.android.gms.drive.DriveFolder.DriveFolderResult;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.DriveResource.MetadataResult;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.plus.Plus;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.gson.GsonFactory;
import com.marcuscalidus.budgetenvelopes.R;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.Arrays;

public class BudgetEnvelopesAsyncTask extends ApiClientAsyncTask<Void, String, Boolean> {

	protected static final String FOLDER_NAME = "BudgetEnvelopes";
	private static final String TAG = "BudgetEnvelopesAsyncTask";
	protected static final int BUFFER_SIZE = 1024 * 2;
	
	private com.google.api.services.drive.Drive mGoogleDriveService;
	private com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential mGoogleAccountCredentials;

	
	protected Context mContext;
	
	public interface OnNotificationListener {
		public void notifyPreExecute(Context context);
		public void notifyPostExecute(Context context, Boolean result);
	}
	
	public interface OnLogMessageListener {
		public void logMessage(String message);
	}
	
	private OnNotificationListener mOnExecuteListener;
	private OnLogMessageListener mOnLogMessageListener;
	
	protected void logMessage(String tag, String message) {
		publishProgress(message);
		Log.d(tag, message);
		
		return;
	}
	
	public BudgetEnvelopesAsyncTask(Context context,
			GoogleApiClient googleApiClient) {
		super(context, googleApiClient);
		mContext = context;
		
		// build RESTFul (DriveSDKv2) service to fall back to for DELETE
	    mGoogleAccountCredentials =
	    GoogleAccountCredential
	      .usingOAuth2(mContext, Arrays.asList(com.google.api.services.drive.DriveScopes.DRIVE_FILE));
		mGoogleAccountCredentials.setSelectedAccountName(Plus.AccountApi.getAccountName(googleApiClient));
	    mGoogleDriveService = new com.google.api.services.drive.Drive.Builder(
	            AndroidHttp.newCompatibleTransport(), new GsonFactory(), mGoogleAccountCredentials).build();
	}
	
	/*public void trash(DriveId dId) {
		  try {
		    String fileID =  dId.getResourceId();
		      if (fileID != null)
		    	  mGoogleDriveService.files().trash(fileID).execute();
		  } catch (Exception e) {} 
		}
		*/

	public void delete(DriveId dId) {
		  try {
		    String fileID = dId.getResourceId();
		      if (fileID != null)
		    	  mGoogleDriveService.files().delete(fileID).execute();
		  } catch (Exception e) {} 
		}
	
	protected Boolean deleteContents(final DriveFile driveFile) {
		delete(driveFile.getDriveId());
		/*
		ContentsResult contentsResult = driveFile.openContents(getGoogleApiClient(), DriveFile.MODE_WRITE_ONLY, null).await();
		
		if (!contentsResult.getStatus().isSuccess()) {
			// oh noes!
			return false;
		}
		
		Contents contents = contentsResult.getContents();
		try {
			MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder().setTitle("deleted").build();
			com.google.android.gms.common.api.Status status = driveFile.commitAndCloseContents(getGoogleApiClient(), contentsResult.getContents(), metadataChangeSet).await();
			
			if (!status.isSuccess()) {
				// more oh noes!
				return false;
		    }
		         
			// nicely deleted
		}
		catch (Exception e) {
			driveFile.discardContents(getGoogleApiClient(), contents);	
			return false;
		}	
		*/
		return true;
}      
	
    public DriveId getFolderDriveID() {			
		MetadataBuffer mbr = 
		Drive.DriveApi
		    .getRootFolder(getGoogleApiClient())
		      .queryChildren(getGoogleApiClient(), 
		    		  		new Query.Builder()
	                                .addFilter(Filters.eq(SearchableField.TITLE, FOLDER_NAME))
	                                .addFilter(Filters.eq(SearchableField.MIME_TYPE, "application/vnd.google-apps.folder"))
	                                .build()).await().getMetadataBuffer();
		
		for (int i = 0; i < mbr.getCount(); i++) {
			Metadata md = mbr.get(i);
			 
		    if (md.isEditable() && md.isFolder() && !md.isTrashed()) {
		        //    logMessage(TAG, mContext.getResources().getString(R.string.msg_found_remote_folder) +" - "+ md.getTitle());
					return md.getDriveId();
			}
		}

	    mbr.close();
		
		MetadataChangeSet changeSet = new MetadataChangeSet.Builder().setTitle(FOLDER_NAME).build();
		DriveFolderResult folderResult = Drive.DriveApi.getRootFolder(getGoogleApiClient())
	                   .createFolder(getGoogleApiClient(), changeSet).await();
			
		if (!folderResult.getStatus().isSuccess()) {
				logMessage(TAG, mContext.getResources().getString(R.string.msg_error_no_create_remote_folder));
				return null;
		}
		else {
		//	logMessage(TAG, mContext.getResources().getString(R.string.msg_new_remote_folder)+" - "+FOLDER_NAME.toString());
			return folderResult.getDriveFolder().getDriveId();
		}
	     
	}    
   
	
    protected boolean syncDrive() {
    	com.google.android.gms.common.api.Status res = Drive.DriveApi.requestSync(getGoogleApiClient()).await();
    	if (!res.isSuccess()) { 
    		logMessage(TAG, mContext.getResources().getString(R.string.msg_error_request_sync_gdrive));
    		logMessage(TAG, res.getStatus().toString());
    		return false;
    	}
    	return true;
    }

    @Override
    protected void onProgressUpdate(String... progress) {
    	if (getOnLogMessageListener() != null)
			getOnLogMessageListener().logMessage(progress[0]);
    }

    @Override
    protected void onPreExecute() {
    	super.onPreExecute();
    	if (getOnExecuteListener() != null) {
    		getOnExecuteListener().notifyPreExecute(mContext);    	
    	}	
    }
    
 	@Override
    protected void onPostExecute(Boolean result) {
        getGoogleApiClient().disconnect();
        super.onPostExecute(result);
    	if (getOnExecuteListener() != null) {
    		getOnExecuteListener().notifyPostExecute(mContext, result);
    	}
    }
    
	@Override
	protected Boolean doInBackgroundConnected(Void... params) {
		return false;
	}

	public OnNotificationListener getOnExecuteListener() {
		return mOnExecuteListener;
	}

	public void setOnExecuteListener(OnNotificationListener mOnExecuteListener) {
		this.mOnExecuteListener = mOnExecuteListener;
	}

	public OnLogMessageListener getOnLogMessageListener() {
		return mOnLogMessageListener;
	}

	public void setOnLogMessageListener(OnLogMessageListener mOnLogMessageListener) {
		this.mOnLogMessageListener = mOnLogMessageListener;
	}

	private DriveId getRemoteFileDriveID(String fileName, String mimeType) {			
		MetadataBuffer mbr = 
		Drive.DriveApi
		.getFolder(getGoogleApiClient(), getFolderDriveID())
				.queryChildren(getGoogleApiClient(), new Query.Builder()
															.addFilter(Filters.eq(SearchableField.TITLE, fileName))
															//.addFilter(Filters.eq(SearchableField.MIME_TYPE, mimeType))
															.build()).await().getMetadataBuffer();
		for (int i = 0; i < mbr.getCount(); i++) {
			Metadata md = mbr.get(i);
			 
		    if (md.isEditable() && !md.isFolder() && !md.isTrashed()) {
		        //    logMessage(TAG, mContext.getResources().getString(R.string.msg_file_exists) +" - " + md.getTitle().toString());
					return md.getDriveId();
			}
		}
		mbr.close();
		
		//we did not find the file ... return nothing
		//logMessage(TAG, mContext.getResources().getString(R.string.msg_file_not_exists)+" - "+mRemoteFileName);
		return null;	     
	}

	protected DriveFile getRemoteFile(String fileName, String mimeType, boolean createFile) {
		DriveId driveId = getRemoteFileDriveID(fileName, mimeType);
		
		if (driveId == null) {
			if (createFile) {
				MetadataChangeSet fileMetadata = new MetadataChangeSet.Builder()
	                .setTitle(fileName)
	              //  .setMimeType(mimeType)
	                .build();
				
				ContentsResult contentsResult =
	                    Drive.DriveApi.newContents(getGoogleApiClient()).await();
	            if (!contentsResult.getStatus().isSuccess()) {
	                // We failed, stop the task and return.
	                return null;
	            }           
				
				DriveFileResult fileResult = Drive.DriveApi
						.getFolder(getGoogleApiClient(), getFolderDriveID())
		        	.createFile(getGoogleApiClient(), fileMetadata, contentsResult.getContents()).await();
				
				return fileResult.getDriveFile();
			} else {
	            // We failed, stop the task and return.
				return null;
			}			
		} else {
			return Drive.DriveApi.getFile(getGoogleApiClient(), driveId);
		}
	}

	protected Metadata uploadFile(String localFileName, String remoteFileName, String mimeType) throws Exception {           
	    DriveFile remoteFile = getRemoteFile(remoteFileName, mimeType, true);
	    
	    if (remoteFile == null) {
	    	logMessage(TAG, mContext.getResources().getString(R.string.msg_error_create_remote_file));
	    	return null;
	    }
	    
	    ContentsResult contentsResult =
	    remoteFile.openContents(getGoogleApiClient(), DriveFile.MODE_WRITE_ONLY, null).await();
	    if (contentsResult.getStatus().isSuccess()) {
	        Contents contents = contentsResult.getContents();
	        
	        try {
		        OutputStream oStream = contents.getOutputStream();
		        BufferedInputStream fStream = new BufferedInputStream(new FileInputStream(localFileName), BUFFER_SIZE);
		        
		        int n = 0;
		        byte[] buffer = new byte[BUFFER_SIZE];
		        
		        while ((n = fStream.read(buffer, 0, BUFFER_SIZE)) != -1) {
		        	oStream.write(buffer, 0, n);
		        }
		        fStream.close();
		        oStream.close();
		   
		        remoteFile.commitAndCloseContents(getGoogleApiClient(), contents).await();
	        }
	        catch (Exception e) {
	        	remoteFile.discardContents(getGoogleApiClient(), contents).await();
	        	logMessage(TAG, mContext.getResources().getString(R.string.msg_error_contents_handling));
	        	throw e;
	        }	        
	        
	    } else {
	    	logMessage(TAG, mContext.getResources().getString(R.string.msg_error_contents_handling) + " - error write access");
	    	throw new Exception(contentsResult.getStatus().toString());
	    }
	    	
	    MetadataResult metadataResult = getRemoteFile(remoteFileName, mimeType, true)
	            .getMetadata(getGoogleApiClient())
	            .await();
	    if (!metadataResult.getStatus().isSuccess()) {
	        return null;
	    }   	  		
		
	    // We succeeded, return the newly created metadata.
	    return metadataResult.getMetadata();
	}
	
	public void copyFile(String src, String dst) throws IOException {
	    FileInputStream inStream = new FileInputStream(src);
	    FileOutputStream outStream = new FileOutputStream(dst);
	    FileChannel inChannel = inStream.getChannel();
	    FileChannel outChannel = outStream.getChannel();
	    inChannel.transferTo(0, inChannel.size(), outChannel);
	    inStream.close();
	    outStream.close();
	}

	protected String downloadFile(String localFileName, String remoteFileName, String mimeType) throws Exception {        
	    DriveFile remoteFile = getRemoteFile(remoteFileName, mimeType, false);
	            
	    if (remoteFile == null) {
	    	logMessage(TAG, mContext.getResources().getString(R.string.msg_no_file_download));
	    	return null;
	    }
	       
	    ContentsResult contentsResult =
	    remoteFile.openContents(getGoogleApiClient(), DriveFile.MODE_READ_ONLY, null).await();
	    if (contentsResult.getStatus().isSuccess()) {
	        Contents contents = contentsResult.getContents();
	        
	        try {
	        	BufferedInputStream iStream = new BufferedInputStream(contents.getInputStream());
		        BufferedOutputStream fStream = new BufferedOutputStream(new FileOutputStream(localFileName, false), BUFFER_SIZE);
		        
		        int n = 0;
		        byte[] buffer = new byte[BUFFER_SIZE];
		        
		        while ((n = iStream.read(buffer, 0, BUFFER_SIZE)) != -1) {
		        	fStream.write(buffer, 0, n);
		        }
		        fStream.close();
		        iStream.close();
		   
		        remoteFile.discardContents(getGoogleApiClient(), contents).await();
	        }
	        catch (Exception e) {
	        	remoteFile.discardContents(getGoogleApiClient(), contents).await();
	        	logMessage(TAG, mContext.getResources().getString(R.string.msg_error_contents_handling));
	        	throw e;
	        }	        
	        
	    } else {
	    	logMessage(TAG, mContext.getResources().getString(R.string.msg_error_contents_handling) + " - open remote");
	    	throw new Exception(contentsResult.getStatus().toString());
	    }	  		
		
	    return localFileName;
	}

}
