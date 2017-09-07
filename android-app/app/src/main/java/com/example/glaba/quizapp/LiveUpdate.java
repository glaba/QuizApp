package com.example.glaba.quizapp;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class LiveUpdate extends Activity {
    LocalBroadcastManager broadcastManager;
    BroadcastReceiver receiver;

    ArrayList<Response> responses;
    ArrayList<Score> scores;
    LiveUpdateAdapter adapter;
    ScoreItemAdapter scoreAdapter;

    Button nextButton;
    TextView header;
    ListView liveUpdateList;

    public void sendBroadcast(String action, String message) {
        Intent intent = new Intent(action);
        if (message != null)
            intent.putExtra("Message", message);
        broadcastManager.sendBroadcast(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_update);

        nextButton = (Button)findViewById(R.id.liveUpdateNextButton);
        header = (TextView)findViewById(R.id.responseScoreTitle);
        liveUpdateList = (ListView)findViewById(R.id.liveUpdateList);

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBroadcast("OutgoingSocketMessage", "{\"what\":\"next_question\"}");
            }
        });

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
                                liveUpdateList.setAdapter(null);

                                header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                                header.setText(msg.getJSONObject("question").getString("question"));
                                nextButton.setEnabled(false);

                                responses = new ArrayList<>();
                            } else if (msg.getString("what").equals("live_update")) {
                                responses.add(new Response(msg.getInt("time"), msg.getBoolean("correctAnswer"), msg.getString("user"), msg.getString("response")));
                                adapter = new LiveUpdateAdapter(LiveUpdate.this, R.layout.live_update_list_item, responses);
                                liveUpdateList.setAdapter(adapter);
                            } else if (msg.getString("what").equals("scores")) {
                                header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                                header.setText("Scores");
                                nextButton.setEnabled(true);

                                scores = new ArrayList<>();
                                for (int i = 0; i < msg.getJSONArray("scores").length(); i++) {
                                    JSONObject curScore = msg.getJSONArray("scores").getJSONObject(i);
                                    scores.add(new Score(curScore.getString("name"), curScore.getInt("score")));
                                }
                                scoreAdapter = new ScoreItemAdapter(LiveUpdate.this, R.layout.live_update_list_item, scores);
                                liveUpdateList.setAdapter(scoreAdapter);
                            } else if (msg.getString("what").equals("game_over")) {
                                Intent gameOver = new Intent(LiveUpdate.this, FinalScore.class);
                                gameOver.putExtra("information", msg.toString());
                                startActivityForResult(gameOver, GameLobby.GAME_OVER);
                            }
                        } catch (Exception e) {
                        }
                    } catch (Exception e) {
                    }
                }
            }
        };
        broadcastManager.registerReceiver(receiver, broadcastIntentFilter);

        sendBroadcast("OutgoingSocketMessage", "{\"what\":\"game_prepared\"}");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GameLobby.GAME_OVER) {
            if (resultCode == GameLobby.GAME_OVER) {
                LiveUpdate.this.setResult(GameLobby.GAME_OVER, null);
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }

    private class Response {
        int time;
        Boolean correctAnswer;
        String name, answer;

        public int getTime() {
            return time;
        }

        public Boolean isCorrect() {
            return correctAnswer;
        }

        public String getName() {
            return name;
        }

        public String getAnswer() {
            return answer;
        }

        public Response(int time, Boolean correctAnswer, String name, String answer) {
            this.time = time;
            this.correctAnswer = correctAnswer;
            this.name = name;
            this.answer = answer;
        }
    }

    private class LiveUpdateAdapter extends ArrayAdapter<Response> {
        List<Response> objects;

        public LiveUpdateAdapter(Context context, int resource, List<Response> objects) {
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
            TextView time = (TextView)v.findViewById(R.id.timeTextView);

            user.setText(objects.get(position).getName() + ": " + objects.get(position).getAnswer());
            time.setText(String.format("%.2f", objects.get(position).getTime() / 1000.0) + "s");

            if (objects.get(position).isCorrect()) {
                v.setBackgroundColor(Color.rgb(0, 200, 0));
            } else {
                v.setBackgroundColor(Color.rgb(200, 0, 0));
            }

            return v;
        }
    }
}
