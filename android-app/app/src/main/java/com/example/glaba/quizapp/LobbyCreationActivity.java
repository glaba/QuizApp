package com.example.glaba.quizapp;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class LobbyCreationActivity extends Activity {
    LocalBroadcastManager broadcastManager;
    Spinner gameListSpinner;
    Button createLobbyButton;
    EditText lobbyText, passwordBox;
    CheckBox leaderPlays, randomizeOrder, showStatistics;

    public void sendBroadcast(String action, String message) {
        Intent intent = new Intent(action);
        if (message != null)
            intent.putExtra("Message", message);
        broadcastManager.sendBroadcast(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby_creation);

        gameListSpinner = (Spinner)findViewById(R.id.gameListSpinner);
        createLobbyButton = (Button)findViewById(R.id.sendCreateLobbyCommandButton);
        lobbyText = (EditText)findViewById(R.id.lobbyName);
        passwordBox = (EditText)findViewById(R.id.passwordBox);
        leaderPlays = (CheckBox)findViewById(R.id.leaderPlaysCheckBox);
        randomizeOrder = (CheckBox)findViewById(R.id.listOrderCheckBox);
        showStatistics = (CheckBox)findViewById(R.id.showStatisticsCheckBox);

        lobbyText.setText(MainActivity.readFromFile(LobbyCreationActivity.this, "username.txt") + "'s lobby");

        createLobbyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (lobbyText.getText().toString().equals("")) {
                    Toast.makeText(LobbyCreationActivity.this, "Invalid game name!", Toast.LENGTH_SHORT).show();
                    return;
                }

                JSONObject message = new JSONObject();
                try {
                    message.put("what", "create");
                    message.put("leaderName", MainActivity.readFromFile(LobbyCreationActivity.this, "username.txt"));
                    message.put("gameName", lobbyText.getText());
                    message.put("leaderPlays", leaderPlays.isChecked());
                    message.put("randomizeOrder", randomizeOrder.isChecked());
                    message.put("showStatsEveryRound", showStatistics.isChecked());
                    message.put("listName", gameListSpinner.getItemAtPosition(gameListSpinner.getSelectedItemPosition()).toString());
                    message.put("password", passwordBox.getText());
                    message.put("giveNextQuestionImmediately", !showStatistics.isChecked());
                } catch (Exception e) {

                }
                sendBroadcast("OutgoingSocketMessage", message.toString());

                startActivity(new Intent(LobbyCreationActivity.this, GameLobby.class));
            }
        });

        IntentFilter broadcastIntentFilter = new IntentFilter();
        for (String filter : SocketService.ACTIONS) {
            broadcastIntentFilter.addAction(filter);
        }
        broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("IncomingSocketMessage")) {
                    String message = intent.getStringExtra("Message");
                    try {
                        final JSONObject msg = new JSONObject(message);
                        try {
                            if (msg.getString("what").equals("list_names")) {
                                JSONArray nameList = msg.getJSONArray("list_names");
                                ArrayList<String> list = new ArrayList<>();
                                for (int i = 0; i < nameList.length(); i++) {
                                    list.add(nameList.getString(i));
                                }
                                ArrayAdapter<String> adapter = new ArrayAdapter<String>(LobbyCreationActivity.this, android.R.layout.simple_spinner_item, list);
                                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                gameListSpinner.setAdapter(adapter);
                            }
                        } catch (Exception e) {
                        }
                    } catch (Exception e) {
                    }
                }
            }
        }, broadcastIntentFilter);

        JSONObject message = new JSONObject();
        try {
            message.put("what", "get_list_names");
        } catch (Exception e) {

        }
        sendBroadcast("OutgoingSocketMessage", message.toString());
    }
}
