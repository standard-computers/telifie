package com.telifie.Models.Utilities;

import org.bson.Document;

public class Parameters {

    private final int resultsPerPage;
    private final int pages; // How many pages of search results
    private final int page; //Current page of the search results
    private final String index;
    private final String postalCode; //Index such as images, maps, developers, articles, etc.
    private double latitude;
    private double longitude;
    private final boolean disableQuickResults;

    public Parameters(Document document) throws NullPointerException {
        this.resultsPerPage = (document.getInteger("results_per_page") == null ? 50 : document.getInteger("results_per_page"));
        this.pages = (document.getInteger("pages") == null ? 1 : document.getInteger("pages"));
        this.page = (document.getInteger("page") == null ? 0 : document.getInteger("page"));
        this.index = (document.getString("index") == null ? "articles" : document.getString("index"));
        this.postalCode = (document.getString("postal_code") == null ? "" : document.getString("postal_code"));
        this.latitude = (document.getDouble("latitude") == null ? 39.103699 : document.getDouble("latitude"));
        this.longitude = (document.getDouble("longitude") == null ? -84.513611 : document.getDouble("longitude"));
        this.disableQuickResults = (document.getBoolean("disable_quick_results") != null && document.getBoolean("disable_quick_results"));
    }

    public int getResultsPerPage() {
        return resultsPerPage;
    }

    public String getIndex() {
        return index;
    }

    public int getSkip(){
        return (this.page * this.resultsPerPage);
    }

    public String getPostalCode() {
        return postalCode;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    @Override
    public String toString() {
        return "{ \"results_per_page\" : " + resultsPerPage +
                ", \"pages\" : " + pages +
                ", \"page\" : " + page +
                ", \"postal_code\" : \"" + postalCode + "\"" +
                ", \"latitude\" : " + latitude +
                ", \"longitude\" : " + longitude +
                ", \"disable_quick_results\" : " + disableQuickResults +
                ", \"index\" : \"" + index + "\"}";
    }
}
