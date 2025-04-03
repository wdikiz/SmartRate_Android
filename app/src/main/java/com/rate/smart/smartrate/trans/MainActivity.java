/*
 *  * Copyright (c) 2024 Walid ZOUBIR
 */

package com.rate.smart.smartrate.trans;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.rate.smart.smartrate.SmartRate;
import com.rate.smart.smartrate.tran.R;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final Context context = this;

        SharedPreferences prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        long launchCount = prefs.getLong("launch_count", 0) + 1;
        editor.putLong("launch_count", launchCount);
        editor.apply();

        SmartRate.init(MainActivity.this)
                .setAfterXLaunches(2)
                .setShowAgainAfterNegative(2)
                .setInAppReviewEnabled(true)
                .setLaunchReviewDirectly(true)
                .setUrlFeedback("https://wwww.exemple.com/sendFeedback.php")
//                .setColorButtonPressed("#E53E30")        // Couleur du bouton pressé (vert)
//                .setColorButtonUnpressed("#955444")      // Couleur du bouton non pressé (bleu ciel)
//                .setColorTextPrimary("#CA64EA")          // Couleur principale du texte
//                .setColorTextSecondary("#FF6C37")        // Couleur secondaire du texte
//                .setColorBackground("#5AA2AE")           // Couleur d'arrière-plan des dialogues
                .setOnCloseClickListener(() -> {
                    SharedPreferences prefs1 = MainActivity.this.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
                    prefs1.edit().putBoolean("DialogClosedWithoutAction", true).apply();
                })
                .setOnFeedbackClickListener(() -> {
                    SmartRate.dontShowAgain(true, MainActivity.this);
                })
                .build();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            SharedPreferences prefs12 = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
            boolean dontShowAgain = prefs12.getBoolean("dontshowagain", false);

            if (!dontShowAgain) {
                boolean dialogClosedWithoutAction = prefs12.getBoolean("DialogClosedWithoutAction", true);
                SmartRate.init(MainActivity.this)
                        .setAfterXLaunches(2)
                        .setShowAgainAfterNegative(2)
                        .setInAppReviewEnabled(true)
                        .setLaunchReviewDirectly(true)
//                        .setColorButtonPressed("#E53E30")        // Couleur du bouton pressé (vert)
//                        .setColorButtonUnpressed("#955444")      // Couleur du bouton non pressé (bleu ciel)
//                        .setColorTextPrimary("#CA64EA")          // Couleur principale du texte
//                        .setColorTextSecondary("#FF6C37")        // Couleur secondaire du texte
//                        .setColorBackground("#5AA2AE")           // Couleur d'arrière-plan des dialogues
                        .setUrlFeedback("https://wwww.exemple.com/sendFeedback.php")
                        .setOnCloseClickListener(() -> {
                            // L'utilisateur a fermé le dialogue; ne pas le montrer à nouveau dans cette session
                        })
                        .setOnFeedbackClickListener(() -> {
                            SmartRate.dontShowAgain(true, MainActivity.this);
                            prefs12.edit().putBoolean("dontshowagain", true)
                                    .putBoolean("InAppReviewShown", true)
                                    .putBoolean("DialogClosedWithoutAction", false)
                                    .apply();
                        })
                        .show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
