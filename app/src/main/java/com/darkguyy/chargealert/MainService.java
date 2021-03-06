package com.darkguyy.chargealert;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.BatteryManager;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 */
public class MainService extends Service {
    private static final String TAG = MainService.class.getName();
    private int mNotificationId = 0;
    private boolean mAlreadyNotified = false;
    private NotificationManager mNotificationManager;
    PendingIntent pendingIntent;
    AlarmManager alarmManager;
    IntentFilter ifilter;
    Intent batteryStatus;
    static MediaPlayer mediaPlayer=null;
    Vibrator vibrator;
//    private int mDebugNotificationId = 1;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "creating service");

        ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        batteryStatus = this.registerReceiver(null, ifilter);

        Intent intent = new Intent(this, BootCompletedReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(
                this.getApplicationContext(), 234324243, intent, 0);
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        vibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);

        mediaPlayer = new MediaPlayer();

        //vibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);

        mNotificationManager = ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE));
        registerReceiver(mReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    public void onDestroy() {
        Log.d(TAG, "destroying service");
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    public static void startIfEnabled(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(MainActivity.PREFERENCES_NAME, 0);
        boolean isEnabled = preferences.getBoolean(MainActivity.PREFERENCE_KEY_ENABLED, false);
        Intent intent = new Intent(context, MainService.class);
        if (isEnabled) {
            context.startService(intent);
        } else {
            context.stopService(intent);
        }
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                final String action = intent.getAction();

                if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                    int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, Integer.MIN_VALUE);

                    int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

                    float batteryPct = level / (float)scale;

                    alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()
                                + (1000), pendingIntent);

                    //Toast.makeText(context,(int)batteryPct + " Battery level",Toast.LENGTH_LONG).show();
//                    SharedPreferences preferences = context.getSharedPreferences(MainActivity.PREFERENCES_NAME, 0);
//                    if((int)(batteryPct*100) == preferences.getInt(MainActivity.PREFERENCE_KEY_ALERT_LEVEL,88)){
//                        Toast.makeText(context,(int)(batteryPct*100) + " Battery level", Toast.LENGTH_LONG).show();
//                        vibrator.vibrate(3000);
//                    }

                    vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

                    mediaPlayer = new MediaPlayer();

                    SharedPreferences preferences = context.getSharedPreferences(MainActivity.PREFERENCES_NAME, 0);
                    if(((int)(batteryPct*100) == preferences.getInt(MainActivity.PREFERENCE_KEY_ALERT_LEVEL,0) || (int)(batteryPct*100) == 100) && MainActivity.isConnected(context)){

                        //mediaPlayer.start();
                        if(mediaPlayer != null && !mediaPlayer.isPlaying())
                        {
                            Toast.makeText(context,"Battery reached the level " + (int)(batteryPct*100) + "% \nRemove from charging !!!",Toast.LENGTH_LONG).show();
                            mediaPlayer = MediaPlayer.create(context, R.raw.ringtone);
                            mediaPlayer.start();
                            vibrator.vibrate(3000);
                        }

                    }


                    if (status == BatteryManager.BATTERY_STATUS_FULL) {
                        if (!mAlreadyNotified) {
                            mAlreadyNotified = true;
                            //SharedPreferences preferences = context.getSharedPreferences(MainActivity.PREFERENCES_NAME, 0);
                            int defaults = Notification.DEFAULT_LIGHTS;
                            if (preferences.getBoolean(MainActivity.PREFERENCE_KEY_VIBRATE, false)) {
                                defaults |= Notification.DEFAULT_VIBRATE;
                            }
                            if (preferences.getBoolean(MainActivity.PREFERENCE_KEY_SOUND, false)) {
                                defaults |= Notification.DEFAULT_SOUND;
                            }

                            Notification notification = new NotificationCompat.Builder(context)
                                    .setSmallIcon(R.drawable.ic_action_battery)
                                    .setContentTitle(getString(R.string.app_name))
                                    .setContentText(getString(R.string.full))
                                    .setOnlyAlertOnce(true)
                                    .setContentIntent(PendingIntent.getActivity(context, 0, new Intent(Intent.ACTION_POWER_USAGE_SUMMARY), 0))
                                    .setDefaults(defaults)
                                    .build();
                            mNotificationManager.notify(mNotificationId, notification);

//                            AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
//                            int alarmType = AlarmManager.ELAPSED_REALTIME_WAKEUP;
//                            long timeOrLengthofWait = 10000;
//                            Intent intentToFire = new Intent(getApplicationContext(), BootCompletedReceiver.class);
//                            PendingIntent alarmIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intentToFire, 0);
//                            alarmManager.set(alarmType, timeOrLengthofWait, alarmIntent);
                        }
                    } else {
                        mAlreadyNotified = false;
                        mNotificationManager.cancel(mNotificationId);
                    }

                    Log.d(TAG, String.format("battery status: %d", status));
//                    Notification notification = new NotificationCompat.Builder(context)
//                            .setSmallIcon(R.drawable.ic_launcher)
//                            .setContentTitle(getString(R.string.app_name))
//                            .setContentText(String.format("battery status: %d", status))
//                            .build();
//                    mNotificationManager.notify(mDebugNotificationId++, notification);
                }
            }
        }
    };



}
