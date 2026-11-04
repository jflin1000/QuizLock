package com.lin.quizlock;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 英语 / 古诗 / 科学题库。优先使用版本更高的一份：
 * 安装包内置 assets（来自仓库 banks/）与本地缓存。
 * 联网成功后由 {@link BankFetcher} 覆盖缓存。
 */
public final class BankRepository {
    public static final String FILE_ENGLISH = "english.json";
    public static final String FILE_POEMS = "poems.json";
    public static final String FILE_SCIENCE = "science.json";

    public static final class Word {
        public final String en;
        public final String zh;
        public final int level;

        Word(String en, String zh, int level) {
            this.en = en;
            this.zh = zh;
            this.level = level;
        }
    }

    public static final class Couplet {
        public final String prev;
        public final String next;
        public final String title;
        public final String author;

        Couplet(String prev, String next, String title, String author) {
            this.prev = prev;
            this.next = next;
            this.title = title;
            this.author = author;
        }
    }

    public static final class Title {
        public final String title;
        public final String author;
        public final String first;

        Title(String title, String author, String first) {
            this.title = title;
            this.author = author;
            this.first = first;
        }
    }

    public static final class SciItem {
        public final String prompt;
        public final String answer;
        public final String[] distractors;

        SciItem(String prompt, String answer, String[] distractors) {
            this.prompt = prompt;
            this.answer = answer;
            this.distractors = distractors;
        }
    }

    private static final class Parsed<T> {
        final int version;
        final String subject;
        final T data;

        Parsed(int version, String subject, T data) {
            this.version = version;
            this.subject = subject;
            this.data = data;
        }
    }

    private static final class PoemData {
        final List<Couplet> couplets;
        final List<Title> titles;

        PoemData(List<Couplet> couplets, List<Title> titles) {
            this.couplets = couplets;
            this.titles = titles;
        }
    }

    private static volatile boolean loaded = false;
    private static volatile List<Word> wordsRef = Collections.emptyList();
    private static volatile List<Couplet> coupletRef = Collections.emptyList();
    private static volatile List<Title> titleRef = Collections.emptyList();
    private static volatile List<SciItem> scienceRef = Collections.emptyList();
    private static volatile String englishSubject = "英语单词";
    private static volatile String poemSubject = "语文古诗词";
    private static volatile String scienceSubject = "科学常识";
    private static volatile String wordsFrom = "内置";
    private static volatile String poemsFrom = "内置";
    private static volatile String scienceFrom = "内置";

    private BankRepository() {}

    public static synchronized void ensureLoaded(Context ctx) {
        if (loaded || ctx == null) return;
        Context app = ctx.getApplicationContext();
        loadEnglish(app);
        loadPoems(app);
        loadScience(app);
        loaded = true;
    }

    public static List<Word> words() {
        return wordsRef;
    }

    public static List<Couplet> couplets() {
        return coupletRef;
    }

    public static List<Title> titles() {
        return titleRef;
    }

    public static List<SciItem> science() {
        return scienceRef;
    }

    public static String englishSubject() {
        return englishSubject;
    }

    public static String poemSubject() {
        return poemSubject;
    }

    public static String scienceSubject() {
        return scienceSubject;
    }

    public static synchronized String summary() {
        if (!loaded) return "题库尚未加载";
        return "英语 " + wordsRef.size() + " 词 · 古诗 " + coupletRef.size()
                + " 句 · 科学 " + scienceRef.size() + " 题（"
                + sourceLabel() + "）";
    }

    public static synchronized boolean applyEnglishJson(Context ctx, String json) {
        Parsed<List<Word>> parsed = parseEnglish(json);
        if (parsed == null) return false;
        writeCache(ctx, FILE_ENGLISH, json);
        publishEnglish(parsed, "网络");
        loaded = true;
        return true;
    }

    public static synchronized boolean applyPoemsJson(Context ctx, String json) {
        Parsed<PoemData> parsed = parsePoems(json);
        if (parsed == null) return false;
        writeCache(ctx, FILE_POEMS, json);
        publishPoems(parsed, "网络");
        loaded = true;
        return true;
    }

    public static synchronized boolean applyScienceJson(Context ctx, String json) {
        Parsed<List<SciItem>> parsed = parseScience(json);
        if (parsed == null) return false;
        writeCache(ctx, FILE_SCIENCE, json);
        publishScience(parsed, "网络");
        loaded = true;
        return true;
    }

