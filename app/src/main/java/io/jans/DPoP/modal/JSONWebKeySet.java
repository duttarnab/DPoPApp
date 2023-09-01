package io.jans.DPoP.modal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.util.JSONObjectUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JSONWebKeySet {

    private List<Map<String, ?>> keys;

    public JSONWebKeySet() {
        keys = new ArrayList<>();
    }

    public List<Map<String, ?>> getKeys() {
        return keys;
    }

    public void setKeys(List<Map<String, ?>> keys) {
        this.keys = keys;
    }

    public void addKey(Map<String, ?> key){
        keys.add(key);
    }

    public String toJsonString() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
