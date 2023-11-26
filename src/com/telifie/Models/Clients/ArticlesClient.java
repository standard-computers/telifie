package com.telifie.Models.Clients;

import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import com.telifie.Models.Actions.Search;
import com.telifie.Models.Andromeda.Andromeda;
import com.telifie.Models.Andromeda.Encoder;
import com.telifie.Models.Domain;
import com.telifie.Models.Utilities.Parameters;
import com.telifie.Models.Article;
import com.telifie.Models.Utilities.Session;
import org.bson.Document;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Pattern;

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
        if((article.getLink() == null || article.getLink().isEmpty()) || this.withLink(article.getLink()) == null){
            super.insertOne(Document.parse(article.toString()));
            if(article.hasAttribute("Longitude") && article.hasAttribute("Latitude")){
                String longitude = article.getAttribute("Longitude");
                String latitude = article.getAttribute("Latitude");
                if (longitude != null && latitude != null && !longitude.equals("null") && !latitude.equals("null")) {
                    double longitudeValue = Double.parseDouble(longitude);
                    double latitudeValue = Double.parseDouble(latitude);
                    Position position = new Position(longitudeValue, latitudeValue);
                    Point point = new Point(position);
                    super.updateOne(new Document("id", article.getId()),
                            new Document("$set", new Document("location", point)));
                }
            }
            return true;
        }
        return false;
    }

    public boolean createMany(ArrayList<Article> articles){
        ArrayList<Document> documents = new ArrayList<>();
        articles.forEach(a -> documents.add(Document.parse(a.toString())));
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
        found.forEach(f -> articles.add(new Article(f)));
        return articles;
    }

    public ArrayList<Article> withProjection(Document filter, Document projection){
        ArrayList<Document> found = this.findWithProjection(filter, projection);
        ArrayList<Article> articles = new ArrayList<>();
        found.forEach(f -> articles.add(new Article(f)));
        return articles;
    }


    public Article findPlace(String place, Parameters params){
        params.setIndex("locations");
        return this.search(
                place, params,
                new Document("$and", Arrays.asList(
                        new Document("title", Pattern.compile("\\b" + Pattern.quote(place) + "\\w*\\b", Pattern.CASE_INSENSITIVE)),
                        new Document("description", "City"),
                        new Document("location", new Document("$near",
                                new Document("$geometry", new Document("type", "Point")
                                        .append("coordinates", Arrays.asList(
                                                params.getLongitude(),
                                                params.getLatitude()
                                        ))
                                ).append("$maxDistance", Integer.MAX_VALUE)
                        )
                        )
                )),
        true).get(0);
    }

    public ArrayList<Article> search(String query, Parameters params, Document filter, boolean quickResults){
        ArrayList<Document> found;
        if(quickResults){
            found = this.findWithProjection(filter, new Document("icon", 1)
                    .append("title", 1)
                    .append("description", 1)
                    .append("link", 1)
                    .append("tags", 1)
                    .append("priority", 1)
                    .append("attributes", 1));
        }else{
            found = this.find(filter);
        }
        ArrayList<Article> results = new ArrayList<>();
        found.forEach(a -> results.add(new Article(a)));
        if(!query.contains(":")){
            if(Andromeda.tools.has(Andromeda.PROXIMITY, query) > -1 || params.getIndex().equals("locations")) {
                Collections.sort(results, new ArticlesClient.DistanceSorter(params.getLatitude(), params.getLongitude()));
            }else{
                Collections.sort(results, new ArticlesClient.CosmoScore(Encoder.clean(query)));
            }
        }
        return results;
    }

    public boolean delete(Article article) {
        return super.deleteOne(new Document("id", article.getId()));
    }

    public ArrayList<Document> getIds(){
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
        sortedDescriptions.forEach(descriptions::append);
        stats.append("descriptions", descriptions);
        return stats;
    }

    public boolean archive(Article article){
        ArchiveClient archive = new ArchiveClient(session);
        return archive.archive(article);
    }

    /**
     * Returns Levenshtein difference for link and link provided of most relevant article found
     * @param uri URI for link of Article
     * @return int Levenshtein difference
     */
    public boolean lookup(String uri){
        ArrayList<Article> matches = get(new Document("link", Search.pattern(uri)));
        for (Article a : matches) {
            String link = a.getLink();
            try {
                URI storedURI = new URI(link);
                URI providedURI = new URI(uri);
                if (areSimilarURLs(storedURI, providedURI)) {
                    return true;
                }
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private boolean areSimilarURLs(URI uri1, URI uri2) {
        return uri1.getHost().equalsIgnoreCase(uri2.getHost()) &&
                removeTrailingSlash(uri1.getPath()).equalsIgnoreCase(removeTrailingSlash(uri2.getPath()));
    }

    private String removeTrailingSlash(String path) {
        if (path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    private static class CosmoScore implements Comparator<Article> {

        private final String query;
        private final String[] words;

        public CosmoScore(String query){
            this.query = query;
            this.words = Encoder.clean(query).split("\\s+");
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