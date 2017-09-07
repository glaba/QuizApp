package com.example.glaba.quizapp;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.ContextThemeWrapper;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class LobbySelectionActivity extends Activity {
    public static final String[] ACTIONS = {"OutgoingSocketMessage"};

    ListView lobbies;
    LocalBroadcastManager broadcastManager;
    ImageButton createLobby;

    ArrayAdapter<String> adapter;

    public void sendBroadcast(String action, String message) {
        Intent intent = new Intent(action);
        if (message != null)
            intent.putExtra("Message", message);
        broadcastManager.sendBroadcast(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lobby_selection);

        lobbies = (ListView)findViewById(R.id.lobbyListView);
        createLobby = (ImageButton)findViewById(R.id.newLobbyButton);

        createLobby.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LobbySelectionActivity.this, LobbyCreationActivity.class));
            }
        });

        final Context _this = this;
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
                            if (msg.getString("what").equals("lobby_update")) {
                                JSONArray lobbyList = msg.getJSONArray("lobby_list");

                                adapter.clear();
                                for (int i = 0; i < lobbyList.length(); i++) {
                                    adapter.add(lobbyList.getString(i));
                                }
                                adapter.notifyDataSetChanged();
                            } else if (msg.getString("what").equals("joined_lobby")) {
                                startActivity(new Intent(LobbySelectionActivity.this, GameLobby.class));
                            } else if (msg.getString("what").equals("wrong_password")) {
                                Toast.makeText(LobbySelectionActivity.this, "Incorrect password", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                        }
                    } catch (Exception e) {
                    }
                }
            }
        }, broadcastIntentFilter);

        adapter = new ArrayAdapter<String>(_this, android.R.layout.simple_list_item_1, new ArrayList<String>());
        lobbies.setAdapter(adapter);
        lobbies.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View v, int position, long id) {
                final AdapterView _parent = parent;
                final int _position = position;

                // Get password if it is a password box
                String lobbyName = (String)parent.getItemAtPosition(position);

                if (lobbyName.charAt(lobbyName.length() - 1) == '\uDD12') {
                    AlertDialog.Builder passwordBox = new AlertDialog.Builder(new ContextThemeWrapper(LobbySelectionActivity.this, R.style.AppTheme));
                    passwordBox.setMessage("Lobby requires a password");
                    passwordBox.setTitle("Password");

                    final EditText input = new EditText(LobbySelectionActivity.this);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                                                                 LinearLayout.LayoutParams.MATCH_PARENT);
                    input.setLayoutParams(lp);
                    passwordBox.setView(input);

                    input.setTransformationMethod(PasswordTransformationMethod.getInstance());

                    passwordBox.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            JSONObject message = new JSONObject();
                            try {
                                message.put("what", "join");
                                message.put("name", MainActivity.readFromFile(LobbySelectionActivity.this, "username.txt"));
                                message.put("lobby", _parent.getItemAtPosition(_position));
                                message.put("password", input.getText());
                            } catch (Exception e) {
                            }
                            sendBroadcast("OutgoingSocketMessage", message.toString());
                        }
                    });

                    passwordBox.setCancelable(true);
                    passwordBox.create().show();
                } else {
                    JSONObject message = new JSONObject();
                    try {
                        message.put("what", "join");
                        message.put("name", MainActivity.readFromFile(LobbySelectionActivity.this, "username.txt"));
                        message.put("lobby", parent.getItemAtPosition(position));
                        message.put("password", "");
                    } catch (Exception e) {
                    }
                    sendBroadcast("OutgoingSocketMessage", message.toString());
                    startActivity(new Intent(LobbySelectionActivity.this, GameLobby.class));
                }
            }
        });

        sendBroadcast("OutgoingSocketMessage", "{\"what\":\"request_lobbies\"}");
    }
}
