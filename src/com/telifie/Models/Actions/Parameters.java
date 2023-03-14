package com.telifie.Models.Actions;

import org.bson.Document;

public class Parameters {

    private int resultsPerPage;
    private int pages; // How many pages of search results
    private int page; //Current page of the search results
    private String index = "articles"; //Index such as images, maps, developers, articles, etc.

    public Parameters(Document document){
        this.resultsPerPage = document.getInteger("results_per_page");
        this.pages = document.getInteger("pages");
        this.page = document.getInteger("page");
        this.index = document.getString("index");

    }

    public Parameters(int resultsPerPage, int pages, String index) {
        this.resultsPerPage = resultsPerPage;
        this.pages = pages;
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

    @Override
    public String toString() {
        return "{ \"results_per_page\" : " + resultsPerPage +
                ", \"pages\" : " + pages +
                ", \"page\" : " + page +
                ", \"index\" : \"" + index + "\"}";
    }
}
