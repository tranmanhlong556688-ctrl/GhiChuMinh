package com.minh.ghichuminh;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String PREF_NAME = "GhiChuMinhData";
    private static final String KEY_NOTES = "notes";

    private final ArrayList<String> notes = new ArrayList<>();
    private LinearLayout listLayout;
    private EditText input;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        loadNotes();
        buildScreen();
        renderNotes();
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 28, 28, 28);
        root.setBackgroundColor(0xFFF7F9FC);

        TextView title = new TextView(this);
        title.setText("Ghi Chú Minh");
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(0xFF0D47A1);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Ghi chú nhanh, lưu trực tiếp trên điện thoại.");
        subtitle.setTextSize(14);
        subtitle.setTextColor(0xFF455A64);
        subtitle.setPadding(0, 8, 0, 20);
        root.addView(subtitle);

        input = new EditText(this);
        input.setHint("Nhập ghi chú mới...");
        input.setMinLines(3);
        input.setGravity(Gravity.TOP);
        input.setTextSize(16);
        root.addView(input, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        Button addButton = new Button(this);
        addButton.setText("Lưu ghi chú");
        addButton.setOnClickListener(v -> addNote());
        root.addView(addButton);

        ScrollView scrollView = new ScrollView(this);
        listLayout = new LinearLayout(this);
        listLayout.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(listLayout);
        root.addView(scrollView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

        setContentView(root);
    }

    private void addNote() {
        String text = input.getText().toString().trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Bạn chưa nhập nội dung ghi chú", Toast.LENGTH_SHORT).show();
            return;
        }
        String time = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        notes.add(0, time + "\n" + text);
        input.setText("");
        saveNotes();
        renderNotes();
    }

    private void renderNotes() {
        listLayout.removeAllViews();
        if (notes.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("Chưa có ghi chú nào.");
            empty.setTextSize(16);
            empty.setTextColor(0xFF78909C);
            empty.setPadding(0, 32, 0, 0);
            listLayout.addView(empty);
            return;
        }

        for (int i = 0; i < notes.size(); i++) {
            final int index = i;
            TextView noteView = new TextView(this);
            noteView.setText(notes.get(i));
            noteView.setTextSize(16);
            noteView.setTextColor(0xFF263238);
            noteView.setPadding(20, 20, 20, 20);
            noteView.setBackgroundColor(0xFFFFFFFF);
            noteView.setOnClickListener(v -> editNote(index));
            noteView.setOnLongClickListener(v -> {
                confirmDelete(index);
                return true;
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 18, 0, 0);
            listLayout.addView(noteView, params);
        }
    }

    private void editNote(int index) {
        EditText editor = new EditText(this);
        editor.setMinLines(4);
        editor.setGravity(Gravity.TOP);
        editor.setText(notes.get(index));

        new AlertDialog.Builder(this)
                .setTitle("Sửa ghi chú")
                .setView(editor)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    notes.set(index, editor.getText().toString().trim());
                    saveNotes();
                    renderNotes();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void confirmDelete(int index) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa ghi chú")
                .setMessage("Bạn có chắc muốn xóa ghi chú này không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    notes.remove(index);
                    saveNotes();
                    renderNotes();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void loadNotes() {
        notes.clear();
        String json = preferences.getString(KEY_NOTES, "[]");
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                notes.add(array.getString(i));
            }
        } catch (JSONException e) {
            notes.clear();
        }
    }

    private void saveNotes() {
        JSONArray array = new JSONArray();
        for (String note : notes) {
            array.put(note);
        }
        preferences.edit().putString(KEY_NOTES, array.toString()).apply();
    }
}
