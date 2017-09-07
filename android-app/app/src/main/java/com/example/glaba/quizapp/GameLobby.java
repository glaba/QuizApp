package com.example.glaba.quizapp;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.ArrayList;

public class GameLobby extends Activity {
    public static final int GAME_OVER = 3;

    LocalBroadcastManager broadcastManager;
    BroadcastReceiver receiver;
    ListView userNameList;
    Button beginButton;

    public void sendBroadcast(String action, String message) {
        Intent intent = new Intent(action);
        if (message != null)
            intent.putExtra("Message", message);
        broadcastManager.sendBroadcast(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_lobby);

        userNameList = (ListView)findViewById(R.id.userNameList);

        userNameList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                JSONObject message = new JSONObject();
                try {
                    message.put("what", "kick_user");
                    message.put("user", parent.getItemAtPosition(position));
                } catch (Exception e) {

                }
                sendBroadcast("OutgoingSocketMessage", message.toString());
            }
        });

        beginButton = (Button)findViewById(R.id.beginButton);
        beginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JSONObject message = new JSONObject();
                try {
                    message.put("what", "start_game");
                } catch (Exception e) {

                }
                sendBroadcast("OutgoingSocketMessage", message.toString());
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
                            if (msg.getString("what").equals("user_list")) {
                                ArrayList<String> usernames = new ArrayList<>();
                                for (int i = 0; i < msg.getJSONArray("user_list").length(); i++) {
                                    usernames.add(msg.getJSONArray("user_list").getString(i));
                                }
                                ArrayAdapter<String> adapter = new ArrayAdapter<>(GameLobby.this, android.R.layout.simple_list_item_1, usernames);
                                userNameList.setAdapter(adapter);
                            } else if (msg.getString("what").equals("kick")) {
                                Toast.makeText(GameLobby.this, msg.getString("msg"), Toast.LENGTH_SHORT).show();
                                finish();
                            } else if (msg.getString("what").equals("start_game")) {
                                Intent i = new Intent(GameLobby.this, TimedQuestion.class);
                                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivityForResult(i, GAME_OVER);
                            } else if (msg.getString("what").equals("start_game_live_updates")) {
                                Intent i = new Intent(GameLobby.this, LiveUpdate.class);
                                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivityForResult(i, GAME_OVER);
                            } else if (msg.getString("what").equals("game_over")) {
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
    }

    @Override
    public void onBackPressed() {
        JSONObject message = new JSONObject();
        try {
            message.put("what", "leave_lobby");
        } catch (Exception e) {

        }
        sendBroadcast("OutgoingSocketMessage", message.toString());
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GAME_OVER) {
            if (resultCode == GAME_OVER) {
                this.finish();
            }
        }
    }
}
