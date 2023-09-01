package io.jans.DPoP;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.UUID;
import io.jans.DPoP.keyGen.DPoPProofFactory;
import io.jans.DPoP.modal.LoginResponse;
import io.jans.DPoP.modal.OIDCClient;
import io.jans.DPoP.modal.OPConfiguration;
import io.jans.DPoP.modal.TokenResponse;
import io.jans.DPoP.retrofit.RetrofitClient;
import io.jans.DPoP.services.DBHandler;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    EditText username;
    EditText password;
    Button loginButton;
    ProgressBar loginProgressBar;
    public static final String USER_INFO = "io.jans.DPoP.LoginActivity.USER_INFO";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginButton = findViewById(R.id.loginButton);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loginProgressBar = findViewById(R.id.loginProgressBar);
                loginProgressBar.setVisibility(View.VISIBLE);

                username = findViewById(R.id.username);
                password = findViewById(R.id.password);
                String usernameText = username.getText().toString();
                String passwordText = password.getText().toString();

                DBHandler dbH = new DBHandler(LoginActivity.this, "chipDB", null, 1);

                OIDCClient oidcClient = dbH.getOIDCClient(1);
                processlogin(usernameText, passwordText, dbH);
            }
        });
    }

    private void processlogin(String usernameText, String passwordText, DBHandler dbH) {

        OPConfiguration opConfiguration = dbH.getOPConfiguration();
        OIDCClient oidcClient = dbH.getOIDCClient(1);

        Log.d("Inside processlogin :: getAuthorizationChallengeEndpoint ::", opConfiguration.getAuthorizationChallengeEndpoint());
        Call<LoginResponse> call = RetrofitClient.getInstance(opConfiguration.getIssuer()).getAPIInterface().
                getAuthorizationChallenge(oidcClient.getClientId(),
                        usernameText,
                        passwordText,
                        UUID.randomUUID().toString(),
                        UUID.randomUUID().toString(),
                        opConfiguration.getAuthorizationChallengeEndpoint());
        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {

                LoginResponse responseFromAPI = response.body();
                Log.d("processlogin Response :: getAuthorizationCode ::", responseFromAPI.getAuthorizationCode());

                if (responseFromAPI.getAuthorizationCode() != null && !responseFromAPI.getAuthorizationCode().isEmpty()) {
                    getToken(responseFromAPI.getAuthorizationCode(), usernameText, passwordText, dbH);
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Log.e("Inside processlogin :: onFailure :: ", t.getMessage());
                Toast.makeText(LoginActivity.this, "Error in  generating authorization code : " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void getToken(String authorizationCode, String usernameText, String passwordText, DBHandler dbH) {
        OPConfiguration opConfiguration = dbH.getOPConfiguration();
        OIDCClient oidcClient = dbH.getOIDCClient(1);
        /*Log.d("Inside getToken :: basic token ::", "Basic " + Base64.encodeToString((usernameText + ":" + passwordText).getBytes(), Base64.NO_WRAP));
        Log.d("Inside getToken :: opConfiguration.getTokenEndpoint() ::", opConfiguration.getTokenEndpoint());
        Log.d("Inside getToken :: authorizationCode ::", authorizationCode);
        Log.d("Inside getToken :: oidcClient.getClientId() ::", oidcClient.getClientId());
        Log.d("Inside getToken :: oidcClient.getClientSecret() ::", oidcClient.getClientSecret());
        Log.d("Inside getToken :: usernameText + \":\" + passwordText ::", oidcClient.getClientId() + ":" + oidcClient.getClientSecret());*/
        try {
            Log.d("dpop token", DPoPProofFactory.issueDPoPJWTToken("POST", opConfiguration.getIssuer()));

//        Log.d("Inside getToken :: oidcClient.getScope() ::",oidcClient.getScope());
            Call<TokenResponse> call = RetrofitClient.getInstance(opConfiguration.getIssuer()).getAPIInterface()
                    .getToken(oidcClient.getClientId(),
                            authorizationCode,
                            "authorization_code",
                            opConfiguration.getIssuer(),
                            "openid",
                            "Basic " + Base64.encodeToString((oidcClient.getClientId() + ":" + oidcClient.getClientSecret()).getBytes(), Base64.NO_WRAP),
                            DPoPProofFactory.issueDPoPJWTToken("POST", opConfiguration.getIssuer()),
                            opConfiguration.getTokenEndpoint());
            call.enqueue(new Callback<TokenResponse>() {
                @Override
                public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                    // this method is called when we get response from our api.
                    Toast.makeText(LoginActivity.this, "Processing Token.", Toast.LENGTH_SHORT).show();

                    TokenResponse responseFromAPI = response.body();

                    if (responseFromAPI.getAccessToken() != null && !responseFromAPI.getAccessToken().isEmpty()) {
                        Log.d("getToken Response :: getIdToken ::", responseFromAPI.getIdToken());
                        Log.d("getToken Response :: getTokenType ::", responseFromAPI.getTokenType());
                        oidcClient.setRecentGeneratedIdToken(responseFromAPI.getIdToken());
                        dbH.updateOIDCClient(oidcClient);
                        getUserInfo(responseFromAPI.getAccessToken(), dbH);
                    }
                }

                @Override
                public void onFailure(Call<TokenResponse> call, Throwable t) {
                    Log.e("Inside getToken :: onFailure :: ", t.getMessage());
                    Toast.makeText(LoginActivity.this, "Error in Token generation : " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
    }

    private void getUserInfo(String accessToken, DBHandler dbH) {
        OPConfiguration opConfiguration = dbH.getOPConfiguration();

        Call<Object> call = RetrofitClient.getInstance(opConfiguration.getIssuer()).getAPIInterface().getUserInfo(accessToken, "Bearer " + accessToken, opConfiguration.getUserinfoEndpoint());
        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                // this method is called when we get response from our api.
                Toast.makeText(LoginActivity.this, "Processing Token.", Toast.LENGTH_SHORT).show();

                Object responseFromAPI = response.body();
                Log.d("getUserInfo Response :: getUserInfo ::", responseFromAPI.toString());

                if (responseFromAPI != null) {
                    Intent intent = new Intent(LoginActivity.this, AfterLoginActivity.class);
                    intent.putExtra(USER_INFO, responseFromAPI.toString());
                    startActivity(intent);
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                Log.e("Inside getUserInfo :: onFailure :: ", t.getMessage());
                Toast.makeText(LoginActivity.this, "Error found is : " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}