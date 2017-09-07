package com.example.glaba.quizapp;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity {
    Button loginButton;
    EditText usernameBox;

    public static String readFromFile(Context context, String file) {
        String str = "";
        try {
            InputStream inputStream = context.openFileInput(file);

            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String curLine;
                StringBuilder stringBuilder = new StringBuilder();

                while ((curLine = bufferedReader.readLine()) != null) {
                    stringBuilder.append(curLine);
                }

                inputStream.close();
                str = stringBuilder.toString();
            }
        } catch (Exception e) {
        }
        return str;
    }

    public static void writeToFile(Context context, String file, String data) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput(file, Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loginButton = (Button)findViewById(R.id.loginButton);
        usernameBox = (EditText)findViewById(R.id.userNameText);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                writeToFile(MainActivity.this, "username.txt", usernameBox.getText().toString());
                startActivity(new Intent(MainActivity.this, LobbySelectionActivity.class));
            }
        });

        usernameBox.setText(readFromFile(MainActivity.this, "username.txt"));
        startService(new Intent(this, SocketService.class));
    }
}
