package com.lin.quizlock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/** 小学高年级口算：多位数加减、乘除、简单分数/小数 */
public final class MathGenerator {
    private MathGenerator() {}

    public static Question next(Random rnd) {
        int kind = rnd.nextInt(6);
        switch (kind) {
            case 0: return addSub(rnd, false);
            case 1: return addSub(rnd, true);
            case 2: return multiply(rnd);
            case 3: return divide(rnd);
            case 4: return decimal(rnd);
            default: return fraction(rnd);
        }
    }

    private static Question addSub(Random rnd, boolean sub) {
        int a = 100 + rnd.nextInt(900);
        int b = 10 + rnd.nextInt(900);
        if (sub && b > a) { int t = a; a = b; b = t; }
        int ans = sub ? a - b : a + b;
        String prompt = a + (sub ? " － " : " ＋ ") + b + " ＝ ？";
        return mc("数学口算", prompt, ans, rnd);
    }

    private static Question multiply(Random rnd) {
        int a = 11 + rnd.nextInt(89);
        int b = 2 + rnd.nextInt(18);
        int ans = a * b;
        String prompt = a + " × " + b + " ＝ ？";
        return mc("数学口算", prompt, ans, rnd);
    }

    private static Question divide(Random rnd) {
        int b = 2 + rnd.nextInt(12);
        int q = 3 + rnd.nextInt(40);
        int a = b * q;
        String prompt = a + " ÷ " + b + " ＝ ？";
        return mc("数学口算", prompt, q, rnd);
    }

    private static Question decimal(Random rnd) {
        int a10 = 10 + rnd.nextInt(90); // 1.0 ~ 9.9 as tenths*10 sometimes larger
        int b10 = 10 + rnd.nextInt(90);
        boolean add = rnd.nextBoolean();
        double a = a10 / 10.0;
        double b = b10 / 10.0;
        if (!add && b > a) { double t = a; a = b; b = t; }
        double ans = add ? a + b : a - b;
        // keep one decimal
        ans = Math.round(ans * 10.0) / 10.0;
        String prompt = String.format(Locale.CHINA, "%.1f %s %.1f ＝ ？", a, add ? "＋" : "－", b);
        return mcDouble("数学口算", prompt, ans, rnd);
    }

    private static Question fraction(Random rnd) {
        // 同分母加减：a/d ± b/d
        int d = 2 + rnd.nextInt(8);
        int a = 1 + rnd.nextInt(d - 1);
        int b = 1 + rnd.nextInt(d - 1);
        boolean add = rnd.nextBoolean();
        if (!add && b > a) { int t = a; a = b; b = t; }
        int num = add ? a + b : a - b;
        // simplify
        int g = gcd(Math.abs(num), d);
        int sn = num / g;
        int sd = d / g;
        String correct = (sd == 1) ? String.valueOf(sn) : (sn + "/" + sd);
        String prompt = a + "/" + d + (add ? " ＋ " : " － ") + b + "/" + d + " ＝ ？";

        List<String> opts = new ArrayList<>();
        opts.add(correct);
        opts.add(num + "/" + d); // unsimplified if different
        if (opts.get(1).equals(correct)) opts.set(1, (num + 1) + "/" + d);
        opts.add((a + b) + "/" + (d + d));
        opts.add(String.valueOf(a + (add ? b : -b)));
        // ensure unique
        while (opts.size() < 4) opts.add(String.valueOf(rnd.nextInt(20)));
        // dedupe
        List<String> unique = new ArrayList<>();
        for (String o : opts) if (!unique.contains(o)) unique.add(o);
        while (unique.size() < 4) {
            String fake = (1 + rnd.nextInt(9)) + "/" + (2 + rnd.nextInt(9));
            if (!unique.contains(fake)) unique.add(fake);
        }
        unique = new ArrayList<>(unique.subList(0, 4));
        int correctIdx = unique.indexOf(correct);
        if (correctIdx < 0) { unique.set(0, correct); correctIdx = 0; }
        Collections.swap(unique, 0, correctIdx);
        Collections.shuffle(unique, rnd);
        int idx = unique.indexOf(correct);
        return new Question("数学口算", prompt, unique.toArray(new String[0]), idx);
    }

    private static int gcd(int a, int b) {
        while (b != 0) { int t = a % b; a = b; b = t; }
        return a == 0 ? 1 : a;
    }

    private static Question mc(String subject, String prompt, int ans, Random rnd) {
        List<Integer> vals = new ArrayList<>();
        vals.add(ans);
        while (vals.size() < 4) {
            int delta = (rnd.nextInt(15) + 1) * (rnd.nextBoolean() ? 1 : -1);
            int fake = ans + delta;
            if (fake != ans && !vals.contains(fake) && fake >= 0) vals.add(fake);
        }
        List<String> opts = new ArrayList<>();
        for (int v : vals) opts.add(String.valueOf(v));
        Collections.shuffle(opts, rnd);
        int idx = opts.indexOf(String.valueOf(ans));
        return new Question(subject, prompt, opts.toArray(new String[0]), idx);
    }

    private static Question mcDouble(String subject, String prompt, double ans, Random rnd) {
        String correct = String.format(Locale.CHINA, "%.1f", ans);
        List<String> opts = new ArrayList<>();
        opts.add(correct);
        while (opts.size() < 4) {
            double fake = ans + (rnd.nextInt(9) + 1) * 0.1 * (rnd.nextBoolean() ? 1 : -1);
            fake = Math.round(fake * 10.0) / 10.0;
            if (fake < 0) fake = Math.abs(fake);
            String s = String.format(Locale.CHINA, "%.1f", fake);
            if (!opts.contains(s)) opts.add(s);
        }
        Collections.shuffle(opts, rnd);
        return new Question(subject, prompt, opts.toArray(new String[0]), opts.indexOf(correct));
    }
}
