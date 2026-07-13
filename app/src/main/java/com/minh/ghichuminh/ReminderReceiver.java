package com.minh.ghichuminh;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.ArrayList;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "ghi_chu_minh_reminders_v2";

    @Override
    public void onReceive(Context context, Intent intent) {
        int taskId = intent.getIntExtra(MainActivity.EXTRA_TASK_ID, -1);
        if (taskId < 0) return;

        ArrayList<MainActivity.TaskItem> tasks = MainActivity.readTasks(context);
        MainActivity.TaskItem found = null;
        for (MainActivity.TaskItem task : tasks) {
            if (task.id == taskId) {
                found = task;
                break;
            }
        }
        if (found == null || found.done) return;

        showNotification(context, found);

        if (found.recurrence != MainActivity.REPEAT_NONE) {
            found.dueTimeMillis = MainActivity.nextOccurrenceAfter(
                    found.dueTimeMillis,
                    found.recurrence,
                    System.currentTimeMillis());
            MainActivity.writeTasks(context, tasks);
            MainActivity.scheduleReminder(context, found);
        }
    }

    private void showNotification(Context context, MainActivity.TaskItem task) {
        if (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Nhắc việc Ghi Chú Minh",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Thông báo công việc và ghi chú đến hạn");
            channel.enableVibration(true);
            manager.createNotificationChannel(channel);
        }

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                task.id,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(context, CHANNEL_ID)
                : new Notification.Builder(context);

        String detail = task.detail == null || task.detail.trim().isEmpty()
                ? task.category + " • " + MainActivity.repeatName(task.recurrence)
                : task.detail;

        builder.setSmallIcon(R.drawable.ic_ml_dragon)
                .setContentTitle("Đến giờ: " + task.title)
                .setContentText(detail)
                .setStyle(new Notification.BigTextStyle().bigText(detail))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL);

        manager.notify(task.id, builder.build());
    }
}
