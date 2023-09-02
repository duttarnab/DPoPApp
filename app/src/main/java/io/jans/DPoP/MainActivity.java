package io.jans.DPoP;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.common.collect.Lists;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.jans.DPoP.keyGen.DPoPProofFactory;
import io.jans.DPoP.keyGen.KeyManager;
import io.jans.DPoP.modal.DCRequest;
import io.jans.DPoP.modal.DCResponse;
import io.jans.DPoP.modal.JSONWebKeySet;
import io.jans.DPoP.modal.OIDCClient;
import io.jans.DPoP.modal.OPConfiguration;
import io.jans.DPoP.retrofit.RetrofitClient;
import io.jans.DPoP.services.DBHandler;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    EditText issuer;
    EditText scopes;
    Button registerButton;
    ProgressBar registerProgressBar;
    AlertDialog.Builder errorDialog;
    public static final String APP_LINK = "https://dpop.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        errorDialog = new AlertDialog.Builder(this);

        registerButton = findViewById(R.id.registerButton);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                registerProgressBar = findViewById(R.id.registerProgressBar);
                registerProgressBar.setVisibility(View.VISIBLE);

                issuer = findViewById(R.id.issuer);
                scopes = findViewById(R.id.scopes);
                String issuerText = issuer.getText().toString();
                String scopeText = scopes.getText().toString();
                DBHandler dbH = new DBHandler(MainActivity.this, "chipDB", null, 1);
                fetchOPConfiguration(issuerText, scopeText, dbH);
            }
        });
    }

    private void fetchOPConfiguration(String configurationUrl, String scopeText, DBHandler dbH) {

        String issuer = configurationUrl.replace("/.well-known/openid-configuration", "");
        Log.d("Inside fetchOPConfiguration :: configurationUrl ::", configurationUrl);
        Call<OPConfiguration> call = RetrofitClient.getInstance(issuer).getAPIInterface().getOPConfiguration(configurationUrl);

        call.enqueue(new Callback<OPConfiguration>() {
            @Override
            public void onResponse(Call<OPConfiguration> call, Response<OPConfiguration> response) {
                if (response.code() == 200) {
                    OPConfiguration opConfiguration = response.body();
                    Log.d("Inside fetchOPConfiguration :: opConfiguration ::", opConfiguration.toString());
                    dbH.addOPConfiguration(opConfiguration);
                    doDCR(scopeText, dbH);
                } else {
                    createErrorDialog("Error in  fetching OP Configuration.\n Error code: " + response.code() + "\n Error message: " + response.message());
                    errorDialog.show();
                }
            }

            @Override
            public void onFailure(Call<OPConfiguration> call, Throwable t) {
                Log.e("Inside fetchOPConfiguration :: onFailure :: ", t.getMessage());
                //Toast.makeText(MainActivity.this, "Error in fetching configuration : " + t.getMessage(), Toast.LENGTH_SHORT).show();
                createErrorDialog("Error in  fetching OP Configuration.\n" + t.getMessage());
                errorDialog.show();
            }
        });
    }

    private void doDCR(String scopeText, DBHandler dbH) {

        OPConfiguration opConfiguration = dbH.getOPConfiguration();
        String issuer = opConfiguration.getIssuer();
        String registrationUrl = opConfiguration.getRegistrationEndpoint();

        DCRequest dcrRequest = new DCRequest();
        dcrRequest.setIssuer(issuer);
        dcrRequest.setClientName("DPoPAppClient-" + UUID.randomUUID());
        dcrRequest.setApplicationType("web");
        dcrRequest.setGrantTypes(Lists.newArrayList("authorization_code", "client_credentials"));
        dcrRequest.setScope(scopeText);
        dcrRequest.setRedirectUris(Lists.newArrayList(issuer));
        dcrRequest.setResponseTypes(Lists.newArrayList("code"));
        dcrRequest.setTokenEndpointAuthMethod("client_secret_basic");
        dcrRequest.setPostLogoutRedirectUris(Lists.newArrayList(issuer));

        Map<String, Object> claims = new HashMap<>();
        claims.put("aapName", "DPoPApp");
        claims.put("seq", UUID.randomUUID());
        try {
            String evidenceJwt = DPoPProofFactory.issueJWTToken(claims);
            dcrRequest.setEvidence(evidenceJwt);
            Log.d("Inside doDCR :: evidence :: ", evidenceJwt);
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException |
                 NoSuchProviderException e) {
            throw new RuntimeException(e);
        }

        JSONWebKeySet jwks = new JSONWebKeySet();
        jwks.addKey(KeyManager.getPublicKeyJWK(KeyManager.getInstance().getPublicKey()).getRequiredParams());

        dcrRequest.setJwks(jwks.toJsonString());
        Log.d("Inside doDCR :: jwks :: ", jwks.toJsonString());

        Call<DCResponse> call = RetrofitClient.getInstance(issuer).getAPIInterface().doDCR(dcrRequest, registrationUrl);
        call.enqueue(new Callback<DCResponse>() {
            @Override
            public void onResponse(Call<DCResponse> call, Response<DCResponse> response) {

                DCResponse responseFromAPI = response.body();
                if (response.code() == 200 || response.code() == 201) {
                    if (responseFromAPI.getClientId() != null && !responseFromAPI.getClientId().isEmpty()) {
                        OIDCClient client = new OIDCClient();
                        client.setClientName(responseFromAPI.getClientName());
                        client.setClientId(responseFromAPI.getClientId());
                        client.setClientSecret(responseFromAPI.getClientSecret());
                        client.setScope(scopeText);
                        dbH.addOIDCClient(client);

                        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                        startActivity(intent);
                    }
                } else {
                    createErrorDialog("Error in  DCR.\n Error code: " + response.code() + "\n Error message: " + response.message());
                    errorDialog.show();
                }
            }

            @Override
            public void onFailure(Call<DCResponse> call, Throwable t) {
                Log.e("Inside doDCR :: onFailure :: ", t.getMessage());
                //Toast.makeText(MainActivity.this, "Error in DCR : " + t.getMessage(), Toast.LENGTH_SHORT).show();
                createErrorDialog("Error in  DCR.\n" + t.getMessage());
                errorDialog.show();
            }
        });
    }
    private void createErrorDialog(String message) {
        errorDialog.setMessage(message)
                .setTitle(R.string.error_title)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
    }
}

