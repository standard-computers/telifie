package com.telifie.Models.Utilities;

import org.bson.Document;

public class Parameters {

    private final int resultsPerPage, pages, page;
    private String index;
    private final String postalCode; //Index such as images, maps, developers, articles, etc.
    private double latitude, longitude;
    private final boolean quickResults;

    public Parameters(Document document) throws NullPointerException {
        this.resultsPerPage = (document.getInteger("results_per_page") == null ? 50 : document.getInteger("results_per_page"));
        this.pages = (document.getInteger("pages") == null ? 1 : document.getInteger("pages"));
        this.page = (document.getInteger("page") == null ? 1 : document.getInteger("page"));
        this.index = (document.getString("index") == null ? "articles" : document.getString("index"));
        this.postalCode = (document.getString("postal_code") == null ? "" : document.getString("postal_code"));
        this.latitude = (document.getDouble("latitude") == null ? 39.103699 : document.getDouble("latitude"));
        this.longitude = (document.getDouble("longitude") == null ? -84.513611 : document.getDouble("longitude"));
        this.quickResults = (document.getBoolean("quick_results") == null ? false : document.getBoolean("quick_results"));
    }

    public int getResultsPerPage() {
        return resultsPerPage;
    }

    public int getPage() {
        return page;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
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

    public boolean isQuickResults() {
        return quickResults;
    }

    @Override
    public String toString() {
        return new StringBuilder().append("{ \"results_per_page\" : ").append(resultsPerPage)
                .append(", \"pages\" : ").append(pages)
                .append(", \"page\" : ").append(page)
                .append(", \"postal_code\" : \"").append(postalCode).append("\"")
                .append(", \"latitude\" : ").append(latitude)
                .append(", \"longitude\" : ").append(longitude)
                .append(", \"quick_results\" : ").append(quickResults)
                .append(", \"index\" : \"").append(index).append("\"}").toString();
    }
}