package com.marcuscalidus.budgetenvelopes.network;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.plus.Plus;
import com.marcuscalidus.budgetenvelopes.BudgetEnvelopes;
import com.marcuscalidus.budgetenvelopes.MainActivity;
import com.marcuscalidus.budgetenvelopes.R;
import com.marcuscalidus.budgetenvelopes.dataobjects.SettingsDataObject;
import com.marcuscalidus.budgetenvelopes.db.DBMain;

public class BudgetEnvelopesSyncService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    int mId;
    private DatabaseSyncAsyncTask mDbSyncTask;
    private GoogleApiClient mGoogleApiClient;

    private static final int RESOLVE_CONNECTION_REQUEST_CODE = 1;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        SQLiteDatabase db = DBMain.getInstance().getReadableDatabase();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Drive.API)
                .addApi(Plus.API)
                .addScope(Drive.SCOPE_FILE)
                .setAccountName(SettingsDataObject.getSetting(this, db, SettingsDataObject.UUID_SYNC_ACCOUNT).getValue())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).build();

        mGoogleApiClient.connect();

        // I don't want this service to stay in memory, so I stop it
        // immediately after doing what I wanted it to do.
        return Service.START_NOT_STICKY;
    }

   // @Override
   // public void onDestroy() {
        // I want to restart this service again in one hour
   //     AlarmManager alarm = (AlarmManager)getSystemService(ALARM_SERVICE);
   //     alarm.set(
   //             alarm.RTC_WAKEUP,
   //             System.currentTimeMillis() + (1000 * 60 * 60),
   //             PendingIntent.getService(this, 0, new Intent(this, BudgetEnvelopesSyncService.class), 0)
   //     );
   // }

    @Override
    public void onConnected(Bundle bundle) {
        DatabaseSyncAsyncTask dbSyncTask = BudgetEnvelopes.getInstance().getSyncTask(mGoogleApiClient);
        //dbSyncTask.setOnLogMessageListener(this);
        dbSyncTask.setOnExecuteListener(new BudgetEnvelopesAsyncTask.OnNotificationListener() {

            @Override
            public void notifyPreExecute(Context context) {
                // TODO Auto-generated method stub
            }

            @Override
            public void notifyPostExecute(Context context, Boolean result) {
                mGoogleApiClient.disconnect();
                stopSelf();
            }
        });
        dbSyncTask.execute();
    }

    @Override
    public void onConnectionSuspended(int i) {
   //     this.stopSelf("connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        stopSelf();
    }
}