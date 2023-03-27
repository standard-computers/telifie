package com.telifie.Models.Actions;

import org.bson.Document;

public class Parameters {

    private int resultsPerPage;
    private int pages; // How many pages of search results
    private int page; //Current page of the search results
    private String index = "articles", postalCode; //Index such as images, maps, developers, articles, etc.
    private double latitude, longitude;

    public Parameters(Document document) throws NullPointerException {
        this.resultsPerPage = (document.getInteger("results_per_page") == null ? 25 : document.getInteger("results_per_page"));
        this.pages = (document.getInteger("pages") == null ? 1 : document.getInteger("pages"));
        this.page = (document.getInteger("page") == null ? 0 : document.getInteger("page"));
        this.index = (document.getString("index") == null ? "articles" : document.getString("index"));
        this.postalCode = (document.getString("postal_code") == null ? "" : document.getString("postal_code"));
        this.latitude = (document.getDouble("latitude") == null ? 0.0 : document.getDouble("latitude"));
        this.longitude = (document.getDouble("longitude") == null ? 0.0 : document.getDouble("longitude"));

    }

    public Parameters(int resultsPerPage, int page, String index) {
        this.resultsPerPage = resultsPerPage;
        this.page = page;
        this.index = index;
    }

    public int getResultsPerPage() {
        return resultsPerPage;
    }

    public void setResultsPerPage(int resultsPerPage) {
        this.resultsPerPage = resultsPerPage;
    }

    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index.trim().toLowerCase();
    }

    public int getSkip(){
        return (this.page * this.resultsPerPage);
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
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
                ", \"index\" : \"" + index + "\"}";
    }
}
