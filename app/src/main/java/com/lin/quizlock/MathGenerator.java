package com.lin.quizlock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

/**
 * 小学六年级口算：多位数乘除、带括号四则、小数、分数（通分/约分/带分数）、
 * 百分数、单位换算、估算。答案均为选择题，干扰项贴近常见口算错误。
 */
public final class MathGenerator {
    private MathGenerator() {}

    public static Question next(Random rnd) {
        int kind = rnd.nextInt(10);
        switch (kind) {
            case 0: return multiply(rnd);
            case 1: return divide(rnd);
            case 2: return mixedOps(rnd);
            case 3: return decimal(rnd);
            case 4: return fraction(rnd);
            case 5: return percent(rnd);
            case 6: return unitConvert(rnd);
            case 7: return estimate(rnd);
            case 8: return multiply(rnd); // 乘除多抽一点
            default: return mixedOps(rnd);
        }
    }

    /** 2–3 位数 × 2 位数 */
    private static Question multiply(Random rnd) {
        int a = rnd.nextBoolean()
                ? (12 + rnd.nextInt(88))           // 2 位
                : (101 + rnd.nextInt(899));        // 3 位
        int b = 11 + rnd.nextInt(89);              // 2 位
        // 控制难度：偶发一边略小以便口算
        if (rnd.nextInt(3) == 0) b = 12 + rnd.nextInt(28);
        int ans = a * b;
        String prompt = a + " × " + b + " ＝ ？";
        return mcInt("数学口算", prompt, ans, rnd, Math.max(10, ans / 50), Math.max(50, ans / 20));
    }

    /** 整除：被除数为 2–4 位，除数为 2 位，商为整数 */
    private static Question divide(Random rnd) {
        int divisor = 11 + rnd.nextInt(89);
        int quotient = 3 + rnd.nextInt(90);
        // 偶尔做 3 位数 ÷ 2 位数更易口算
        if (rnd.nextBoolean()) {
            divisor = 12 + rnd.nextInt(38);
            quotient = 11 + rnd.nextInt(80);
        }
        int dividend = divisor * quotient;
        String prompt = dividend + " ÷ " + divisor + " ＝ ？";
        return mcInt("数学口算", prompt, quotient, rnd, 1, Math.max(5, quotient / 5 + 3));
    }

    /** 带括号的混合运算 */
    private static Question mixedOps(Random rnd) {
        int type = rnd.nextInt(4);
        int ans;
        String prompt;
        switch (type) {
            case 0: { // (a + b) × c
                int a = 15 + rnd.nextInt(85);
                int b = 10 + rnd.nextInt(90);
                int c = 3 + rnd.nextInt(12);
                ans = (a + b) * c;
                prompt = "(" + a + " ＋ " + b + ") × " + c + " ＝ ？";
                break;
            }
            case 1: { // a × b − c
                int a = 12 + rnd.nextInt(38);
                int b = 11 + rnd.nextInt(19);
                int c = 20 + rnd.nextInt(180);
                ans = a * b - c;
                if (ans < 0) { c = a * b - (5 + rnd.nextInt(40)); ans = a * b - c; }
                prompt = a + " × " + b + " － " + c + " ＝ ？";
                break;
            }
            case 2: { // a × (b − c)
                int b = 30 + rnd.nextInt(70);
                int c = 5 + rnd.nextInt(Math.min(25, b - 2));
                int a = 4 + rnd.nextInt(16);
                ans = a * (b - c);
                prompt = a + " × (" + b + " － " + c + ") ＝ ？";
                break;
            }
            default: { // a ÷ b + c × d，整除
                int b = 2 + rnd.nextInt(9);
                int q = 4 + rnd.nextInt(20);
                int a = b * q;
                int c = 3 + rnd.nextInt(12);
                int d = 2 + rnd.nextInt(9);
                ans = q + c * d;
                prompt = a + " ÷ " + b + " ＋ " + c + " × " + d + " ＝ ？";
                break;
            }
        }
        return mcInt("数学口算", prompt, ans, rnd, 1, Math.max(8, Math.abs(ans) / 10 + 2));
    }

