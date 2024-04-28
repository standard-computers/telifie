package com.telifie.Models.Utilities;

import org.bson.Document;

public class Parameters {

    public final int rpp, pages, page;
    public final String index, zip;
    public final double latitude, longitude;

    public Parameters(Document document) throws NullPointerException {
        this.rpp = (document.getInteger("results_per_page") == null ? 50 : document.getInteger("results_per_page"));
        this.pages = (document.getInteger("pages") == null ? 1 : document.getInteger("pages"));
        this.page = (document.getInteger("page") == null ? 1 : document.getInteger("page"));
        this.index = (document.getString("index") == null ? "articles" : document.getString("index"));
        this.zip = (document.getString("zip") == null ? "" : document.getString("zip"));
        this.latitude = (document.getDouble("latitude") == null ? 39.103699 : document.getDouble("latitude"));
        this.longitude = (document.getDouble("longitude") == null ? -84.513611 : document.getDouble("longitude"));
    }

    @Override
    public String toString() {
        return new StringBuilder().append("{ \"results_per_page\" : ").append(rpp)
                .append(", \"pages\" : ").append(pages)
                .append(", \"page\" : ").append(page)
                .append(", \"zip\" : \"").append(zip).append("\"")
                .append(", \"latitude\" : ").append(latitude)
                .append(", \"longitude\" : ").append(longitude)
                .append(", \"index\" : \"").append(index).append("\"}").toString();
    }
}