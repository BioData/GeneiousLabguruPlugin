package com.biomatters.plugins.example;
import com.google.gson.annotations.SerializedName;

public class Item {
    @SerializedName("item")
    private Plasmid iPlasmid;
    public Item(Plasmid plasmid){
        iPlasmid = plasmid;
    }
}