    /** 小数加减乘（1–2 位小数） */
    private static Question decimal(Random rnd) {
        int kind = rnd.nextInt(3);
        if (kind == 0) { // 加减，两位小数
            int a100 = 105 + rnd.nextInt(8900); // 1.05 ~ 90.04
            int b100 = 105 + rnd.nextInt(8900);
            boolean add = rnd.nextBoolean();
            if (!add && b100 > a100) { int t = a100; a100 = b100; b100 = t; }
            int ans100 = add ? a100 + b100 : a100 - b100;
            double a = a100 / 100.0;
            double b = b100 / 100.0;
            double ans = ans100 / 100.0;
            String prompt = String.format(Locale.CHINA, "%.2f %s %.2f ＝ ？", a, add ? "＋" : "－", b);
            return mcDecimal("数学口算", prompt, ans, 2, rnd);
        } else if (kind == 1) { // 一位小数加减
            int a10 = 15 + rnd.nextInt(485);
            int b10 = 15 + rnd.nextInt(485);
            boolean add = rnd.nextBoolean();
            if (!add && b10 > a10) { int t = a10; a10 = b10; b10 = t; }
            double a = a10 / 10.0;
            double b = b10 / 10.0;
            double ans = (add ? a10 + b10 : a10 - b10) / 10.0;
            String prompt = String.format(Locale.CHINA, "%.1f %s %.1f ＝ ？", a, add ? "＋" : "－", b);
            return mcDecimal("数学口算", prompt, ans, 1, rnd);
        } else { // 小数乘法：一位 × 整数 或 一位 × 一位
            if (rnd.nextBoolean()) {
                int a10 = 12 + rnd.nextInt(88); // 1.2 ~ 9.9
                int b = 3 + rnd.nextInt(18);
                double a = a10 / 10.0;
                double ans = (a10 * b) / 10.0;
                String prompt = String.format(Locale.CHINA, "%.1f × %d ＝ ？", a, b);
                return mcDecimal("数学口算", prompt, ans, 1, rnd);
            } else {
                int a10 = 11 + rnd.nextInt(89);
                int b10 = 11 + rnd.nextInt(89);
                double a = a10 / 10.0;
                double b = b10 / 10.0;
                double ans = (a10 * b10) / 100.0;
                String prompt = String.format(Locale.CHINA, "%.1f × %.1f ＝ ？", a, b);
                return mcDecimal("数学口算", prompt, ans, 2, rnd);
            }
        }
    }

    /** 分数：同分母/异分母加减、乘除、约分、带分数 */
    private static Question fraction(Random rnd) {
        int kind = rnd.nextInt(5);
        switch (kind) {
            case 0: return fracSameDenom(rnd);
            case 1: return fracUnlikeAddSub(rnd);
            case 2: return fracMul(rnd);
            case 3: return fracDiv(rnd);
            default: return fracMixedNumber(rnd);
        }
    }

    private static Question fracSameDenom(Random rnd) {
        int d = 3 + rnd.nextInt(10);
        int a = 1 + rnd.nextInt(d - 1);
        int b = 1 + rnd.nextInt(d - 1);
        boolean add = rnd.nextBoolean();
        if (!add && b > a) { int t = a; a = b; b = t; }
        int num = add ? a + b : a - b;
        String correct = simplifyFrac(num, d);
        String prompt = a + "/" + d + (add ? " ＋ " : " － ") + b + "/" + d + " ＝ ？";
        return mcFrac(prompt, correct, num, d, rnd);
    }

    /** 异分母加减（通分），分母较小便于口算 */
    private static Question fracUnlikeAddSub(Random rnd) {
        int[] dens = {2, 3, 4, 5, 6, 8, 10, 12};
        int d1 = dens[rnd.nextInt(dens.length)];
        int d2 = dens[rnd.nextInt(dens.length)];
        while (d2 == d1) d2 = dens[rnd.nextInt(dens.length)];
        int a = 1 + rnd.nextInt(d1 - 1);
        int b = 1 + rnd.nextInt(d2 - 1);
        boolean add = rnd.nextBoolean();
        int l = lcm(d1, d2);
        int n1 = a * (l / d1);
        int n2 = b * (l / d2);
        if (!add && n2 > n1) {
            // swap fractions
            int ta = a, td = d1; a = b; d1 = d2; b = ta; d2 = td;
            n1 = a * (l / d1); n2 = b * (l / d2);
            // recompute l in case — actually l unchanged since same pair
            l = lcm(d1, d2);
            n1 = a * (l / d1);
            n2 = b * (l / d2);
        }
        int num = add ? n1 + n2 : n1 - n2;
        String correct = simplifyFrac(num, l);
        String prompt = a + "/" + d1 + (add ? " ＋ " : " － ") + b + "/" + d2 + " ＝ ？";
        return mcFrac(prompt, correct, num, l, rnd);
    }

