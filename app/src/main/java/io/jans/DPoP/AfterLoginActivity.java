package io.jans.DPoP;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.util.UUID;
import io.jans.DPoP.modal.OIDCClient;
import io.jans.DPoP.modal.OPConfiguration;
import io.jans.DPoP.retrofit.RetrofitClient;
import io.jans.DPoP.services.DBHandler;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AfterLoginActivity extends AppCompatActivity {
    TextView message;
    Button logoutButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_after_login);

        message = findViewById(R.id.userInfo);
        Intent intent = getIntent();
        String userInfo = intent.getStringExtra(LoginActivity.USER_INFO);
        message.setText("User Info is: " + userInfo);

        logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DBHandler dbH = new DBHandler(AfterLoginActivity.this, "chipDB", null, 1);
                logout(dbH);
            }
        });
    }

    private void logout(DBHandler dbH) {
        Toast.makeText(AfterLoginActivity.this, "Processing Logout.", Toast.LENGTH_SHORT).show();
        OPConfiguration opConfiguration = dbH.getOPConfiguration();
        OIDCClient oidcClient = dbH.getOIDCClient(1);

        Call<Void> call = RetrofitClient.getInstance(opConfiguration.getIssuer()).getAPIInterface().logout(opConfiguration.getEndSessionEndpoint()+"?state="+ UUID.randomUUID().toString()+"&id_token_hint="+oidcClient.getRecentGeneratedIdToken());
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                // this method is called when we get response from our api.

                Intent intent = new Intent(AfterLoginActivity.this, LoginActivity.class);
                startActivity(intent);
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("Inside getUserInfo :: onFailure :: ", t.getMessage());
                Toast.makeText(AfterLoginActivity.this, "Error in Logout. : " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}