    private static void loadEnglish(Context app) {
        Parsed<List<Word>> asset = parseEnglish(readAsset(app, FILE_ENGLISH));
        Parsed<List<Word>> cache = parseEnglish(readCache(app, FILE_ENGLISH));
        Parsed<List<Word>> chosen = prefer(asset, cache);
        if (chosen == null) {
            wordsRef = Collections.emptyList();
            wordsFrom = "无";
            return;
        }
        publishEnglish(chosen, chosen == cache ? "缓存" : "内置");
    }

    private static void loadPoems(Context app) {
        Parsed<PoemData> asset = parsePoems(readAsset(app, FILE_POEMS));
        Parsed<PoemData> cache = parsePoems(readCache(app, FILE_POEMS));
        Parsed<PoemData> chosen = prefer(asset, cache);
        if (chosen == null) {
            coupletRef = Collections.emptyList();
            titleRef = Collections.emptyList();
            poemsFrom = "无";
            return;
        }
        publishPoems(chosen, chosen == cache ? "缓存" : "内置");
    }

    private static void loadScience(Context app) {
        Parsed<List<SciItem>> asset = parseScience(readAsset(app, FILE_SCIENCE));
        Parsed<List<SciItem>> cache = parseScience(readCache(app, FILE_SCIENCE));
        Parsed<List<SciItem>> chosen = prefer(asset, cache);
        if (chosen == null) {
            scienceRef = Collections.emptyList();
            scienceFrom = "无";
            return;
        }
        publishScience(chosen, chosen == cache ? "缓存" : "内置");
    }

    private static void publishEnglish(Parsed<List<Word>> parsed, String from) {
        wordsRef = Collections.unmodifiableList(parsed.data);
        if (parsed.subject.length() > 0) englishSubject = parsed.subject;
        wordsFrom = from;
    }

    private static void publishPoems(Parsed<PoemData> parsed, String from) {
        coupletRef = Collections.unmodifiableList(parsed.data.couplets);
        titleRef = Collections.unmodifiableList(parsed.data.titles);
        if (parsed.subject.length() > 0) poemSubject = parsed.subject;
        poemsFrom = from;
    }

    private static void publishScience(Parsed<List<SciItem>> parsed, String from) {
        scienceRef = Collections.unmodifiableList(parsed.data);
        if (parsed.subject.length() > 0) scienceSubject = parsed.subject;
        scienceFrom = from;
    }

    private static String sourceLabel() {
        if (wordsFrom.equals(poemsFrom) && poemsFrom.equals(scienceFrom)) return wordsFrom;
        return "英语" + wordsFrom + " / 古诗" + poemsFrom + " / 科学" + scienceFrom;
    }

    private static <T> Parsed<T> prefer(Parsed<T> asset, Parsed<T> cache) {
        if (cache == null) return asset;
        if (asset == null) return cache;
        if (asset.version > cache.version) return asset;
        return cache;
    }