    private static Question fracMul(Random rnd) {
        int d1 = 2 + rnd.nextInt(8);
        int d2 = 2 + rnd.nextInt(8);
        int a = 1 + rnd.nextInt(d1);
        int b = 1 + rnd.nextInt(d2);
        // 约分后更友好：偶尔让 a、d2 有公因子
        String correct = simplifyFrac(a * b, d1 * d2);
        String prompt = a + "/" + d1 + " × " + b + "/" + d2 + " ＝ ？";
        return mcFrac(prompt, correct, a * b, d1 * d2, rnd);
    }

    private static Question fracDiv(Random rnd) {
        int d1 = 2 + rnd.nextInt(8);
        int d2 = 2 + rnd.nextInt(8);
        int a = 1 + rnd.nextInt(d1);
        int b = 1 + rnd.nextInt(d2);
        // a/d1 ÷ b/d2 = a/d1 × d2/b
        String correct = simplifyFrac(a * d2, d1 * b);
        String prompt = a + "/" + d1 + " ÷ " + b + "/" + d2 + " ＝ ？";
        return mcFrac(prompt, correct, a * d2, d1 * b, rnd);
    }

    /** 带分数加减：如 2又1/3 ＋ 1又1/2 */
    private static Question fracMixedNumber(Random rnd) {
        int w1 = 1 + rnd.nextInt(4);
        int w2 = 1 + rnd.nextInt(3);
        int d1 = 2 + rnd.nextInt(5);
        int d2 = 2 + rnd.nextInt(5);
        int f1 = 1 + rnd.nextInt(d1 - 1);
        int f2 = 1 + rnd.nextInt(d2 - 1);
        boolean add = rnd.nextBoolean();
        int n1 = w1 * d1 + f1;
        int n2 = w2 * d2 + f2;
        int l = lcm(d1, d2);
        int t1 = n1 * (l / d1);
        int t2 = n2 * (l / d2);
        if (!add && t2 > t1) {
            int tw = w1, td = d1, tf = f1;
            w1 = w2; d1 = d2; f1 = f2;
            w2 = tw; d2 = td; f2 = tf;
            n1 = w1 * d1 + f1;
            n2 = w2 * d2 + f2;
            l = lcm(d1, d2);
            t1 = n1 * (l / d1);
            t2 = n2 * (l / d2);
        }
        int num = add ? t1 + t2 : t1 - t2;
        String correct = simplifyToMixed(num, l);
        String prompt = mixedStr(w1, f1, d1) + (add ? " ＋ " : " － ") + mixedStr(w2, f2, d2) + " ＝ ？";
        List<String> opts = new LinkedHashSetList();
        opts.add(correct);
        opts.add(simplifyFrac(num, l)); // improper form if different
        opts.add(simplifyToMixed(num + l / Math.max(1, gcd(num == 0 ? 1 : Math.abs(num), l)), l));
        opts.add(simplifyToMixed(Math.max(0, num - 1), l));
        opts.add(String.valueOf(w1 + (add ? w2 : -w2)));
        opts.add(mixedStr(w1 + w2, f1 + f2, d1)); // 错把分数部分直接加
        return finishStringMc("数学口算", prompt, correct, opts, rnd);
    }

    private static String mixedStr(int w, int f, int d) {
        return w + "又" + f + "/" + d;
    }

