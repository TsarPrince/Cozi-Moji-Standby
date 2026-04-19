package com.example.cozymojistandby

import android.app.*
import android.content.*
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

class ChargingReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "ChargingReceiver"
        private const val CHANNEL_ID = "standby_channel"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_EXIT = "com.example.cozymojistandby.EXIT"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "onReceive: action received = $action")
        
        // Detailed battery status log
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }
        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                 status == BatteryManager.BATTERY_STATUS_FULL
        
        val chargePlug: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val usbCharge: Boolean = chargePlug == BatteryManager.BATTERY_PLUGGED_USB
        val acCharge: Boolean = chargePlug == BatteryManager.BATTERY_PLUGGED_AC
        
        Log.d(TAG, "Battery status: isCharging=$isCharging, plugType=$chargePlug (USB=$usbCharge, AC=$acCharge)")

        when (action) {
            Intent.ACTION_POWER_CONNECTED -> {
                Log.d(TAG, "Power connected! Attempting to launch standby...")
                handlePowerConnected(context)
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                Log.d(TAG, "Power disconnected! Sending exit signal...")
                val exitIntent = Intent(ACTION_EXIT)
                context.sendBroadcast(exitIntent)
            }
            else -> {
                Log.d(TAG, "Received unhandled action: $action")
            }
        }
    }

    private fun handlePowerConnected(context: Context) {
        val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or 
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION or 
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            fullScreenIntent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        createNotificationChannel(context)

        Log.d(TAG, "Creating high-priority notification with FullScreenIntent")
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Charging Started")
            .setContentText("Tap to open Cozy Moji")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, builder.build())
        
        try {
            Log.d(TAG, "Attempting direct startActivity...")
            context.startActivity(fullScreenIntent)
            Log.d(TAG, "startActivity call completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Direct start failed: ${e.message}. App likely backgrounded without DrawOverlays permission.")
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Standby Activation"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = "Shows the clock when charging"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
