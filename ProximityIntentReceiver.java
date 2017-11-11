package cg.paridel.mazone.api;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import cg.paridel.mazone.R;
import cg.paridel.mazone.fragments.Maps;

import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by Paridel MAKOUALA on 11/10/2017.
 */

public class ProximityIntentReceiver extends BroadcastReceiver {

    private static final int NOTIFICATION_ID = 1000;
    public static final String EVENT_ID_INTENT_EXTRA = "EventIDIntentExtraKey";

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent) {

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        long eventID = intent.getLongExtra(EVENT_ID_INTENT_EXTRA, -1);
        Log.v("MaZone", "Proximity Alert Intent Received, eventID = "+eventID);

        String key = LocationManager.KEY_PROXIMITY_ENTERING;

        Boolean entering = intent.getBooleanExtra(key, false);
        if (entering) {
            Log.e(getClass().getSimpleName(), "Entrée dans la zone");
        }
        else {
            Log.e(getClass().getSimpleName(), "Sortie dans la zone");
        }

        Intent notificationIntent = new Intent(context, Maps.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("MaZone")
                .setContentText("Vous êtes près de MaZone !")
                .setContentIntent(pendingIntent)
                .setStyle(new NotificationCompat.BigTextStyle().bigText("Vous êtes près de MaZone !"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setShowWhen(true)
                .setAutoCancel(true)
                .setLights(Color.WHITE, 1500, 1500)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .build();

        assert notificationManager != null;
        notificationManager.notify(NOTIFICATION_ID, notification);

    }

}