    /** 百分数：求一个数的百分之几、百分数与小数互化等 */
    private static Question percent(Random rnd) {
        int kind = rnd.nextInt(3);
        if (kind == 0) {
            // 求 a 的 p%
            int[] percents = {5, 10, 15, 20, 25, 30, 40, 50, 60, 75, 80};
            int p = percents[rnd.nextInt(percents.length)];
            int a = 20 + rnd.nextInt(181); // 保证常见口算：尽量整
            // 调整 a 使结果为整数
            if ((a * p) % 100 != 0) {
                a = ((100 / gcd(p, 100)) * (1 + rnd.nextInt(8)));
                if (a < 20) a *= (2 + rnd.nextInt(4));
            }
            int ans = a * p / 100;
            String prompt = a + " 的 " + p + "% 是多少？";
            return mcInt("数学口算", prompt, ans, rnd, 1, Math.max(3, ans / 5 + 2));
        } else if (kind == 1) {
            // 百分数 → 小数
            int[] percents = {5, 8, 12, 15, 25, 36, 45, 60, 75, 80, 125};
            int p = percents[rnd.nextInt(percents.length)];
            double ans = p / 100.0;
            String prompt = p + "% 化成小数是？";
            String correct = trimDecimal(ans);
            List<String> opts = new LinkedHashSetList();
            opts.add(correct);
            opts.add(trimDecimal(p / 10.0));
            opts.add(trimDecimal(p / 1000.0));
            opts.add(String.valueOf(p));
            opts.add(trimDecimal(ans + 0.1));
            return finishStringMc("数学口算", prompt, correct, opts, rnd);
        } else {
            // 小数 → 百分数
            int[] tenths = {5, 8, 12, 15, 25, 36, 45, 60, 75, 80, 125};
            int t = tenths[rnd.nextInt(tenths.length)]; // 实际是百分数分子
            double dec = t / 100.0;
            String prompt = trimDecimal(dec) + " 化成百分数是？";
            String correct = t + "%";
            List<String> opts = new LinkedHashSetList();
            opts.add(correct);
            opts.add((t * 10) + "%");
            opts.add((t / 10 > 0 ? t / 10 : t + 1) + "%");
            opts.add((t + 5) + "%");
            opts.add(trimDecimal(dec) + "%");
            return finishStringMc("数学口算", prompt, correct, opts, rnd);
        }
    }

    /** 单位换算：面积、体积、速度常见口算 */
    private static Question unitConvert(Random rnd) {
        int kind = rnd.nextInt(6);
        switch (kind) {
            case 0: { // km² → m²
                int km = 1 + rnd.nextInt(8);
                long ans = km * 1_000_000L;
                String prompt = km + " 平方千米 ＝ ？平方米";
                return mcLong("数学口算", prompt, ans, rnd);
            }
            case 1: { // m² → dm² 或 cm²
                int m = 2 + rnd.nextInt(20);
                if (rnd.nextBoolean()) {
                    int ans = m * 100;
                    String prompt = m + " 平方米 ＝ ？平方分米";
                    return mcInt("数学口算", prompt, ans, rnd, 10, 50);
                } else {
                    int ans = m * 10000;
                    String prompt = m + " 平方米 ＝ ？平方厘米";
                    return mcInt("数学口算", prompt, ans, rnd, 100, 2000);
                }
            }
            case 2: { // L ↔ mL 或 m³ ↔ L
                if (rnd.nextBoolean()) {
                    int L = 2 + rnd.nextInt(18);
                    int ans = L * 1000;
                    String prompt = L + " 升 ＝ ？毫升";
                    return mcInt("数学口算", prompt, ans, rnd, 100, 500);
                } else {
                    int m3 = 1 + rnd.nextInt(6);
                    int ans = m3 * 1000;
                    String prompt = m3 + " 立方米 ＝ ？升";
                    return mcInt("数学口算", prompt, ans, rnd, 100, 500);
                }
            }
            case 3: { // 速度：路程 = 速度 × 时间
                int v = 40 + rnd.nextInt(61); // km/h
                int t = 2 + rnd.nextInt(5);   // h
                int ans = v * t;
                String prompt = "一辆车以每小时 " + v + " 千米行驶 " + t + " 小时，路程是多少千米？";
                return mcInt("数学口算", prompt, ans, rnd, 5, 40);
            }
            case 4: { // 时间：求速度
                int t = 2 + rnd.nextInt(4);
                int v = 35 + rnd.nextInt(50);
                int s = v * t;
                String prompt = "行驶 " + s + " 千米用了 " + t + " 小时，平均速度是每小时多少千米？";
                return mcInt("数学口算", prompt, v, rnd, 2, 15);
            }
            default: { // ha → m²
                int ha = 1 + rnd.nextInt(12);
                int ans = ha * 10000;
                String prompt = ha + " 公顷 ＝ ？平方米";
                return mcInt("数学口算", prompt, ans, rnd, 100, 2000);
            }
        }
    }

