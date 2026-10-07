package com.yyoossk.memo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** メモ・やる事リスト・手書きの 1 件分のデータ */
final class Note {
    static final int TEXT = 0;
    static final int TODO = 1;
    static final int DRAW = 2;

    static final String[] DAY_LABELS = {"日", "月", "火", "水", "木", "金", "土"};

    String id;
    int type;
    String title = "";
    String body = "";
    boolean pinned;
    long created;
    long updated;
    final List<Item> items = new ArrayList<>();

    /** やる事リストの 1 項目 */
    static final class Item {
        String text = "";
        boolean done;
        int hour = -1;
        int minute = -1;
        /** 繰り返す曜日。bit i = DAY_LABELS[i]（i=0 が日曜）。0 なら 1 回だけ */
        int days;

        boolean hasAlarm() {
            return hour >= 0;
        }

        String timeLabel() {
            return String.format(Locale.JAPAN, "%d:%02d", hour, minute);
        }

        String daysLabel() {
            if (days == 0) return "";
            if (days == 0x7F) return "毎日";
            if (days == 0x3E) return "平日";
            StringBuilder sb = new StringBuilder("毎週 ");
            boolean first = true;
            for (int i = 0; i < 7; i++) {
                if ((days & (1 << i)) != 0) {
                    if (!first) sb.append('・');
                    sb.append(DAY_LABELS[i]);
                    first = false;
                }
            }
            return sb.toString();
        }

        /** AlarmClock.EXTRA_DAYS 用（Calendar.SUNDAY=1 … SATURDAY=7） */
        ArrayList<Integer> calendarDays() {
            ArrayList<Integer> list = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                if ((days & (1 << i)) != 0) list.add(Calendar.SUNDAY + i);
            }
            return list;
        }

        JSONObject toJson() throws JSONException {
            JSONObject o = new JSONObject();
            o.put("text", text);
            o.put("done", done);
            o.put("hour", hour);
            o.put("minute", minute);
            o.put("days", days);
            return o;
        }

        static Item fromJson(JSONObject o) {
            Item it = new Item();
            it.text = o.optString("text", "");
            it.done = o.optBoolean("done", false);
            it.hour = o.optInt("hour", -1);
            it.minute = o.optInt("minute", -1);
            it.days = o.optInt("days", 0);
            return it;
        }
    }

    static Note create(int type) {
        Note n = new Note();
        n.id = UUID.randomUUID().toString();
        n.type = type;
        n.created = System.currentTimeMillis();
        n.updated = n.created;
        return n;
    }

    String drawingName() {
        return "draw_" + id + ".png";
    }

    boolean isEmpty() {
        if (type == TEXT) return title.trim().isEmpty() && body.trim().isEmpty();
        if (type == TODO) return title.trim().isEmpty() && items.isEmpty();
        return false;
    }

    int doneCount() {
        int c = 0;
        for (Item it : items) if (it.done) c++;
        return c;
    }

    int alarmCount() {
        int c = 0;
        for (Item it : items) if (it.hasAlarm() && !it.done) c++;
        return c;
    }

    String displayTitle() {
        String t = title.trim();
        if (!t.isEmpty()) return t;
        if (type == TODO) return "やる事リスト";
        if (type == DRAW) return "手書き";
        String b = body.trim();
        if (!b.isEmpty()) {
            int nl = b.indexOf('\n');
            String first = nl >= 0 ? b.substring(0, nl) : b;
            return first.length() > 20 ? first.substring(0, 20) + "…" : first;
        }
        return "無題のメモ";
    }

    String typeLabel() {
        if (type == TODO) return "やる事";
        if (type == DRAW) return "手書き";
        return "メモ";
    }

    boolean matches(String query) {
        if (query.isEmpty()) return true;
        if (title.toLowerCase(Locale.ROOT).contains(query)) return true;
        if (body.toLowerCase(Locale.ROOT).contains(query)) return true;
        for (Item it : items) {
            if (it.text.toLowerCase(Locale.ROOT).contains(query)) return true;
        }
        return false;
    }

    /** LINE などに送るときの文章 */
    String shareText() {
        StringBuilder sb = new StringBuilder();
        String t = title.trim();
        if (type == TODO) {
            sb.append(t.isEmpty() ? "やる事リスト" : t).append('\n');
            for (Item it : items) {
                sb.append(it.done ? "✓ " : "□ ").append(it.text);
                if (it.hasAlarm() && !it.done) {
                    sb.append("（").append(it.timeLabel());
                    if (it.days != 0) sb.append(" ").append(it.daysLabel());
                    sb.append("）");
                }
                sb.append('\n');
            }
        } else {
            if (!t.isEmpty()) sb.append(t).append("\n\n");
            sb.append(body);
        }
        return sb.toString().trim();
    }

    JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("type", type);
        o.put("title", title);
        o.put("body", body);
        o.put("pinned", pinned);
        o.put("created", created);
        o.put("updated", updated);
        JSONArray a = new JSONArray();
        for (Item it : items) a.put(it.toJson());
        o.put("items", a);
        return o;
    }

    static Note fromJson(JSONObject o) {
        Note n = new Note();
        n.id = o.optString("id", UUID.randomUUID().toString());
        n.type = o.optInt("type", TEXT);
        n.title = o.optString("title", "");
        n.body = o.optString("body", "");
        n.pinned = o.optBoolean("pinned", false);
        n.created = o.optLong("created", System.currentTimeMillis());
        n.updated = o.optLong("updated", n.created);
        JSONArray a = o.optJSONArray("items");
        if (a != null) {
            for (int i = 0; i < a.length(); i++) {
                JSONObject io = a.optJSONObject(i);
                if (io != null) n.items.add(Item.fromJson(io));
            }
        }
        return n;
    }
}
