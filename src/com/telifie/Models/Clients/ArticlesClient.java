package com.telifie.Models.Clients;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;
import com.mongodb.client.*;
import com.telifie.Models.Andromeda;
import com.telifie.Models.Domain;
import com.telifie.Models.Utilities.Parameters;
import com.telifie.Models.Article;
import com.telifie.Models.Utilities.Session;
import com.telifie.Models.Utilities.Telifie;
import org.bson.Document;

import java.util.*;

public class ArticlesClient extends Client {

    public ArticlesClient(Session session){
        super(session);
        if(!session.getDomain().equals("telifie")){
            super.collection = "domain-articles";
        }else{
            super.collection = "articles";
        }
    }

    public boolean update(Article article, Article newArticle){
        return super.updateOne(new Document("id", article.getId()), new Document("$set", Document.parse(newArticle.toString())));
    }

    public boolean create(Article article){
        if(article.getOwner() == null || article.getOwner().isEmpty()){
            article.setOwner(session.getUser());
        }
        if(article.getLink() == null || article.getLink().isEmpty()){
            return super.insertOne(Document.parse(article.toString()));
        }else if(this.withLink(article.getLink()) == null){
            return super.insertOne(Document.parse(article.toString()));
        }else{
            return false;
        }
    }

    public ArrayList<Article> linked(){
        return this.get(
            new Document("$or",
                Arrays.asList(
                        new Document("source.url", new Document("$exists", true)),
                        new Document("link", new Document("$exists", true))
                )
            )
        );
    }

    public boolean createMany(ArrayList<Article> articles){
        ArrayList<Document> documents = new ArrayList<>();
        for(Article article : articles){
            documents.add(Document.parse(article.toString()));
        }
        return super.insertMany(documents);
    }

    public Article withLink(String link){
        try{
            return new Article(this.findOne(new Document("link", link)));
        }catch (NullPointerException e){
            return null;
        }
    }

    public boolean verify(String articleId){
        return this.updateOne(new Document("id", articleId), new Document("$set", new Document("verified", true)));
    }

    public Article withId(String articleId) {
        return new Article(this.findOne(new Document("id", articleId)));
    }

    public ArrayList<Article> get(Document filter){
        ArrayList<Document> found = this.find(filter);
        ArrayList<Article> articles = new ArrayList<>();
        for(Document doc : found){
            articles.add(new Article(doc));
        }
        return articles;
    }

    public ArrayList<Article> search(String query, Parameters params, Document filter){
        try {
            MongoDatabase database = super.mc.getDatabase("telifie");
            MongoCollection<Document> collection = database.getCollection(super.collection);
            FindIterable<Document> iterable = collection.find(filter).sort(new BasicDBObject("priority", -1));
            ArrayList<Article> results = new ArrayList<>();
            for (Document document : iterable) {
                results.add(new Article(document));
            }
            if(results != null && !query.contains(":")){
                if(Telifie.tools.has(Andromeda.PROXIMITY, query) > -1) {
                    Collections.sort(results, new ArticlesClient.DistanceSorter(params.getLatitude(), params.getLongitude()));
                }else{
                    Collections.sort(results, new ArticlesClient.CosmoScore(Andromeda.encoder.clean(query)));
                }
            }
            return results;
        }catch(MongoException e){
            return null;
        }
    }

    public boolean delete(Article article) {
        return super.deleteOne(new Document("id", article.getId()));
    }

    public ArrayList<Document> getIds(String q){
        return super.find(new Document("verified", false));
    }

    public boolean move(Article article, Domain domain){
        this.delete(article);
        session.setDomain(domain.getId());
        return this.create(article);
    }

    public boolean duplicate(Article article, Domain domain){
        session.setDomain(domain.getId());
        return this.create(article);
    }

    public boolean exists(String id){
        return super.exists(new Document("id", id));
    }

    public boolean existsWithSource(String source){
        return (super.findOne(new Document("source.url", source)) == null ? false : true);
    }

    public ArrayList<Document> withSource(String source){
        if(source.startsWith("http")){
            return super.find(new Document("source.url", source));
        }
        return super.find(new Document("source.name", source));
    }