    /** 估算：选最接近的结果 */
    private static Question estimate(Random rnd) {
        int a = 48 + rnd.nextInt(50); // ~50–97
        int b = 19 + rnd.nextInt(30); // ~19–48
        int exact = a * b;
        // 正确估算目标：四舍五入到整十再乘
        int ra = ((a + 5) / 10) * 10;
        int rb = ((b + 5) / 10) * 10;
        int est = ra * rb;
        String prompt = "估算 " + a + " × " + b + "，最接近的是？";
        // 选项：估算值、精确值附近、错误进位
        List<Integer> vals = new ArrayList<>();
        vals.add(est);
        int wrong1 = (a / 10 * 10) * (b / 10 * 10); // 截断
        int wrong2 = ((a + 9) / 10 * 10) * ((b + 9) / 10 * 10);
        int wrong3 = exact;
        for (int w : new int[]{wrong1, wrong2, wrong3, est + 100, est - 100, ra * b, a * rb}) {
            if (w > 0 && w != est && !vals.contains(w)) vals.add(w);
            if (vals.size() >= 4) break;
        }
        while (vals.size() < 4) {
            int fake = est + (rnd.nextInt(9) + 1) * 10 * (rnd.nextBoolean() ? 1 : -1);
            if (fake > 0 && !vals.contains(fake)) vals.add(fake);
        }
        List<String> opts = new ArrayList<>();
        for (int i = 0; i < 4; i++) opts.add(String.valueOf(vals.get(i)));
        Collections.shuffle(opts, rnd);
        int idx = opts.indexOf(String.valueOf(est));
        if (idx < 0) { opts.set(0, String.valueOf(est)); idx = 0; }
        return new Question("数学口算", prompt, opts.toArray(new String[0]), idx);
    }

    // ——— helpers ———

    private static int gcd(int a, int b) {
        a = Math.abs(a); b = Math.abs(b);
        while (b != 0) { int t = a % b; a = b; b = t; }
        return a == 0 ? 1 : a;
    }

    private static int lcm(int a, int b) {
        return a / gcd(a, b) * b;
    }

    private static String simplifyFrac(int num, int den) {
        if (den < 0) { num = -num; den = -den; }
        if (num == 0) return "0";
        int g = gcd(num, den);
        num /= g; den /= g;
        if (den == 1) return String.valueOf(num);
        if (num > den) {
            // keep improper as simplified improper OR mixed — use improper for 乘除结果一致性，
            // but also accept mixed in options separately; here return mixed-friendly improper simplified
            return num + "/" + den;
        }
        return num + "/" + den;
    }

    private static String simplifyToMixed(int num, int den) {
        if (den <= 0) den = 1;
        if (num == 0) return "0";
        int g = gcd(num, den);
        num /= g; den /= g;
        if (den == 1) return String.valueOf(num);
        if (Math.abs(num) < den) return num + "/" + den;
        int w = num / den;
        int r = Math.abs(num % den);
        if (r == 0) return String.valueOf(w);
        return w + "又" + r + "/" + den;
    }

