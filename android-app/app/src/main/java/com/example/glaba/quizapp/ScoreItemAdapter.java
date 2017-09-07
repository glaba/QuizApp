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

public class ScoreItemAdapter extends ArrayAdapter<Score> {
    List<Score> objects;

    public ScoreItemAdapter(Context context, int resource, List<Score> objects) {
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
        score.setText(Integer.toString(objects.get(position).getScore()));

        return v;
    }
}