package com.lin.quizlock;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * 从题库目录 URL 拉取 english.json / poems.json / science.json。
 * 失败时保留 {@link BankRepository} 已装入的缓存或内置题库，并在开机网络未就绪时重试。
 */
public final class BankFetcher {
    private static final String TAG = "QuizLock";
    private static final int MAX_BYTES = 2_000_000;
    private static final String[] FILES = {
            BankRepository.FILE_ENGLISH,
            BankRepository.FILE_POEMS,
            BankRepository.FILE_SCIENCE
    };
    private static final String[] LABELS = {"英语", "古诗", "科学"};

    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "quizlock-bank");
            t.setPriority(Thread.NORM_PRIORITY - 1);
            return t;
        }
    });
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final Object LOCK = new Object();
    private static final List<Runnable> WAITERS = new ArrayList<Runnable>();
    private static final boolean[] DONE_OK = new boolean[3];
    private static boolean running = false;
    private static boolean forcePending = false;
    private static int retries = 0;

    private BankFetcher() {}

    public static void kick(Context ctx) {
        kick(ctx, null, false);
    }

    /** @param force 忽略本次进程里已经成功的文件，重新下载全部题库 */
    public static void kick(Context ctx, Runnable onDone, boolean force) {
        if (ctx == null) {
            if (onDone != null) onDone.run();
            return;
        }
        final Context app = ctx.getApplicationContext();
        try {
            BankRepository.ensureLoaded(app);
        } catch (Exception ignored) {
        }
        synchronized (LOCK) {
            if (onDone != null) WAITERS.add(onDone);
            if (force) forcePending = true;
            if (running) return;
            running = true;
            if (forcePending) {
                Arrays.fill(DONE_OK, false);
                retries = 0;
                forcePending = false;
            }
        }
        EXEC.execute(new Runnable() {
            @Override
            public void run() {
                String status;
                try {
                    status = fetchOnce(app);
                } catch (Exception e) {
                    status = "网络题库更新失败";
                    Log.w(TAG, "bank fetch failed", e);
                }
                try {
                    Prefs.setBankStatus(app, status);
                } catch (Exception ignored) {
                }
                boolean again;
                boolean allOk;
                int attempt;
                List<Runnable> copy;
                synchronized (LOCK) {
                    running = false;
                    again = forcePending;
                    allOk = DONE_OK[0] && DONE_OK[1] && DONE_OK[2];
                    if (allOk) {
                        retries = 0;
                        attempt = 99;
                    } else if (!again) {
                        retries++;
                        attempt = retries;
                    } else {
                        attempt = 99;
                    }
                    copy = new ArrayList<Runnable>(WAITERS);
                    WAITERS.clear();
                }
                for (Runnable r : copy) {
                    try {
                        r.run();
                    } catch (Exception ignored) {
                    }
                }
                if (again) {
                    kick(app, null, true);
                    return;
                }
                if (!allOk && attempt <= 2) {
                    final long delay = attempt == 1 ? 20_000L : 50_000L;
                    MAIN.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            kick(app);
                        }
                    }, delay);
                }
            }
        });
    }

    private static String fetchOnce(Context app) {
        if (!isOnline(app)) {
            return "网络题库：当前离线，使用缓存或内置";
        }
        final String base;
        try {
            base = normalizeBase(Prefs.getBankBaseUrl(app));
        } catch (IOException e) {
            return "网络题库：" + shortMsg(e.getMessage());
        }
        String[] detail = new String[3];
        boolean anyFail = false;
        for (int i = 0; i < FILES.length; i++) {
            synchronized (LOCK) {
                if (DONE_OK[i]) {
                    detail[i] = "已更新";
                    continue;
                }
            }
            try {
                String body = httpGet(base + "/" + FILES[i]);
                boolean applied;
                if (i == 0) applied = BankRepository.applyEnglishJson(app, body);
                else if (i == 1) applied = BankRepository.applyPoemsJson(app, body);
                else applied = BankRepository.applyScienceJson(app, body);
                if (!applied) throw new IOException("格式无效");
                synchronized (LOCK) {
                    DONE_OK[i] = true;
                }
                detail[i] = "已更新";
                Log.i(TAG, "bank " + FILES[i] + " updated");
            } catch (IOException e) {
                anyFail = true;
                detail[i] = shortMsg(e.getMessage());
                Log.w(TAG, "bank " + FILES[i] + " " + detail[i]);
            }
        }
        if (!anyFail) {
            String time = new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(new Date());
            return "网络题库：已更新 " + time;
        }
        StringBuilder sb = new StringBuilder("网络题库：");
        for (int i = 0; i < LABELS.length; i++) {
            if (i > 0) sb.append("，");
            sb.append(LABELS[i]).append(detail[i] == null ? "失败" : detail[i]);
        }
        String text = sb.toString();
        return text.length() > 180 ? text.substring(0, 180) : text;
    }

    static String normalizeBase(String base) throws IOException {
        if (base == null) throw new IOException("题库地址为空");
        String b = base.trim();
        while (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        if (b.endsWith(".json")) {
            int slash = b.lastIndexOf('/');
            if (slash > "https://".length()) b = b.substring(0, slash);
        }
        if (!(b.startsWith("https://") || b.startsWith("http://"))) {
            throw new IOException("地址需以 http:// 或 https:// 开头");
        }
        return b;
    }

    private static String httpGet(String url) throws IOException {
        HttpURLConnection conn = null;
        InputStream in = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(20_000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "QuizLock/1.3.0");
            conn.setRequestProperty("Accept", "application/json,text/plain,*/*");
            int code = conn.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP " + code);
            }
            in = conn.getInputStream();
            return BankRepository.readUtf8(in, MAX_BYTES);
        } finally {
            if (in != null) {
                try { in.close(); } catch (IOException ignored) {}
            }
            if (conn != null) conn.disconnect();
        }
    }

    @SuppressWarnings("deprecation")
    private static boolean isOnline(Context ctx) {
        try {
            ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return true;
            NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        } catch (Exception e) {
            return true;
        }
    }

    private static String shortMsg(String s) {
        if (s == null) return "失败";
        String t = s.replace('\n', ' ').trim();
        if (t.length() == 0) return "失败";
        return t.length() > 48 ? t.substring(0, 48) : t;
    }
}
