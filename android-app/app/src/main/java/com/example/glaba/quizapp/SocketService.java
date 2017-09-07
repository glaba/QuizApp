package com.example.glaba.quizapp;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.ArrayAdapter;

import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

public class SocketService extends Service {
    public static final String[] ACTIONS = {"IncomingSocketMessage"};

    LocalBroadcastManager broadcastManager;
    Socket mySocket;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void sendMessage(String message) {
        try {
            PrintWriter printWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(mySocket.getOutputStream())), true);
            printWriter.print(message);
            printWriter.flush();
        } catch (IOException e) {

        }
    }

    @Override
    public void onCreate() {
        IntentFilter broadcastIntentFilter = new IntentFilter();
        for (String filter : LobbySelectionActivity.ACTIONS) {
            broadcastIntentFilter.addAction(filter);
        }
        broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction() == "OutgoingSocketMessage") {
                    sendMessage(intent.getStringExtra("Message"));
                }
            }
        }, broadcastIntentFilter);
    }

    public void sendBroadcast(String action, String message) {
        Intent intent = new Intent(action);
        if (message != null)
            intent.putExtra("Message", message);
        broadcastManager.sendBroadcast(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Client client = new Client("107.170.2.182", 1234);
        client.setClientCallback(new Client.ClientCallback() {
            Timer lobbyUpdateTimer;

            @Override
            public void onMessage(String message) {
                sendBroadcast("IncomingSocketMessage", message);
                try {
                    Log.wtf("Broadcast", (new JSONObject(message)).getString("what"));
                } catch (Exception e) {

                }
            }

            @Override
            public void onConnect(Socket socket) {
                mySocket = socket;
                sendMessage("{\"what\":\"request_lobbies\"}");
            }

            @Override
            public void onDisconnect(Socket socket, String message) {

            }

            @Override
            public void onConnectError(Socket socket, String message) {
                Log.wtf("Error", message);
            }
        });
        client.connect();

        return START_STICKY;
    }
}
