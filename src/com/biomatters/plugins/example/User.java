package com.biomatters.plugins.example;

import com.google.gson.annotations.SerializedName;

public class User {
    @SerializedName("login")
    private String usrLogin;
    @SerializedName("password")
    private String usrPassword;
    @SerializedName("token")
    private String token;

    public User(String login, String password){
        usrLogin = login;
        usrPassword = password;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public String getUsrLogin() {return usrLogin;}
}
