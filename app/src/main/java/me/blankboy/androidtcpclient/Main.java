package me.blankboy.androidtcpclient;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;

public class Main extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        //TextView label = findViewById(R.id.textView2);
        //label.setText(item.getTitle());

        if (id == R.id.nav_console) {
            ConstraintLayout cl = findViewById(R.id.first);
            cl.bringToFront();
        } else if (id == R.id.nav_gallery) {
            ConstraintLayout cl = findViewById(R.id.second);
            cl.bringToFront();
        } else if (id == R.id.nav_slideshow) {
            ConstraintLayout cl = findViewById(R.id.third);
            cl.bringToFront();
        } else if (id == R.id.nav_manage) {
            ConstraintLayout cl = findViewById(R.id.fourth);
            cl.bringToFront();
        } /*else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }*/

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void onHeaderClick(View view){
        int id = view.getId();
        if (id == R.id.headerButton){
            Button btn = findViewById(R.id.headerButton);
            if (!Variables.IsServerConnected()) startActivityForResult(new Intent(this, MainActivity.class), 13000);
            else {
                Variables.PrimaryServer.Disconnect();
                UpdateHeader();
            }
        }
        if (id == R.id.navHeader || id == R.id.headerTextView){
            if(Variables.IsServerConnected()) startActivity(new Intent(this, MainActivity.class));
        }
    }

    void UpdateHeader(){
        Button btn = findViewById(R.id.headerButton);
        TextView txt = findViewById(R.id.headerTextView);
        if (Variables.IsServerConnected()){
            btn.setText("Disconnect");
            InetSocketAddress address = Variables.PrimaryServer.UniqueAddress;
            txt.setText(address.getHostString() + ":" + address.getPort());
        } else {
            btn.setText("Connect");
            txt.setText("Not Connected!");
            txt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        }
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 13000){
            UpdateHeader();
        }
    }
}
