package com.example.glaba.quizapp;


import java.io.Serializable;

public class Score implements Serializable {
    String name;
    int score;

    public String getName() {
        return name;
    }

    public int getScore() {
        return score;
    }

    public Score(String name, int score) {
        this.name = name;
        this.score = score;
    }
}
