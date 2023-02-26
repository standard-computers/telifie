package com.telifie.Models;

import com.telifie.Models.Clients.UsersClient;
import com.telifie.Models.Connectors.Available.TextMessenger;
import com.telifie.Models.Utilities.*;
import com.telifie.Models.Utilities.Network;
import com.telifie.Models.Actions.Out;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.bson.Document;
import org.json.JSONObject;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class User implements Serializable {

    private String id, email, name, phone, token, customerId;
    private final int origin;
    private int permissions;
    private Theme theme;

    public User(String email) {
        this.email = email;
        this.origin = (int) (System.currentTimeMillis() / 1000);
        this.permissions = 0;
    }

    public User(String email, String name, String phone) {
        this.id = Tool.md5(Tool.eid());
        this.email = email;
        this.name = name;
        this.phone = phone;
        this.origin = (int) (System.currentTimeMillis() / 1000);
        this.permissions = 0;
    }

    public User(Document document){
        this.id = document.getString("id");
        this.email = document.getString("email");
        this.name = document.getString("name");
        this.phone = document.getString("phone");
        this.token = document.getString("token");
        this.origin = document.getInteger("origin");
        this.permissions = document.getInteger("permissions");
        this.theme = new Theme(document.get("theme", Document.class));
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public int getOrigin() {
        return origin;
    }

    public int getPermissions() {
        return permissions;
    }

    public void setPermissions(int permissions) {
        this.permissions = permissions;
    }

    public Theme getTheme() {
        return theme;
    }

    public void setTheme(Theme theme) {
        this.theme = theme;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public boolean hasToken(String attempt){
        if(attempt.equals(this.token)){
            return true;
        }else{
            return false;
        }
    }

    public boolean lock(){
        if(this.getPhone() == null || this.getPhone() == ""){
            return lockWithEmail();
        }else{
            return lockWithPhone();
        }
    }

    private boolean lockWithEmail(){
        //TODO
        return false;
    }

    private boolean lockWithPhone(){
        String code = Tool.simpleCode();
        TextMessenger messenger = new TextMessenger();
        messenger.send(this.getPhone(), "+12243476722", "Welcome back! Your login code is " + code);
        //TODO ini UsersClient class with domain to fix OOP issue
        UsersClient users = new UsersClient("mongodb://137.184.70.9:27017");
        return users.lockUser(this, code);
    }

    public boolean requestAuthenticationCode(){
        //TODO move to DB connection
        Network get2fa = new Network();
        List<NameValuePair> data = new ArrayList<>();
        data.add(new BasicNameValuePair("email", this.getEmail()));
        data.add(new BasicNameValuePair("app_id", "main.java.telifie.User.requestAuthenticationCode"));
        data.add(new BasicNameValuePair("auth_token", "an9f7moqw8fhx387fhcwomr"));
        CloseableHttpResponse response = get2fa.post("http://telifie.net/connect", data);
        Out.console("[ HTTP RESPONSE CODE / CONNECT ] " + response);
        if(get2fa.getStatusCode() == 200){
            return true;
        }else{
            return false;
        }
    }

    public boolean verify(String attempt){
        Network get2fa = new Network();
        List<NameValuePair> data = new ArrayList<>();
        data.add(new BasicNameValuePair("email", this.getEmail()));
        data.add(new BasicNameValuePair("attempt", attempt));
        data.add(new BasicNameValuePair("auth_token", "asdukhflaisuhdfpas9d8fy"));
        data.add(new BasicNameValuePair("app_id", "main.java.telifie.Start.install"));
        CloseableHttpResponse response = get2fa.post("http://telifie.net/verify", data);
        Out.console("[ HTTP RESPONSE CODE / VERIFY ] " + get2fa.getStatusCode());
        if(get2fa.getStatusCode() == 200){
            String jsonResponse = null;
            try {
                jsonResponse = EntityUtils.toString(response.getEntity());
                Gson gson = new Gson();
                JsonElement element = gson.fromJson(jsonResponse, JsonElement.class);
                JsonObject jsonObject = element.getAsJsonObject();
                this.id = jsonObject.get("id").getAsString();
                this.name = jsonObject.get("name").getAsString();
                this.phone = jsonObject.get("phone").getAsString();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return true;
        }else{
            return false;
        }
    }

    @Override
    public String toString() {
        return "{" +
                "\"id\" : \"" + id + '\"' +
                ", \"email\" : \"" + email + '\"' +
                ", \"name\" : \"" + name + '\"' +
                ", \"phone\" : \"" + phone + '\"' +
                (this.customerId == null || this.customerId.equals("") ? "" : ", \"phone\" : \"" + phone + '\"') +
                ", \"origin\" : " + origin +
                ", \"permissions\" : " + permissions +
                ", \"theme\" : " + theme +
                '}';
    }

    public JSONObject toJson(){
        return new JSONObject(this.toString());
    }

}
