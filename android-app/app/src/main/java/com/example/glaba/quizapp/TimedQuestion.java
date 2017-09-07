package com.example.glaba.quizapp;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.json.JSONObject;

public class TimedQuestion extends Activity implements Button.OnClickListener {
    LocalBroadcastManager broadcastManager;
    BroadcastReceiver receiver;
    TextView question;
    Button buttonA, buttonB, buttonC, buttonD;
    Long previousTime;
    ProgressBar timeLeftBar;
    Boolean answerSent = true;

    Boolean intermediateScoreScreenOpen = false;

    Runnable progressBarUpdaterRunnable;
    int timerProgress;

    public void sendBroadcast(String action, String message) {
        Intent intent = new Intent(action);
        if (message != null)
            intent.putExtra("Message", message);
        broadcastManager.sendBroadcast(intent);
    }

    @Override
    public void onClick(View v) {
        // Button onclick listener
        if (!answerSent) {
            answerSent = true;
            timerProgress = 0;
            JSONObject message = new JSONObject();
            try {
                message.put("what", "answer");
                message.put("response", ((Button) v).getText());
                message.put("timeToAnswer", System.currentTimeMillis() - previousTime);
            } catch (Exception e) {

            }
            sendBroadcast("OutgoingSocketMessage", message.toString());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timed_question);

        Display display = getWindowManager().getDefaultDisplay();
        Point s = new Point();
        display.getSize(s);
        int width = s.x;

        question = (TextView)findViewById(R.id.questionText);
        buttonA = (Button)findViewById(R.id.buttonA);
        buttonB = (Button)findViewById(R.id.buttonB);
        buttonC = (Button)findViewById(R.id.buttonC);
        buttonD = (Button)findViewById(R.id.buttonD);
        timeLeftBar = (ProgressBar)findViewById(R.id.timeRemainingBar);

        final Button[] buttons = {buttonA, buttonB, buttonC, buttonD};
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setOnClickListener(this);
            buttons[i].setWidth(width / 2);
        }

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
                                answerSent = false;

                                previousTime = System.currentTimeMillis();
                                question.setText(msg.getJSONObject("question").getString("question"));
                                for (int i = 0; i < buttons.length; i++) {
                                    buttons[i].setText(msg.getJSONObject("question").getJSONArray("options").getString(i));
                                }

                                timeLeftBar.setMax(msg.getJSONObject("question").getInt("time") / 50);
                                timeLeftBar.setProgress(msg.getJSONObject("question").getInt("time") / 50);
                                timerProgress = timeLeftBar.getProgress();

                                final Handler progressBarUpdater = new Handler();
                                progressBarUpdaterRunnable = new Runnable() {
                                    @Override
                                    public void run() {
                                        if (timerProgress <= 0) {
                                            if (!answerSent) {
                                                answerSent = true;
                                                JSONObject message = new JSONObject();
                                                try {
                                                    message.put("what", "answer");
                                                    message.put("response", "");
                                                    message.put("timeToAnswer", System.currentTimeMillis() - previousTime);
                                                } catch (Exception e) {

                                                }
                                                sendBroadcast("OutgoingSocketMessage", message.toString());
                                            }
                                        } else {
                                            timeLeftBar.setProgress(--timerProgress);
                                            progressBarUpdater.postDelayed(progressBarUpdaterRunnable, 50);
                                        }
                                    }
                                };
                                progressBarUpdater.postDelayed(progressBarUpdaterRunnable, 50);
                            } else if (msg.getString("what").equals("scores")) {
                                timerProgress = 0;

                                Intent scoreIntent = new Intent(TimedQuestion.this, IntermediateScoreScreen.class);
                                scoreIntent.putExtra("score", msg.getJSONArray("scores").toString());
                                startActivity(scoreIntent);
                                intermediateScoreScreenOpen = true;
                            } else if (msg.getString("what").equals("game_over")) {
                                Intent gameOver = new Intent(TimedQuestion.this, FinalScore.class);
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
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GameLobby.GAME_OVER) {
            if (resultCode == GameLobby.GAME_OVER) {
                TimedQuestion.this.setResult(GameLobby.GAME_OVER, null);
                finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
    }
}
