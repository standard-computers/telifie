package com.telifie.Models.Utilities;

import java.io.Serializable;

public class Address implements Serializable {

    private String line1, line2, city, territory, zip, country;

    public String getLine1() {
        return line1;
    }

    public void setLine1(String line1) {
        this.line1 = line1;
    }

    public String getLine2() {
        return line2;
    }

    public void setLine2(String line2) {
        this.line2 = line2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getTerritory() {
        return territory;
    }

    public void setTerritory(String territory) {
        this.territory = territory;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    @Override
    public String toString() {
        return "{" +
                "line1 : '" + line1 + '\'' +
                ", line2 : '" + line2 + '\'' +
                ", city : '" + city + '\'' +
                ", territory : '" + territory + '\'' +
                ", zip : '" + zip + '\'' +
                ", country : '" + country + '\'' +
                '}';
    }
}
