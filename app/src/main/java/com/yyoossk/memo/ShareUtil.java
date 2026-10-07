package com.yyoossk.memo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.util.Base64;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/** LINE などへの送信と印刷 */
final class ShareUtil {
    static final String LINE_PACKAGE = "jp.naver.line.android";
    static final String AUTHORITY = "com.yyoossk.memo.files";

    /** 印刷が終わるまで WebView を保持しておく */
    private static WebView printView;

    private ShareUtil() {
    }

    /** 「LINEで送る / 他のアプリで送る / 印刷」を選ぶ画面 */
    static void showMenu(Activity a, Note note, Runnable saveFirst) {
        String[] items = {"LINE で送る", "他のアプリで送る", "印刷する"};
        new AlertDialog.Builder(a)
                .setTitle("送る・印刷")
                .setItems(items, (d, which) -> {
                    if (saveFirst != null) saveFirst.run();
                    if (which == 0) send(a, note, true);
                    else if (which == 1) send(a, note, false);
                    else print(a, note);
                })
                .show();
    }

    static void send(Activity a, Note note, boolean line) {
        if (note.type == Note.DRAW) {
            sendImage(a, NoteStore.get(a).drawingFile(note), note.displayTitle(), line);
        } else {
            String text = note.shareText();
            if (text.isEmpty()) {
                Toast.makeText(a, "送る内容がありません", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TEXT, text);
            launch(a, i, line);
        }
    }

    private static void sendImage(Activity a, File file, String title, boolean line) {
        if (!file.exists()) {
            Toast.makeText(a, "まだ何も描かれていません", Toast.LENGTH_SHORT).show();
            return;
        }
        Uri uri = Uri.parse("content://" + AUTHORITY + "/" + file.getName());
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("image/png");
        i.putExtra(Intent.EXTRA_STREAM, uri);
        i.setClipData(ClipData.newRawUri(title, uri));
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        launch(a, i, line);
    }

    private static void launch(Activity a, Intent intent, boolean line) {
        if (line) {
            Intent toLine = new Intent(intent);
            toLine.setPackage(LINE_PACKAGE);
            try {
                a.startActivity(toLine);
                return;
            } catch (ActivityNotFoundException e) {
                Toast.makeText(a, "LINE が見つからないため、送り先を選んでください", Toast.LENGTH_LONG).show();
            }
        }
        a.startActivity(Intent.createChooser(intent, "送り先を選ぶ"));
    }

    static void print(Activity a, Note note) {
        String html = html(a, note);
        String job = note.displayTitle();
        WebView w = new WebView(a);
        w.setWebViewClient(new WebViewClient() {
            private boolean started;

            @Override
            public void onPageFinished(WebView view, String url) {
                if (started) return;
                started = true;
                PrintManager pm = (PrintManager) a.getSystemService(Context.PRINT_SERVICE);
                if (pm == null) {
                    Toast.makeText(a, "この端末では印刷できません", Toast.LENGTH_SHORT).show();
                    return;
                }
                pm.print(job, view.createPrintDocumentAdapter(job),
                        new PrintAttributes.Builder()
                                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                                .build());
            }
        });
        printView = w;
        w.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }

    private static String html(Context c, Note note) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><meta charset='utf-8'><style>")
                .append("body{font-family:sans-serif;color:#1B1F24;margin:24px}")
                .append("h1{font-size:22px;margin:0 0 4px}")
                .append(".d{color:#5B636D;font-size:12px;margin-bottom:16px}")
                .append(".l{border-bottom:1px solid #C9D2DB;min-height:34px;line-height:34px;font-size:15px;white-space:pre-wrap;word-break:break-all}")
                .append(".t{font-size:15px;padding:10px 0;border-bottom:1px solid #E3E5E0}")
                .append(".done{color:#5B636D;text-decoration:line-through}")
                .append(".a{color:#9A3412;font-weight:bold;margin-left:8px}")
                .append("img{max-width:100%}")
                .append("</style></head><body>");
        sb.append("<h1>").append(escape(note.displayTitle())).append("</h1>");
        sb.append("<div class='d'>").append(escape(Ui.formatDate(note.updated))).append("</div>");
        if (note.type == Note.TEXT) {
            String[] lines = note.body.split("\n", -1);
            for (String line : lines) {
                sb.append("<div class='l'>")
                        .append(line.isEmpty() ? "&nbsp;" : escape(line))
                        .append("</div>");
            }
            for (int i = 0; i < 6; i++) sb.append("<div class='l'>&nbsp;</div>");
        } else if (note.type == Note.TODO) {
            for (Note.Item it : note.items) {
                sb.append("<div class='t'>");
                if (it.done) {
                    sb.append("<span class='done'>✓ ").append(escape(it.text)).append("</span>");
                } else {
                    sb.append("□ ").append(escape(it.text));
                    if (it.hasAlarm()) {
                        sb.append("<span class='a'>").append(it.timeLabel());
                        if (it.days != 0) sb.append(" ").append(escape(it.daysLabel()));
                        sb.append("</span>");
                    }
                }
                sb.append("</div>");
            }
        } else {
            File f = NoteStore.get(c).drawingFile(note);
            String b64 = readBase64(f);
            if (b64 != null) {
                sb.append("<img src='data:image/png;base64,").append(b64).append("'>");
            }
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    private static String readBase64(File f) {
        if (!f.exists()) return null;
        try (InputStream in = new FileInputStream(f)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
        } catch (IOException e) {
            return null;
        }
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
