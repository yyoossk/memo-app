package com.yyoossk.memo;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 端末内（アプリ専用領域）の notes.json に保存する */
final class NoteStore {
    private static final String TAG = "NoteStore";
    private static NoteStore instance;

    private final File dir;
    private final File file;
    private final List<Note> notes = new ArrayList<>();

    static synchronized NoteStore get(Context context) {
        if (instance == null) instance = new NoteStore(context.getApplicationContext());
        return instance;
    }

    private NoteStore(Context context) {
        dir = context.getFilesDir();
        file = new File(dir, "notes.json");
        load();
    }

    private void load() {
        if (!file.exists()) return;
        try (InputStream in = new FileInputStream(file)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            JSONArray a = new JSONArray(new String(out.toByteArray(), StandardCharsets.UTF_8));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i);
                if (o != null) notes.add(Note.fromJson(o));
            }
        } catch (IOException | JSONException e) {
            Log.e(TAG, "load failed", e);
        }
    }

    /** ピン留めを先頭に、更新日が新しい順 */
    List<Note> all() {
        List<Note> list = new ArrayList<>(notes);
        Collections.sort(list, (a, b) -> {
            if (a.pinned != b.pinned) return a.pinned ? -1 : 1;
            return Long.compare(b.updated, a.updated);
        });
        return list;
    }

    Note find(String id) {
        if (id == null) return null;
        for (Note n : notes) if (n.id.equals(id)) return n;
        return null;
    }

    boolean contains(Note note) {
        return find(note.id) != null;
    }

    void put(Note note) {
        note.updated = System.currentTimeMillis();
        if (!contains(note)) notes.add(note);
        write();
    }

    /** 並び順を変えずに保存（ピン留めなど） */
    void saveQuietly() {
        write();
    }

    void remove(Note note) {
        Note existing = find(note.id);
        if (existing != null) notes.remove(existing);
        File d = drawingFile(note);
        if (d.exists() && !d.delete()) Log.w(TAG, "could not delete " + d);
        write();
    }

    File drawingFile(Note note) {
        return new File(dir, note.drawingName());
    }

    private void write() {
        JSONArray a = new JSONArray();
        try {
            for (Note n : notes) a.put(n.toJson());
        } catch (JSONException e) {
            Log.e(TAG, "json failed", e);
            return;
        }
        File tmp = new File(dir, "notes.json.tmp");
        try (OutputStream out = new FileOutputStream(tmp)) {
            out.write(a.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Log.e(TAG, "write failed", e);
            return;
        }
        if (!tmp.renameTo(file)) Log.e(TAG, "rename failed");
    }
}
