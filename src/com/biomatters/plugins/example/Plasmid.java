package com.biomatters.plugins.example;

import com.google.gson.annotations.SerializedName;

public class Plasmid {
    @SerializedName("title")
    private String pTitle;
    @SerializedName("sequence")
    private String pSequence;
    public Plasmid(String title, String sequence){
        pTitle = title;
        pSequence = sequence;
    }
}