    static Parsed<List<Word>> parseEnglish(String json) {
        JSONObject o = objectOf(json);
        if (o == null) return null;
        JSONArray arr = o.optJSONArray("words");
        if (arr == null) return null;
        List<Word> list = new ArrayList<Word>();
        Set<String> ens = new LinkedHashSet<String>();
        Set<String> zhs = new LinkedHashSet<String>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject w = arr.optJSONObject(i);
            if (w == null) continue;
            String en = w.optString("en", "").trim();
            String zh = w.optString("zh", "").trim();
            int level = w.optInt("level", 6);
            if (en.length() == 0 || zh.length() == 0) continue;
            if (!ens.add(en) || !zhs.add(zh)) continue;
            list.add(new Word(en, zh, level));
        }
        if (list.size() < 4) return null;
        return new Parsed<List<Word>>(o.optInt("version", 1), o.optString("subject", "英语单词").trim(), list);
    }

    static Parsed<PoemData> parsePoems(String json) {
        JSONObject o = objectOf(json);
        if (o == null) return null;
        JSONArray arr = o.optJSONArray("couplets");
        if (arr == null) return null;
        List<Couplet> couplets = new ArrayList<Couplet>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject c = arr.optJSONObject(i);
            if (c == null) continue;
            String prev = c.optString("prev", "").trim();
            String next = c.optString("next", "").trim();
            if (prev.length() == 0 || next.length() == 0 || prev.equals(next)) continue;
            couplets.add(new Couplet(prev, next, c.optString("title", "").trim(), c.optString("author", "").trim()));
        }
        if (couplets.size() < 4) return null;
        List<Title> titles = new ArrayList<Title>();
        JSONArray titleArr = o.optJSONArray("titles");
        if (titleArr != null) {
            for (int i = 0; i < titleArr.length(); i++) {
                JSONObject t = titleArr.optJSONObject(i);
                if (t == null) continue;
                String title = t.optString("title", "").trim();
                String author = t.optString("author", "").trim();
                String first = t.optString("first", "").trim();
                if (title.length() == 0 || author.length() == 0) continue;
                titles.add(new Title(title, author, first));
            }
        }
        if (titles.size() < 4) titles = deriveTitles(couplets);
        if (titles.size() < 4) return null;
        return new Parsed<PoemData>(o.optInt("version", 1), o.optString("subject", "语文古诗词").trim(),
                new PoemData(couplets, titles));
    }

    private static List<Title> deriveTitles(List<Couplet> couplets) {
        Map<String, Title> map = new LinkedHashMap<String, Title>();
        for (Couplet c : couplets) {
            if (c.title.length() == 0 || c.author.length() == 0) continue;
            if (!map.containsKey(c.title)) map.put(c.title, new Title(c.title, c.author, c.prev));
        }
        return new ArrayList<Title>(map.values());
    }

    static Parsed<List<SciItem>> parseScience(String json) {
        JSONObject o = objectOf(json);
        if (o == null) return null;
        JSONArray arr = o.optJSONArray("items");
        if (arr == null) return null;
        List<SciItem> list = new ArrayList<SciItem>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject item = arr.optJSONObject(i);
            if (item == null) continue;
            String prompt = item.optString("prompt", "").trim();
            String answer = item.optString("answer", "").trim();
            JSONArray dist = item.optJSONArray("distractors");
            if (prompt.length() == 0 || answer.length() == 0 || dist == null) continue;
            List<String> ds = new ArrayList<String>();
            Set<String> seen = new LinkedHashSet<String>();
            seen.add(answer);
            for (int j = 0; j < dist.length(); j++) {
                String d = dist.optString(j, "").trim();
                if (d.length() == 0 || !seen.add(d)) continue;
                ds.add(d);
                if (ds.size() >= 3) break;
            }
            if (ds.size() < 3) continue;
            list.add(new SciItem(prompt, answer, ds.toArray(new String[0])));
        }
        if (list.size() < 4) return null;
        return new Parsed<List<SciItem>>(o.optInt("version", 1), o.optString("subject", "科学常识").trim(), list);
    }

    private static JSONObject objectOf(String json) {
        if (json == null) return null;
        String text = json;
        if (text.startsWith("\uFEFF")) text = text.substring(1);
        text = text.trim();
        if (text.length() == 0 || text.charAt(0) != '{') return null;
        try {
            return new JSONObject(text);
        } catch (Exception e) {
            return null;
        }
    }

    private static String readAsset(Context ctx, String name) {
        InputStream in = null;
        try {
            in = ctx.getAssets().open(name);
            return readUtf8(in, 2_000_000);
        } catch (IOException e) {
            return null;
        } finally {
            close(in);
        }
    }

    private static String readCache(Context ctx, String name) {
        File f = new File(new File(ctx.getFilesDir(), "banks"), name);
        if (!f.isFile() || f.length() <= 0 || f.length() > 2_000_000) return null;
        InputStream in = null;
        try {
            in = new FileInputStream(f);
            return readUtf8(in, 2_000_000);
        } catch (IOException e) {
            return null;
        } finally {
            close(in);
        }
    }

    private static void writeCache(Context ctx, String name, String json) {
        if (ctx == null || json == null) return;
        File dir = new File(ctx.getFilesDir(), "banks");
        if (!dir.exists() && !dir.mkdirs()) return;
        File tmp = new File(dir, name + ".tmp");
        File dest = new File(dir, name);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(tmp);
            out.write(json.getBytes("UTF-8"));
            out.flush();
        } catch (IOException e) {
            tmp.delete();
            return;
        } finally {
            if (out != null) {
                try { out.close(); } catch (IOException ignored) {}
            }
        }
        if (dest.exists() && !dest.delete()) {
            tmp.delete();
            return;
        }
        if (!tmp.renameTo(dest)) tmp.delete();
    }

    static String readUtf8(InputStream in, int maxBytes) throws IOException {
        byte[] buf = new byte[4096];
        int total = 0;
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        int n;
        while ((n = in.read(buf)) >= 0) {
            total += n;
            if (total > maxBytes) throw new IOException("题库文件过大");
            bos.write(buf, 0, n);
        }
        return new String(bos.toByteArray(), "UTF-8");
    }

    private static void close(InputStream in) {
        if (in == null) return;
        try { in.close(); } catch (IOException ignored) {}
    }
}
