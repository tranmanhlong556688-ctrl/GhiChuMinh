package com.minh.ghichuminh;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    static final String PREF_NAME = "GhiChuMinhData";
    static final String KEY_TASKS = "tasks";
    static final String EXTRA_TASK_ID = "task_id";

    private final ArrayList<TaskItem> tasks = new ArrayList<>();
    private SharedPreferences preferences;
    private LinearLayout listLayout;
    private EditText inputTitle;
    private EditText inputDetail;
    private CheckBox repeatDailyBox;
    private Button pickTimeButton;
    private TextView summaryText;
    private final Calendar selectedCalendar = Calendar.getInstance();
    private final SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        selectedCalendar.add(Calendar.HOUR_OF_DAY, 1);
        selectedCalendar.set(Calendar.SECOND, 0);
        selectedCalendar.set(Calendar.MILLISECOND, 0);
        requestNotificationPermissionIfNeeded();
        loadTasks();
        buildScreen();
        renderTasks();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
        }
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 28, 28, 28);
        root.setBackgroundColor(0xFFF5F7FA);

        TextView title = new TextView(this);
        title.setText("Minh Ghi Chú Công Việc");
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(0xFF0D47A1);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Ghi chú, đặt giờ nhắc việc, theo dõi công việc hằng ngày.");
        subtitle.setTextSize(14);
        subtitle.setTextColor(0xFF455A64);
        subtitle.setPadding(0, 8, 0, 18);
        root.addView(subtitle);

        inputTitle = new EditText(this);
        inputTitle.setHint("Tên công việc / ghi chú");
        inputTitle.setSingleLine(false);
        inputTitle.setMaxLines(2);
        inputTitle.setTextSize(16);
        root.addView(inputTitle, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        inputDetail = new EditText(this);
        inputDetail.setHint("Nội dung chi tiết");
        inputDetail.setMinLines(2);
        inputDetail.setGravity(Gravity.TOP);
        inputDetail.setTextSize(15);
        root.addView(inputDetail, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        repeatDailyBox = new CheckBox(this);
        repeatDailyBox.setText("Nhắc lại hằng ngày");
        root.addView(repeatDailyBox);

        pickTimeButton = new Button(this);
        pickTimeButton.setText(timeButtonText(selectedCalendar.getTimeInMillis()));
        pickTimeButton.setOnClickListener(v -> pickDateTime(selectedCalendar, pickTimeButton));
        root.addView(pickTimeButton);

        Button saveButton = new Button(this);
        saveButton.setText("Lưu công việc và đặt nhắc việc");
        saveButton.setOnClickListener(v -> saveNewTask());
        root.addView(saveButton);

        summaryText = new TextView(this);
        summaryText.setTextSize(15);
        summaryText.setTypeface(Typeface.DEFAULT_BOLD);
        summaryText.setTextColor(0xFF263238);
        summaryText.setPadding(0, 18, 0, 8);
        root.addView(summaryText);

        ScrollView scrollView = new ScrollView(this);
        listLayout = new LinearLayout(this);
        listLayout.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(listLayout);
        root.addView(scrollView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        setContentView(root);
    }

    private String timeButtonText(long millis) {
        return "Thời gian nhắc: " + displayFormat.format(millis);
    }

    private void pickDateTime(Calendar calendar, Button targetButton) {
        DatePickerDialog dateDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    TimePickerDialog timeDialog = new TimePickerDialog(this,
                            (timeView, hourOfDay, minute) -> {
                                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                calendar.set(Calendar.MINUTE, minute);
                                calendar.set(Calendar.SECOND, 0);
                                calendar.set(Calendar.MILLISECOND, 0);
                                targetButton.setText(timeButtonText(calendar.getTimeInMillis()));
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            true);
                    timeDialog.show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dateDialog.show();
    }

    private void saveNewTask() {
        String title = inputTitle.getText().toString().trim();
        String detail = inputDetail.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Bạn chưa nhập tên công việc", Toast.LENGTH_SHORT).show();
            return;
        }

        TaskItem task = new TaskItem();
        task.id = (int) (System.currentTimeMillis() & 0x7fffffff);
        task.title = title;
        task.detail = detail;
        task.dueTimeMillis = selectedCalendar.getTimeInMillis();
        task.repeatDaily = repeatDailyBox.isChecked();
        task.done = false;
        tasks.add(0, task);
        saveTasks();
        scheduleReminder(this, task);

        inputTitle.setText("");
        inputDetail.setText("");
        repeatDailyBox.setChecked(false);
        selectedCalendar.setTimeInMillis(System.currentTimeMillis());
        selectedCalendar.add(Calendar.HOUR_OF_DAY, 1);
        selectedCalendar.set(Calendar.SECOND, 0);
        selectedCalendar.set(Calendar.MILLISECOND, 0);
        pickTimeButton.setText(timeButtonText(selectedCalendar.getTimeInMillis()));
        renderTasks();
        Toast.makeText(this, "Đã lưu và đặt nhắc việc", Toast.LENGTH_SHORT).show();
    }

    private void renderTasks() {
        listLayout.removeAllViews();
        int activeCount = 0;
        for (TaskItem task : tasks) {
            if (!task.done) activeCount++;
        }
        summaryText.setText("Danh sách công việc: " + activeCount + " chưa xong / " + tasks.size() + " tổng số");

        if (tasks.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Chưa có công việc nào. Hãy thêm việc cần nhắc ở phía trên.");
            empty.setTextSize(16);
            empty.setTextColor(0xFF78909C);
            empty.setPadding(0, 24, 0, 0);
            listLayout.addView(empty);
            return;
        }

        for (TaskItem task : tasks) {
            listLayout.addView(createTaskView(task));
        }
    }

    private View createTaskView(TaskItem task) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(20, 18, 20, 18);
        card.setBackgroundColor(0xFFFFFFFF);

        TextView title = new TextView(this);
        title.setText((task.done ? "✓ " : "") + task.title);
        title.setTextSize(17);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(0xFF263238);
        card.addView(title);

        TextView detail = new TextView(this);
        detail.setText(task.detail.isEmpty() ? "Không có ghi chú chi tiết" : task.detail);
        detail.setTextSize(14);
        detail.setTextColor(0xFF455A64);
        detail.setPadding(0, 5, 0, 0);
        card.addView(detail);

        TextView time = new TextView(this);
        time.setText((task.repeatDaily ? "Nhắc hằng ngày: " : "Nhắc lúc: ") + displayFormat.format(task.dueTimeMillis));
        time.setTextSize(13);
        time.setTextColor(0xFF1565C0);
        time.setPadding(0, 6, 0, 0);
        card.addView(time);

        if (task.done) {
            title.setPaintFlags(title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            title.setAlpha(0.55f);
            detail.setAlpha(0.55f);
            time.setAlpha(0.55f);
        }

        card.setOnClickListener(v -> toggleDone(task));
        card.setOnLongClickListener(v -> {
            showTaskActions(task);
            return true;
        });

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 14);
        card.setLayoutParams(params);
        return card;
    }

    private void toggleDone(TaskItem task) {
        task.done = !task.done;
        if (task.done) {
            cancelReminder(this, task.id);
            Toast.makeText(this, "Đã đánh dấu hoàn thành", Toast.LENGTH_SHORT).show();
        } else {
            scheduleReminder(this, task);
            Toast.makeText(this, "Đã mở lại công việc", Toast.LENGTH_SHORT).show();
        }
        saveTasks();
        renderTasks();
    }

    private void showTaskActions(TaskItem task) {
        String[] actions = new String[]{"Sửa", "Xóa"};
        new AlertDialog.Builder(this)
                .setTitle(task.title)
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) showEditDialog(task);
                    if (which == 1) confirmDelete(task);
                })
                .show();
    }

    private void showEditDialog(TaskItem task) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(20, 8, 20, 0);

        EditText titleEdit = new EditText(this);
        titleEdit.setHint("Tên công việc");
        titleEdit.setText(task.title);
        form.addView(titleEdit);

        EditText detailEdit = new EditText(this);
        detailEdit.setHint("Nội dung chi tiết");
        detailEdit.setMinLines(2);
        detailEdit.setGravity(Gravity.TOP);
        detailEdit.setText(task.detail);
        form.addView(detailEdit);

        CheckBox repeatBox = new CheckBox(this);
        repeatBox.setText("Nhắc lại hằng ngày");
        repeatBox.setChecked(task.repeatDaily);
        form.addView(repeatBox);

        Calendar editCalendar = Calendar.getInstance();
        editCalendar.setTimeInMillis(task.dueTimeMillis);
        Button editTimeButton = new Button(this);
        editTimeButton.setText(timeButtonText(editCalendar.getTimeInMillis()));
        editTimeButton.setOnClickListener(v -> pickDateTime(editCalendar, editTimeButton));
        form.addView(editTimeButton);

        new AlertDialog.Builder(this)
                .setTitle("Sửa công việc")
                .setView(form)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String newTitle = titleEdit.getText().toString().trim();
                    if (newTitle.isEmpty()) {
                        Toast.makeText(this, "Tên công việc không được trống", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    task.title = newTitle;
                    task.detail = detailEdit.getText().toString().trim();
                    task.dueTimeMillis = editCalendar.getTimeInMillis();
                    task.repeatDaily = repeatBox.isChecked();
                    task.done = false;
                    saveTasks();
                    cancelReminder(this, task.id);
                    scheduleReminder(this, task);
                    renderTasks();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void confirmDelete(TaskItem task) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa công việc?")
                .setMessage("Bạn có chắc muốn xóa: " + task.title + "?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    cancelReminder(this, task.id);
                    tasks.remove(task);
                    saveTasks();
                    renderTasks();
                    Toast.makeText(this, "Đã xóa", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void loadTasks() {
        tasks.clear();
        String json = preferences.getString(KEY_TASKS, "[]");
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                tasks.add(TaskItem.fromJson(array.getJSONObject(i)));
            }
        } catch (JSONException e) {
            tasks.clear();
        }
    }

    private void saveTasks() {
        JSONArray array = new JSONArray();
        for (TaskItem task : tasks) {
            array.put(task.toJson());
        }
        preferences.edit().putString(KEY_TASKS, array.toString()).apply();
    }

    static ArrayList<TaskItem> readTasks(Context context) {
        ArrayList<TaskItem> result = new ArrayList<>();
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_TASKS, "[]");
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                result.add(TaskItem.fromJson(array.getJSONObject(i)));
            }
        } catch (JSONException ignored) {
        }
        return result;
    }

    static void writeTasks(Context context, List<TaskItem> taskList) {
        JSONArray array = new JSONArray();
        for (TaskItem task : taskList) {
            array.put(task.toJson());
        }
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().putString(KEY_TASKS, array.toString()).apply();
    }

    static void scheduleReminder(Context context, TaskItem task) {
        if (task == null || task.done) return;
        long triggerAt = task.dueTimeMillis;
        long now = System.currentTimeMillis();
        while (triggerAt <= now) {
            triggerAt += 24L * 60L * 60L * 1000L;
        }
        task.dueTimeMillis = triggerAt;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra(EXTRA_TASK_ID, task.id);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, task.id, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent openIntent = new Intent(context, MainActivity.class);
        PendingIntent showIntent = PendingIntent.getActivity(context, task.id, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(triggerAt, showIntent);
        alarmManager.setAlarmClock(info, alarmIntent);
    }

    static void cancelReminder(Context context, int taskId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;
        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, taskId, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(pendingIntent);
    }

    static class TaskItem {
        int id;
        String title = "";
        String detail = "";
        long dueTimeMillis;
        boolean repeatDaily;
        boolean done;

        JSONObject toJson() {
            JSONObject object = new JSONObject();
            try {
                object.put("id", id);
                object.put("title", title);
                object.put("detail", detail);
                object.put("dueTimeMillis", dueTimeMillis);
                object.put("repeatDaily", repeatDaily);
                object.put("done", done);
            } catch (JSONException ignored) {
            }
            return object;
        }

        static TaskItem fromJson(JSONObject object) throws JSONException {
            TaskItem task = new TaskItem();
            task.id = object.optInt("id", (int) (System.currentTimeMillis() & 0x7fffffff));
            task.title = object.optString("title", "");
            task.detail = object.optString("detail", "");
            task.dueTimeMillis = object.optLong("dueTimeMillis", System.currentTimeMillis());
            task.repeatDaily = object.optBoolean("repeatDaily", false);
            task.done = object.optBoolean("done", false);
            return task;
        }
    }
}
