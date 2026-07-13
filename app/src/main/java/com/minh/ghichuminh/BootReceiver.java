package com.minh.ghichuminh;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;

        ArrayList<MainActivity.TaskItem> tasks = MainActivity.readTasks(context);
        for (MainActivity.TaskItem task : tasks) {
            if (!task.done) {
                MainActivity.scheduleReminder(context, task);
            }
        }
        MainActivity.writeTasks(context, tasks);
    }
}
