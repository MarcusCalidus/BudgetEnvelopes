package com.marcuscalidus.budgetenvelopes.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AutoStart extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, BudgetEnvelopesSyncService.class));
    }
}
