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
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    static final String PREF_NAME = "GhiChuMinhData";
    static final String KEY_TASKS = "tasks";
    static final String EXTRA_TASK_ID = "task_id";

    static final int REPEAT_NONE = 0;
    static final int REPEAT_DAILY = 1;
    static final int REPEAT_WEEKDAYS = 2;
    static final int REPEAT_WEEKLY = 3;
    static final int REPEAT_MONTHLY = 4;

    private static final String[] CATEGORIES = {
            "Nhà máy", "Văn phòng", "Cá nhân", "Khẩn cấp", "Ý tưởng"
    };
    private static final String[] PRIORITIES = {
            "Thấp", "Bình thường", "Cao", "Khẩn cấp"
    };
    private static final String[] REPEAT_OPTIONS = {
            "Nhắc một lần", "Hằng ngày", "Thứ 2 - Thứ 6", "Hằng tuần", "Hằng tháng"
    };
    private static final String[] FILTER_OPTIONS = {
            "Tất cả", "Hôm nay", "Quá hạn", "Sắp tới", "Đã hoàn thành"
    };

    private final ArrayList<TaskItem> tasks = new ArrayList<>();
    private SharedPreferences preferences;
    private LinearLayout listLayout;
    private EditText inputTitle;
    private EditText inputDetail;
    private EditText searchInput;
    private Spinner categorySpinner;
    private Spinner prioritySpinner;
    private Spinner repeatSpinner;
    private Spinner filterSpinner;
    private CheckBox pinBox;
    private Button pickTimeButton;
    private TextView summaryText;
    private final Calendar selectedCalendar = Calendar.getInstance();
    private final SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        selectedCalendar.setTimeInMillis(System.currentTimeMillis());
        selectedCalendar.add(Calendar.HOUR_OF_DAY, 1);
        selectedCalendar.set(Calendar.SECOND, 0);
        selectedCalendar.set(Calendar.MILLISECOND, 0);
        requestNotificationPermissionIfNeeded();
        loadTasks();
        buildScreen();
        applySharedText(getIntent());
        renderTasks();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        applySharedText(intent);
    }

    private void applySharedText(Intent intent) {
        if (intent == null || !Intent.ACTION_SEND.equals(intent.getAction())) return;
        CharSequence shared = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
        if (shared != null && inputDetail != null) {
            inputDetail.setText(shared.toString());
            inputDetail.requestFocus();
            Toast.makeText(this, "Đã nhận nội dung chia sẻ", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
        }
    }

    private void buildScreen() {
        ScrollView page = new ScrollView(this);
        page.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(28));
        root.setBackgroundColor(Color.rgb(244, 248, 249));
        page.addView(root);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_ml_dragon);
        header.addView(logo, new LinearLayout.LayoutParams(dp(58), dp(58)));

        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.VERTICAL);
        heading.setPadding(dp(12), 0, 0, 0);
        TextView title = text("Ghi Chú Minh", 25, true, Color.rgb(9, 92, 95));
        TextView subtitle = text("Quản lý công việc cá nhân • v2.0", 13, false, Color.DKGRAY);
        heading.addView(title);
        heading.addView(subtitle);
        header.addView(heading, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        root.addView(header);

        searchInput = new EditText(this);
        searchInput.setHint("Tìm theo tên, nội dung hoặc nhóm...");
        searchInput.setSingleLine(true);
        searchInput.setPadding(dp(14), dp(10), dp(14), dp(10));
        root.addView(searchInput, matchWrap(dp(12), dp(10)));
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { renderTasks(); }
            @Override public void afterTextChanged(Editable s) { }
        });

        filterSpinner = spinner(FILTER_OPTIONS);
        root.addView(filterSpinner, matchWrap(0, dp(12)));
        filterSpinner.setOnItemSelectedListener(new SimpleItemSelectedListener(this::renderTasks));

        TextView formTitle = text("Tạo ghi chú / công việc mới", 18, true, Color.rgb(38, 50, 56));
        root.addView(formTitle, matchWrap(dp(4), dp(8)));

        inputTitle = new EditText(this);
        inputTitle.setHint("Tên công việc / ghi chú");
        inputTitle.setMaxLines(2);
        root.addView(inputTitle, matchWrap(0, dp(4)));

        inputDetail = new EditText(this);
        inputDetail.setHint("Nội dung chi tiết");
        inputDetail.setMinLines(2);
        inputDetail.setGravity(Gravity.TOP);
        root.addView(inputDetail, matchWrap(0, dp(4)));

        TextView categoryLabel = text("Nhóm công việc", 13, true, Color.DKGRAY);
        root.addView(categoryLabel);
        categorySpinner = spinner(CATEGORIES);
        root.addView(categorySpinner, matchWrap(0, dp(4)));

        TextView priorityLabel = text("Mức ưu tiên", 13, true, Color.DKGRAY);
        root.addView(priorityLabel);
        prioritySpinner = spinner(PRIORITIES);
        prioritySpinner.setSelection(1);
        root.addView(prioritySpinner, matchWrap(0, dp(4)));

        pinBox = new CheckBox(this);
        pinBox.setText("Ghim việc này lên đầu danh sách");
        root.addView(pinBox);

        TextView repeatLabel = text("Chu kỳ nhắc", 13, true, Color.DKGRAY);
        root.addView(repeatLabel);
        repeatSpinner = spinner(REPEAT_OPTIONS);
        root.addView(repeatSpinner, matchWrap(0, dp(4)));

        pickTimeButton = new Button(this);
        pickTimeButton.setText(timeButtonText(selectedCalendar.getTimeInMillis()));
        pickTimeButton.setOnClickListener(v -> pickDateTime(selectedCalendar, pickTimeButton));
        root.addView(pickTimeButton, matchWrap(0, dp(6)));

        Button saveButton = new Button(this);
        saveButton.setText("LƯU VÀ ĐẶT NHẮC VIỆC");
        saveButton.setTextColor(Color.WHITE);
        saveButton.setBackgroundColor(Color.rgb(13, 127, 131));
        saveButton.setOnClickListener(v -> saveNewTask());
        root.addView(saveButton, matchWrap(0, dp(10)));

        summaryText = text("", 15, true, Color.rgb(38, 50, 56));
        root.addView(summaryText, matchWrap(dp(6), dp(8)));

        listLayout = new LinearLayout(this);
        listLayout.setOrientation(LinearLayout.VERTICAL);
        root.addView(listLayout);

        Button feedbackButton = new Button(this);
        feedbackButton.setText("Đóng góp ý kiến / Báo lỗi");
        feedbackButton.setOnClickListener(v -> sendFeedback());
        root.addView(feedbackButton, matchWrap(dp(8), 0));

        setContentView(page);
    }

    private void sendFeedback() {
        String subject = "Góp ý ứng dụng Ghi Chú Minh v2.0";
        String body = "Nội dung góp ý:\n\nThiết bị: " + Build.MANUFACTURER + " " + Build.MODEL
                + "\nAndroid: " + Build.VERSION.RELEASE;
        Intent email = new Intent(Intent.ACTION_SENDTO);
        email.setData(Uri.parse("mailto:lienhe@mltudonghoa.pro.vn"));
        email.putExtra(Intent.EXTRA_SUBJECT, subject);
        email.putExtra(Intent.EXTRA_TEXT, body);
        try {
            startActivity(email);
        } catch (Exception e) {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_SUBJECT, subject);
            share.putExtra(Intent.EXTRA_TEXT, body + "\n\nGửi tới: lienhe@mltudonghoa.pro.vn");
            startActivity(Intent.createChooser(share, "Gửi góp ý"));
        }
    }

    private void saveNewTask() {
        String title = inputTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, "Bạn chưa nhập tên công việc", Toast.LENGTH_SHORT).show();
            return;
        }

        TaskItem task = new TaskItem();
        task.id = (int) (System.currentTimeMillis() & 0x7fffffff);
        task.title = title;
        task.detail = inputDetail.getText().toString().trim();
        task.category = CATEGORIES[categorySpinner.getSelectedItemPosition()];
        task.priority = prioritySpinner.getSelectedItemPosition();
        task.pinned = pinBox.isChecked();
        task.recurrence = repeatSpinner.getSelectedItemPosition();
        task.dueTimeMillis = selectedCalendar.getTimeInMillis();
        task.done = false;
        task.createdAtMillis = System.currentTimeMillis();
        tasks.add(task);
        saveTasks();
        scheduleReminder(this, task);
        clearForm();
        renderTasks();
        Toast.makeText(this, "Đã lưu công việc", Toast.LENGTH_SHORT).show();
    }

    private void clearForm() {
        inputTitle.setText("");
        inputDetail.setText("");
        categorySpinner.setSelection(0);
        prioritySpinner.setSelection(1);
        pinBox.setChecked(false);
        repeatSpinner.setSelection(0);
        selectedCalendar.setTimeInMillis(System.currentTimeMillis());
        selectedCalendar.add(Calendar.HOUR_OF_DAY, 1);
        selectedCalendar.set(Calendar.SECOND, 0);
        selectedCalendar.set(Calendar.MILLISECOND, 0);
        pickTimeButton.setText(timeButtonText(selectedCalendar.getTimeInMillis()));
    }

    private void renderTasks() {
        if (listLayout == null) return;
        listLayout.removeAllViews();

        int active = 0;
        int overdue = 0;
        long now = System.currentTimeMillis();
        for (TaskItem task : tasks) {
            if (!task.done) {
                active++;
                if (task.dueTimeMillis < now) overdue++;
            }
        }
        summaryText.setText("Còn " + active + " việc • " + overdue + " quá hạn • " + tasks.size() + " tổng số");

        ArrayList<TaskItem> visible = new ArrayList<>();
        String query = searchInput == null ? "" : searchInput.getText().toString().trim().toLowerCase(Locale.getDefault());
        int filter = filterSpinner == null ? 0 : filterSpinner.getSelectedItemPosition();
        for (TaskItem task : tasks) {
            if (matchesSearch(task, query) && matchesFilter(task, filter, now)) {
                visible.add(task);
            }
        }

        Collections.sort(visible, new Comparator<TaskItem>() {
            @Override
            public int compare(TaskItem a, TaskItem b) {
                if (a.pinned != b.pinned) return a.pinned ? -1 : 1;
                if (a.done != b.done) return a.done ? 1 : -1;
                if (a.priority != b.priority) return Integer.compare(b.priority, a.priority);
                return Long.compare(a.dueTimeMillis, b.dueTimeMillis);
            }
        });

        if (visible.isEmpty()) {
            TextView empty = text("Không có ghi chú phù hợp.", 16, false, Color.GRAY);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(28), 0, dp(28));
            listLayout.addView(empty);
            return;
        }

        for (TaskItem task : visible) {
            listLayout.addView(createTaskView(task));
        }
    }

    private boolean matchesSearch(TaskItem task, String query) {
        if (query.isEmpty()) return true;
        return task.title.toLowerCase(Locale.getDefault()).contains(query)
                || task.detail.toLowerCase(Locale.getDefault()).contains(query)
                || task.category.toLowerCase(Locale.getDefault()).contains(query)
                || priorityName(task.priority).toLowerCase(Locale.getDefault()).contains(query);
    }

    private boolean matchesFilter(TaskItem task, int filter, long now) {
        switch (filter) {
            case 1: return !task.done && isSameDay(task.dueTimeMillis, now);
            case 2: return !task.done && task.dueTimeMillis < now;
            case 3: return !task.done && task.dueTimeMillis >= now;
            case 4: return task.done;
            default: return true;
        }
    }

    private View createTaskView(TaskItem task) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(14));
        bg.setColor(cardColor(task.priority));
        bg.setStroke(dp(1), task.done ? Color.LTGRAY : Color.rgb(174, 198, 199));
        card.setBackground(bg);

        TextView title = text((task.pinned ? "📌 " : "") + task.title, 17, true, Color.rgb(38, 50, 56));
        card.addView(title);

        TextView meta = text(task.category + " • " + priorityName(task.priority), 13, true, priorityColor(task.priority));
        meta.setPadding(0, dp(3), 0, 0);
        card.addView(meta);

        if (!task.detail.isEmpty()) {
            TextView detail = text(task.detail, 14, false, Color.DKGRAY);
            detail.setPadding(0, dp(5), 0, 0);
            card.addView(detail);
        }

        String state = task.done ? "Đã hoàn thành" : repeatName(task.recurrence) + ": " + displayFormat.format(task.dueTimeMillis);
        TextView time = text(state, 13, false, task.done ? Color.GRAY : Color.rgb(12, 91, 148));
        time.setPadding(0, dp(6), 0, dp(6));
        card.addView(time);

        if (task.done) {
            title.setPaintFlags(title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            title.setAlpha(0.6f);
        }

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button doneButton = smallButton(task.done ? "Mở lại" : "Hoàn thành");
        doneButton.setOnClickListener(v -> toggleDone(task));
        actions.addView(doneButton, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button editButton = smallButton("Sửa");
        editButton.setOnClickListener(v -> showEditDialog(task));
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        actionParams.setMargins(dp(6), 0, 0, 0);
        actions.addView(editButton, actionParams);

        Button deleteButton = smallButton("Xóa");
        deleteButton.setOnClickListener(v -> confirmDelete(task));
        LinearLayout.LayoutParams deleteParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        deleteParams.setMargins(dp(6), 0, 0, 0);
        actions.addView(deleteButton, deleteParams);
        card.addView(actions);

        LinearLayout.LayoutParams params = matchWrap(0, dp(10));
        card.setLayoutParams(params);
        return card;
    }

    private void toggleDone(TaskItem task) {
        task.done = !task.done;
        if (task.done) {
            cancelReminder(this, task.id);
        } else {
            if (task.dueTimeMillis <= System.currentTimeMillis() && task.recurrence == REPEAT_NONE) {
                task.dueTimeMillis = System.currentTimeMillis() + 60_000L;
            }
            scheduleReminder(this, task);
        }
        saveTasks();
        renderTasks();
    }

    private void showEditDialog(TaskItem task) {
        ScrollView scroll = new ScrollView(this);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(18), dp(8), dp(18), dp(8));
        scroll.addView(form);

        EditText titleEdit = new EditText(this);
        titleEdit.setHint("Tên công việc");
        titleEdit.setText(task.title);
        form.addView(titleEdit);

        EditText detailEdit = new EditText(this);
        detailEdit.setHint("Nội dung chi tiết");
        detailEdit.setMinLines(2);
        detailEdit.setText(task.detail);
        form.addView(detailEdit);

        Spinner categoryEdit = spinner(CATEGORIES);
        categoryEdit.setSelection(indexOf(CATEGORIES, task.category));
        form.addView(categoryEdit);

        Spinner priorityEdit = spinner(PRIORITIES);
        priorityEdit.setSelection(clamp(task.priority, 0, PRIORITIES.length - 1));
        form.addView(priorityEdit);

        CheckBox pinEdit = new CheckBox(this);
        pinEdit.setText("Ghim lên đầu danh sách");
        pinEdit.setChecked(task.pinned);
        form.addView(pinEdit);

        Spinner repeatEdit = spinner(REPEAT_OPTIONS);
        repeatEdit.setSelection(clamp(task.recurrence, 0, REPEAT_OPTIONS.length - 1));
        form.addView(repeatEdit);

        Calendar editCalendar = Calendar.getInstance();
        editCalendar.setTimeInMillis(task.dueTimeMillis);
        Button timeEdit = new Button(this);
        timeEdit.setText(timeButtonText(editCalendar.getTimeInMillis()));
        timeEdit.setOnClickListener(v -> pickDateTime(editCalendar, timeEdit));
        form.addView(timeEdit);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Sửa ghi chú")
                .setView(scroll)
                .setPositiveButton("Lưu", null)
                .setNegativeButton("Hủy", null)
                .create();
        dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn -> {
            String newTitle = titleEdit.getText().toString().trim();
            if (newTitle.isEmpty()) {
                titleEdit.setError("Không được để trống");
                return;
            }
            cancelReminder(this, task.id);
            task.title = newTitle;
            task.detail = detailEdit.getText().toString().trim();
            task.category = CATEGORIES[categoryEdit.getSelectedItemPosition()];
            task.priority = priorityEdit.getSelectedItemPosition();
            task.pinned = pinEdit.isChecked();
            task.recurrence = repeatEdit.getSelectedItemPosition();
            task.dueTimeMillis = editCalendar.getTimeInMillis();
            task.done = false;
            saveTasks();
            scheduleReminder(this, task);
            renderTasks();
            dialog.dismiss();
        }));
        dialog.show();
    }

    private void confirmDelete(TaskItem task) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa ghi chú?")
                .setMessage(task.title)
                .setPositiveButton("Xóa", (dialog, which) -> {
                    cancelReminder(this, task.id);
                    tasks.remove(task);
                    saveTasks();
                    renderTasks();
                })
                .setNegativeButton("Hủy", null)
                .show();
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
                            calendar.get(Calendar.MINUTE), true);
                    timeDialog.show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dateDialog.show();
    }

    private String timeButtonText(long millis) {
        return "Thời gian nhắc: " + displayFormat.format(millis);
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
            Toast.makeText(this, "Không đọc được dữ liệu cũ", Toast.LENGTH_LONG).show();
        }
    }

    private void saveTasks() {
        writeTasks(this, tasks);
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
        } catch (JSONException ignored) { }
        return result;
    }

    static void writeTasks(Context context, List<TaskItem> taskList) {
        JSONArray array = new JSONArray();
        for (TaskItem task : taskList) array.put(task.toJson());
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_TASKS, array.toString()).apply();
    }

    static void scheduleReminder(Context context, TaskItem task) {
        if (task == null || task.done) return;
        long triggerAt = task.dueTimeMillis;
        long now = System.currentTimeMillis();
        if (triggerAt <= now) {
            if (task.recurrence == REPEAT_NONE) return;
            triggerAt = nextOccurrenceAfter(triggerAt, task.recurrence, now);
            task.dueTimeMillis = triggerAt;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra(EXTRA_TASK_ID, task.id);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, task.id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent openIntent = new Intent(context, MainActivity.class);
        PendingIntent showIntent = PendingIntent.getActivity(context, task.id, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(triggerAt, showIntent), alarmIntent);
    }

    static long nextOccurrenceAfter(long current, int recurrence, long afterMillis) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(current);
        do {
            switch (recurrence) {
                case REPEAT_DAILY:
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                    break;
                case REPEAT_WEEKDAYS:
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                    while (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
                            || calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                        calendar.add(Calendar.DAY_OF_YEAR, 1);
                    }
                    break;
                case REPEAT_WEEKLY:
                    calendar.add(Calendar.WEEK_OF_YEAR, 1);
                    break;
                case REPEAT_MONTHLY:
                    calendar.add(Calendar.MONTH, 1);
                    break;
                default:
                    return current;
            }
        } while (calendar.getTimeInMillis() <= afterMillis);
        return calendar.getTimeInMillis();
    }

    static void cancelReminder(Context context, int taskId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;
        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, taskId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        alarmManager.cancel(pendingIntent);
    }

    static String repeatName(int recurrence) {
        switch (recurrence) {
            case REPEAT_DAILY: return "Hằng ngày";
            case REPEAT_WEEKDAYS: return "Ngày làm việc";
            case REPEAT_WEEKLY: return "Hằng tuần";
            case REPEAT_MONTHLY: return "Hằng tháng";
            default: return "Nhắc lúc";
        }
    }

    private static boolean isSameDay(long first, long second) {
        Calendar a = Calendar.getInstance();
        Calendar b = Calendar.getInstance();
        a.setTimeInMillis(first);
        b.setTimeInMillis(second);
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    private int cardColor(int priority) {
        switch (priority) {
            case 3: return Color.rgb(255, 232, 232);
            case 2: return Color.rgb(255, 244, 220);
            case 0: return Color.rgb(235, 249, 239);
            default: return Color.WHITE;
        }
    }

    private int priorityColor(int priority) {
        switch (priority) {
            case 3: return Color.rgb(183, 28, 28);
            case 2: return Color.rgb(230, 81, 0);
            case 0: return Color.rgb(46, 125, 50);
            default: return Color.rgb(69, 90, 100);
        }
    }

    private String priorityName(int priority) {
        return PRIORITIES[clamp(priority, 0, PRIORITIES.length - 1)];
    }

    private Spinner spinner(String[] values) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, values);
        spinner.setAdapter(adapter);
        return spinner;
    }

    private TextView text(String value, int size, boolean bold, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT_BOLD);
        return view;
    }

    private Button smallButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(12);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        button.setPadding(dp(4), dp(5), dp(4), dp(5));
        return button;
    }

    private LinearLayout.LayoutParams matchWrap(int top, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, top, 0, bottom);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static int indexOf(String[] values, String target) {
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(target)) return i;
        }
        return 0;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static class SimpleItemSelectedListener implements android.widget.AdapterView.OnItemSelectedListener {
        private final Runnable action;
        SimpleItemSelectedListener(Runnable action) { this.action = action; }
        @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) { action.run(); }
        @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { action.run(); }
    }

    static class TaskItem {
        int id;
        String title = "";
        String detail = "";
        String category = "Văn phòng";
        int priority = 1;
        boolean pinned;
        int recurrence = REPEAT_NONE;
        long dueTimeMillis;
        boolean done;
        long createdAtMillis;

        JSONObject toJson() {
            JSONObject object = new JSONObject();
            try {
                object.put("id", id);
                object.put("title", title);
                object.put("detail", detail);
                object.put("category", category);
                object.put("priority", priority);
                object.put("pinned", pinned);
                object.put("recurrence", recurrence);
                object.put("dueTimeMillis", dueTimeMillis);
                object.put("done", done);
                object.put("createdAtMillis", createdAtMillis);
                object.put("repeatDaily", recurrence == REPEAT_DAILY);
            } catch (JSONException ignored) { }
            return object;
        }

        static TaskItem fromJson(JSONObject object) {
            TaskItem task = new TaskItem();
            task.id = object.optInt("id", (int) (System.currentTimeMillis() & 0x7fffffff));
            task.title = object.optString("title", "");
            task.detail = object.optString("detail", "");
            task.category = object.optString("category", "Văn phòng");
            task.priority = object.optInt("priority", 1);
            task.pinned = object.optBoolean("pinned", false);
            if (object.has("recurrence")) {
                task.recurrence = object.optInt("recurrence", REPEAT_NONE);
            } else {
                task.recurrence = object.optBoolean("repeatDaily", false) ? REPEAT_DAILY : REPEAT_NONE;
            }
            task.dueTimeMillis = object.optLong("dueTimeMillis", System.currentTimeMillis() + 3_600_000L);
            task.done = object.optBoolean("done", false);
            task.createdAtMillis = object.optLong("createdAtMillis", System.currentTimeMillis());
            return task;
        }
    }
}