    private static String trimDecimal(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-9) {
            return String.valueOf((long) Math.rint(v));
        }
        String s = String.format(Locale.CHINA, "%.4f", v);
        while (s.contains(".") && (s.endsWith("0") || s.endsWith("."))) {
            if (s.endsWith(".")) { s = s.substring(0, s.length() - 1); break; }
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    private static Question mcInt(String subject, String prompt, int ans, Random rnd, int minDelta, int maxDelta) {
        Set<Integer> vals = new LinkedHashSet<>();
        vals.add(ans);
        // 常见错误：差 1、进位错、差一个数量级的一部分
        int[] hints = {
                ans + 1, ans - 1, ans + 10, ans - 10,
                ans + minDelta, ans - minDelta,
                (int) Math.round(ans * 1.1), (int) Math.round(ans * 0.9)
        };
        for (int h : hints) {
            if (h != ans && h >= 0) vals.add(h);
            if (vals.size() >= 4) break;
        }
        int guard = 0;
        while (vals.size() < 4 && guard++ < 40) {
            int span = Math.max(1, maxDelta);
            int delta = (rnd.nextInt(span) + minDelta) * (rnd.nextBoolean() ? 1 : -1);
            int fake = ans + delta;
            if (fake != ans && fake >= 0) vals.add(fake);
        }
        List<String> opts = new ArrayList<>();
        for (int v : vals) {
            opts.add(String.valueOf(v));
            if (opts.size() == 4) break;
        }
        Collections.shuffle(opts, rnd);
        int idx = opts.indexOf(String.valueOf(ans));
        if (idx < 0) { opts.set(0, String.valueOf(ans)); idx = 0; }
        return new Question(subject, prompt, opts.toArray(new String[0]), idx);
    }

    private static Question mcLong(String subject, String prompt, long ans, Random rnd) {
        Set<Long> vals = new LinkedHashSet<>();
        vals.add(ans);
        vals.add(ans / 10);
        vals.add(ans * 10);
        vals.add(ans / 100);
        vals.add(ans * 100);
        vals.add(ans + 1000);
        List<String> opts = new ArrayList<>();
        for (long v : vals) {
            if (v > 0) opts.add(String.valueOf(v));
            if (opts.size() == 4) break;
        }
        while (opts.size() < 4) {
            long fake = ans + (rnd.nextInt(9) + 1) * 1000L;
            String s = String.valueOf(fake);
            if (!opts.contains(s)) opts.add(s);
        }
        Collections.shuffle(opts, rnd);
        int idx = opts.indexOf(String.valueOf(ans));
        if (idx < 0) { opts.set(0, String.valueOf(ans)); idx = 0; }
        return new Question(subject, prompt, opts.toArray(new String[0]), idx);
    }

    private static Question mcDecimal(String subject, String prompt, double ans, int places, Random rnd) {
        String fmt = "%." + places + "f";
        String correct = String.format(Locale.CHINA, fmt, ans);
        // normalize -0
        if (correct.startsWith("-0.") && ans == 0) correct = String.format(Locale.CHINA, fmt, 0.0);
        List<String> opts = new LinkedHashSetList();
        opts.add(correct);
        double step = places == 1 ? 0.1 : 0.01;
        double[] deltas = {step, -step, 10 * step, -10 * step, 0.5, -0.5, 1.0, -1.0};
        for (double d : deltas) {
            double fake = ans + d;
            if (fake < 0) fake = Math.abs(fake);
            String s = String.format(Locale.CHINA, fmt, Math.round(fake / step) * step);
            if (!opts.contains(s)) opts.add(s);
            if (opts.size() >= 4) break;
        }
        int guard = 0;
        while (opts.size() < 4 && guard++ < 20) {
            double fake = ans + (rnd.nextInt(9) + 1) * step * (rnd.nextBoolean() ? 1 : -1);
            if (fake < 0) fake = Math.abs(fake);
            String s = String.format(Locale.CHINA, fmt, Math.round(fake / step) * step);
            if (!opts.contains(s)) opts.add(s);
        }
        return finishStringMc(subject, prompt, correct, opts, rnd);
    }

    private static Question mcFrac(String prompt, String correct, int rawNum, int rawDen, Random rnd) {
        List<String> opts = new LinkedHashSetList();
        opts.add(correct);
        // 未约分
        if (rawDen != 0) {
            String unsimp = rawNum + "/" + rawDen;
            if (!unsimp.equals(correct)) opts.add(unsimp);
        }
        // 分子分母直接加减的错误
        opts.add(simplifyFrac(rawNum + 1, rawDen));
        opts.add(simplifyFrac(Math.max(0, rawNum - 1), rawDen));
        opts.add(simplifyFrac(rawNum, rawDen + 1));
        opts.add(String.valueOf(rawNum)); // 丢掉分母
        opts.add(simplifyToMixed(rawNum, rawDen));
        return finishStringMc("数学口算", prompt, correct, opts, rnd);
    }

    private static Question finishStringMc(String subject, String prompt, String correct,
                                          List<String> candidates, Random rnd) {
        List<String> unique = new ArrayList<>();
        for (String o : candidates) {
            if (o == null) continue;
            if (!unique.contains(o)) unique.add(o);
        }
        if (!unique.contains(correct)) unique.add(0, correct);
        int guard = 0;
        while (unique.size() < 4 && guard++ < 30) {
            String fake = (1 + rnd.nextInt(12)) + "/" + (2 + rnd.nextInt(12));
            if (!unique.contains(fake)) unique.add(fake);
        }
        // ensure correct is among first build then take 4 including correct
        List<String> opts = new ArrayList<>();
        opts.add(correct);
        for (String o : unique) {
            if (!o.equals(correct)) opts.add(o);
            if (opts.size() == 4) break;
        }
        while (opts.size() < 4) opts.add(String.valueOf(rnd.nextInt(50)));
        Collections.shuffle(opts, rnd);
        int idx = opts.indexOf(correct);
        if (idx < 0) { opts.set(0, correct); idx = 0; }
        return new Question(subject, prompt, opts.toArray(new String[0]), idx);
    }

    /** Tiny list that ignores duplicate adds like LinkedHashSet but stays a List. */
    private static final class LinkedHashSetList extends ArrayList<String> {
        @Override
        public boolean add(String s) {
            if (s == null || contains(s)) return false;
            return super.add(s);
        }
    }
}
