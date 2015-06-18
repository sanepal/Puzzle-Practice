package com.snepal.padpractice;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;


public class MainActivity extends ActionBarActivity {

    private AdView adView;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        loadAds();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            Uri image = data.getData();
            Intent intent = new Intent(this, GameActivity.class);
            intent.putExtra("uri", image.toString());
            startActivity(intent);
        }
    }

    public void findImage(View v) {
        Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, 1);
//        Intent intent = new Intent(this, GameActivity.class);
//        startActivity(intent);
    }

    private void loadAds() {
        boolean firstRun = sharedPreferences.getBoolean("first_run", true);
        boolean disableAds = sharedPreferences.getBoolean("disable_ads", false);
        if (!firstRun && !disableAds) {
            adView = (AdView) findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        }
        if (firstRun) {
            sharedPreferences.edit().putBoolean("first_run", false).apply();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        sharedPreferences = getPreferences(Context.MODE_PRIVATE);
        boolean disableAds = sharedPreferences.getBoolean("disable_ads", false);
        menu.getItem(0).setChecked(disableAds);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_ads) {
            boolean checked = item.isChecked();
            item.setChecked(! checked);
            sharedPreferences.edit().putBoolean("disable_ads", !checked).apply();
            if (! checked) {
                if (adView != null) {
                    adView.destroy();
                    adView = null;
                }
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /** Called when leaving the activity */
    @Override
    public void onPause() {
        if (adView != null) {
            adView.pause();
        }
        super.onPause();
    }

    /** Called when returning to the activity */
    @Override
    public void onResume() {
        super.onResume();
        if (adView != null) {
            adView.resume();
        }
    }

    /** Called before the activity is destroyed */
    @Override
    public void onDestroy() {
        if (adView != null) {
            adView.destroy();
        }
        super.onDestroy();
    }
}
