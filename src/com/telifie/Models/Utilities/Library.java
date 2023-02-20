package com.telifie.Models.Utilities;

import java.util.ArrayList;

public class Library extends ArrayList<Asset> {

    public Library(){
        super();
    }

    public String toJSON(){

        StringBuilder json = new StringBuilder(); //Prep for JSON exporting
        for(int i = 0; i < this.size(); i++){
            if (i == (this.size() - 1)) {
                json.append(this.get(i).toString());
            }else{
                json.append(this.get(i).toString()).append(",");
            }
        }
        return json + "";
    }

    public boolean add(Asset asset){
        super.add(asset);
        return false;
    }

}