    public Document stats() {
        Document groupFields = new Document("_id", "$description");
        groupFields.put("count", new Document("$sum", 1));
        Document groupStage = new Document("$group", groupFields);
        ArrayList<Document> iterable = super.aggregate(groupStage);
        Document stats = new Document();
        int total = super.count();
        stats.append("total", total);
        TreeMap<String, Document> sortedDescriptions = new TreeMap<>();
        for (Document document : iterable) {
            String description = document.getString("_id");
            int count = document.getInteger("count");
            if (description == null) {
                description = "Unclassified";
            }
            double percent = (double) count / total * 100; // Calculate the percentage
            Document descriptionStats = new Document();
            descriptionStats.append("count", count);
            descriptionStats.append("percent", percent);
            sortedDescriptions.put(description, descriptionStats);
        }
        Document descriptions = new Document();
        sortedDescriptions.forEach((key, value) -> descriptions.append(key, value));
        stats.append("descriptions", descriptions);
        return stats;
    }

    public boolean archive(Article article){
        ArchiveClient archive = new ArchiveClient(session);
        return archive.archive(article);
    }

    private static class CosmoScore implements Comparator<Article> {

        private final String query;
        private final String[] words;

        public CosmoScore(String query){
            this.query = query;
            this.words = Andromeda.encoder.nova(Andromeda.encoder.clean(query));
        }

        @Override
        public int compare(Article a, Article b) {
            double relevanceA = relevance(a);
            double relevanceB = relevance(b);
            return Double.compare(relevanceB, relevanceA);
        }

        private double relevance(Article a) {
            if(a.getTitle().trim().toLowerCase().equals(query)){
                return Integer.MAX_VALUE;
            }
            double titleGrade = (countMatches(a.getTitle(), words) / words.length) * 5;
            double linkGrade = (countMatches((a.getLink() == null ? "" : a.getLink()), words) / words.length) * 2;
            double tagsGrade = 0;
            if(a.getTags() != null && !a.getTags().isEmpty()){
                for(String tag : a.getTags()){
                    tagsGrade += countMatches(tag, words);
                }
            }
            double retroGrade = ((titleGrade + linkGrade) + ((tagsGrade / words.length) * 0.25)) * a.getPriority();
            return (a.isVerified() ? (retroGrade * 2) : retroGrade);
        }

        private double countMatches(String text, String[] words) {
            int matches = 0;
            for(String word : words) {
                if(text.contains(word)) {
                    matches++;
                }
            }
            return matches / words.length;
        }
    }

    private static class RelevanceComparator implements Comparator<Article> {

        private String query;

        public RelevanceComparator(String query) {
            this.query = query;
        }

        @Override
        public int compare(Article a, Article b) {
            int relevanceA = relevance(a.getTitle());
            int relevanceB = relevance(b.getTitle());
            return Integer.compare(relevanceB, relevanceA);
        }

        private int relevance(String title) {
            int lenDiff = Math.abs(title.length() - query.length());
            if (lenDiff > 3) {
                return lenDiff;
            }
            int[] prev = new int[query.length() + 1];
            int[] curr = new int[query.length() + 1];
            for (int i = 0; i <= title.length(); i++) {
                for (int j = 0; j <= query.length(); j++) {
                    if (i == 0) {
                        curr[j] = j;
                    } else if (j == 0) {
                        curr[j] = i;
                    } else {
                        curr[j] = Math.min(prev[j - 1] + (title.charAt(i - 1) == query.charAt(j - 1) ? 0 : 1),
                                Math.min(prev[j], curr[j - 1]) + 1);
                    }
                }
                int[] temp = prev;
                prev = curr;
                curr = temp;
            }
            return prev[query.length()];
        }
    }

    private static class DistanceSorter implements Comparator<Article> {

        private final double latitude, longitude;

        public DistanceSorter(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        @Override
        public int compare(Article a, Article b) {
            double relevanceA = distance(Double.parseDouble(a.getAttribute("Latitude")), Double.parseDouble(a.getAttribute("Longitude")));
            double relevanceB = distance(Double.parseDouble(b.getAttribute("Latitude")), Double.parseDouble(b.getAttribute("Longitude")));
            return Double.compare(relevanceB, relevanceA);
        }

        private double distance(double latitude, double longitude) {
            final int R = 6371; // Radius of the earth in km
            double latDistance = Math.toRadians(latitude - this.latitude);
            double lonDistance = Math.toRadians(longitude - this.longitude);
            double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) + Math.cos(Math.toRadians(this.latitude)) * Math.cos(Math.toRadians(this.longitude)) * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            return R * c;
        }
    }
}
