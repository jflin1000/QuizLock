package com.lin.quizlock;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public final class QuestionBank {
    private static final Random RND = new Random();
    private static final Set<String> RECENT = new HashSet<>();
    private static final int RECENT_LIMIT = 40;

    private QuestionBank() {}

    public static Question next() {
        Question q = null;
        for (int attempt = 0; attempt < 12; attempt++) {
            int cat = RND.nextInt(4);
            switch (cat) {
                case 0: q = MathGenerator.next(RND); break;
                case 1: q = EnglishBank.next(RND); break;
                case 2: q = PoemBank.next(RND); break;
                default: q = ScienceBank.next(RND); break;
            }
            String key = q.subject + "|" + q.prompt;
            if (!RECENT.contains(key)) {
                remember(key);
                return q;
            }
        }
        if (q != null) remember(q.subject + "|" + q.prompt);
        return q != null ? q : MathGenerator.next(RND);
    }

    private static void remember(String key) {
        RECENT.add(key);
        if (RECENT.size() > RECENT_LIMIT) {
            // drop arbitrary one
            String first = RECENT.iterator().next();
            RECENT.remove(first);
        }
    }
}
