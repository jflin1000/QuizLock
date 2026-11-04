package com.lin.quizlock;

public class Question {
    public final String subject;
    public final String prompt;
    public final String[] options; // length 4
    public final int correctIndex; // 0-3 after shuffle

    public Question(String subject, String prompt, String[] options, int correctIndex) {
        this.subject = subject;
        this.prompt = prompt;
        this.options = options;
        this.correctIndex = correctIndex;
    }
}
