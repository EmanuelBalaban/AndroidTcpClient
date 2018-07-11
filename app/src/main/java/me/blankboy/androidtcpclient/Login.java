package me.blankboy.androidtcpclient;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.net.InetSocketAddress;

import me.blankboy.tcpclient.ConnectionListener;

public abstract class Login extends AppCompatActivity implements ConnectionListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ((TextView) findViewById(R.id.title)).setText("Login to " + Variables.PrimaryServer.UniqueIdentity());
    }

    void onLoginClick(View view) {
        TextView title = (TextView) findViewById(R.id.title);
        TextView usernameBox = (TextView) findViewById(R.id.usernameBox);
        TextView passwordBox = (TextView) findViewById(R.id.passwordBox);

        String username = usernameBox.getText().toString();
        String password = passwordBox.getText().toString();

        if (username == "") usernameBox.setError("This field is required!");
        if (username.contains(":")) usernameBox.setError("Username cannot contain ':'");
        if (password == "") passwordBox.setError("This field is required!");
        if (password.contains(":")) passwordBox.setError("Password cannot contain ':'");
        if (usernameBox.getError() != null || passwordBox.getError() != null) return;

        Variables.PrimaryServer.Login(username, password);

        finish();
    }
}
