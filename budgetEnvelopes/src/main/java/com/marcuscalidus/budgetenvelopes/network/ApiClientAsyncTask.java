package com.marcuscalidus.budgetenvelopes.network;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;

import java.util.concurrent.CountDownLatch;

public abstract class ApiClientAsyncTask<Params, Progress, Result> 
		extends AsyncTask<Params, Progress, Result> {

	private GoogleApiClient mClient;
	
	private static final String TAG = "ApiClientAsyncTask";
	
	public ApiClientAsyncTask(Context context, GoogleApiClient googleApiClient) {
		mClient = googleApiClient;
	}
		
	protected final Result doInBackground(Params... params) {
        Log.d(TAG, "in background");
        
        final CountDownLatch latch = new CountDownLatch(1);
        
        mClient.registerConnectionCallbacks(new ConnectionCallbacks() {
            @Override
            public void onConnectionSuspended(int cause) {
            }

            @Override
            public void onConnected(Bundle arg0) {
                latch.countDown();
            }
        });

        mClient.registerConnectionFailedListener(new OnConnectionFailedListener() {
            @Override
            public void onConnectionFailed(ConnectionResult arg0) {
                latch.countDown();
            }
        });
        mClient.connect();
        try {
            latch.await();
        } catch (InterruptedException e) {
            return null;
        }
        if (!mClient.isConnected()) {
            return null;
        }
        try {
            return doInBackgroundConnected(params);
        } finally {
          //  mClient.disconnect();
        }
    }

    /**
     * Override this method to perform a computation on a background thread, while the client is
     * connected.
     */
    protected abstract Result doInBackgroundConnected(Params... params);

    /**
     * Gets the GoogleApliClient owned by this async task.
     */
    protected GoogleApiClient getGoogleApiClient() {
        return mClient;
    }
}