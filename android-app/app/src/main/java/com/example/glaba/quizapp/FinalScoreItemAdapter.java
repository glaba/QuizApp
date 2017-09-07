package com.example.glaba.quizapp;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class FinalScoreItemAdapter extends ArrayAdapter<Score> {
    List<Score> objects;

    public FinalScoreItemAdapter(Context context, int resource, List<Score> objects) {
        super(context, resource, objects);
        this.objects = objects;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            v = inflater.inflate(R.layout.live_update_list_item, null);
        }

        TextView user = (TextView)v.findViewById(R.id.userTextView);
        TextView score = (TextView)v.findViewById(R.id.timeTextView);

        user.setText(objects.get(position).getName());
        score.setText("" + objects.get(position).getScore());

        // Find how high this is among the others
        int numScoresHigher = 0;
        int myScore = objects.get(position).getScore();
        for (int i = 0; i < objects.size(); i++) {
            if (objects.get(i).getScore() > myScore)
                numScoresHigher++;
        }

        int backColor;
        if (numScoresHigher == 0) {
            // 1st place
            backColor = Color.rgb(201, 137, 16);
        } else if (numScoresHigher == 1) {
            // 2nd place
            backColor = Color.rgb(168, 168, 168);
        } else if (numScoresHigher == 2) {
            // 3rd place
            backColor = Color.rgb(150, 90, 56);
        } else {
            backColor = Color.rgb(0, 0, 0);
        }
        v.setBackgroundColor(backColor);

        return v;
    }
}