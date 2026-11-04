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
            // 权重：数学 ~45%，英语/古诗/科学各 ~18%
            int roll = RND.nextInt(100);
            if (roll < 45) {
                q = MathGenerator.next(RND);
            } else if (roll < 63) {
                q = EnglishBank.next(RND);
            } else if (roll < 81) {
                q = PoemBank.next(RND);
            } else {
                q = ScienceBank.next(RND);
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
            String first = RECENT.iterator().next();
            RECENT.remove(first);
        }
    }
}
