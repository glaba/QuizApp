package com.example.glaba.quizapp;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class IntermediateScoreScreen extends Activity {
    ListView scoreListView;
    Button continueButton;
    LocalBroadcastManager broadcastManager;
    BroadcastReceiver receiver;

    public void sendBroadcast(String action, String message) {
        Intent intent = new Intent(action);
        if (message != null)
            intent.putExtra("Message", message);
        broadcastManager.sendBroadcast(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intermediate_score_screen);

        IntentFilter broadcastIntentFilter = new IntentFilter();
        for (String filter : SocketService.ACTIONS) {
            broadcastIntentFilter.addAction(filter);
        }
        broadcastManager = LocalBroadcastManager.getInstance(this);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("IncomingSocketMessage")) {
                    String message = intent.getStringExtra("Message");
                    try {
                        final JSONObject msg = new JSONObject(message);
                        try {
                            if (msg.getString("what").equals("question")) {
                                finish();
                            }
                        } catch (Exception e) {
                        }
                    } catch (Exception e) {
                    }
                }
            }
        };
        broadcastManager.registerReceiver(receiver, broadcastIntentFilter);

        scoreListView = (ListView)findViewById(R.id.scoresListView);
        continueButton = (Button)findViewById(R.id.continueButton);

        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBroadcast("OutgoingSocketMessage", "{\"what\":\"next_question\"}");
            }
        });

        Intent intent = getIntent();
        JSONArray scores;
        try {
            scores = new JSONArray(intent.getStringExtra("score"));
            ArrayList<Score> scoreObjects = new ArrayList<>();
            for (int i = 0; i < scores.length(); i++) {
                scoreObjects.add(new Score(scores.getJSONObject(i).getString("name"), scores.getJSONObject(i).getInt("score")));
            }
            ScoreItemAdapter adapter = new ScoreItemAdapter(IntermediateScoreScreen.this, R.layout.live_update_list_item, scoreObjects);
            scoreListView.setAdapter(adapter);
        } catch (Exception e) {
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }
}
