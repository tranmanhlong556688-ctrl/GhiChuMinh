package com.minh.ghichuminh;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            for (MainActivity.TaskItem task : MainActivity.readTasks(context)) {
                if (!task.done) {
                    MainActivity.scheduleReminder(context, task);
                }
            }
        }
    }
}
