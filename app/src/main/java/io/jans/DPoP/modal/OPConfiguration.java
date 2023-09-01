package io.jans.DPoP.modal;

import com.google.gson.annotations.SerializedName;

public class OPConfiguration {
    @SerializedName("issuer")
    private String issuer;
    @SerializedName("registration_endpoint")
    private String registrationEndpoint;
    @SerializedName("token_endpoint")
    private String tokenEndpoint;
    @SerializedName("userinfo_endpoint")
    private String userinfoEndpoint;
    @SerializedName("authorization_challenge_endpoint")
    private String authorizationChallengeEndpoint;
    @SerializedName("end_session_endpoint")
    private String endSessionEndpoint;

    public String getEndSessionEndpoint() {
        return endSessionEndpoint;
    }

    public void setEndSessionEndpoint(String endSessionEndpoint) {
        this.endSessionEndpoint = endSessionEndpoint;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAuthorizationChallengeEndpoint() {
        return authorizationChallengeEndpoint;
    }
    public void setAuthorizationChallengeEndpoint(String authorizationChallengeEndpoint) {
        this.authorizationChallengeEndpoint = authorizationChallengeEndpoint;
    }

    public String getRegistrationEndpoint() {
        return registrationEndpoint;
    }

    public void setRegistrationEndpoint(String registrationEndpoint) {
        this.registrationEndpoint = registrationEndpoint;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public void setTokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    public String getUserinfoEndpoint() {
        return userinfoEndpoint;
    }

    public void setUserinfoEndpoint(String userinfoEndpoint) {
        this.userinfoEndpoint = userinfoEndpoint;
    }
    @Override
    public String toString() {
        return "OPConfiguration{" +
                "issuer='" + issuer + '\'' +
                ", registrationEndpoint='" + registrationEndpoint + '\'' +
                ", tokenEndpoint='" + tokenEndpoint + '\'' +
                ", userinfoEndpoint='" + userinfoEndpoint + '\'' +
                ", authorizationChallengeEndpoint='" + authorizationChallengeEndpoint + '\'' +
                '}';
    }
}
