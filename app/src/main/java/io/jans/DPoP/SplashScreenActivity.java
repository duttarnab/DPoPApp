package io.jans.DPoP;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import io.jans.DPoP.modal.OIDCClient;
import io.jans.DPoP.services.DBHandler;

public class SplashScreenActivity extends Activity {
    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                DBHandler dbH = new DBHandler(SplashScreenActivity.this, "chipDB", null, 1);
                OIDCClient client = dbH.getOIDCClient(1);

                if (client == null) {
                    Intent intent = new Intent(SplashScreenActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    Log.d("client", client.toString());
                    Intent intent = new Intent(SplashScreenActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }

            }
        }, 1000);
    }
}