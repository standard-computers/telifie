package com.telifie.Models.Clients;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.geojson.Point;
import com.mongodb.client.model.geojson.Position;
import com.telifie.Models.Actions.Search;
import com.telifie.Models.Andromeda.Andromeda;
import com.telifie.Models.Andromeda.Unit;
import com.telifie.Models.Domain;
import com.telifie.Models.Utilities.Console;
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
        super.collection = "articles";
    }

    public boolean update(Article article, Article newArticle){
        return super.updateOne(new Document("id", article.getId()), new Document("$set", Document.parse(newArticle.toString())));
    }

    public boolean create(Article article){
        if(article.getOwner() == null || article.getOwner().isEmpty()){
            article.setOwner(session.getUser());
        }
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
        return this.find(filter).map(Article::new).into(new ArrayList<>());
    }

    public ArrayList<Article> withProjection(Document filter, Document projection){
        ArrayList<Article> articles = new ArrayList<>();
        this.findWithProjection(filter, projection).map(Article::new).into(new ArrayList<>());
        return articles;
    }

    public Article findPlace(String place, Parameters params){
        params.setIndex("locations");
        return this.search(new Unit(place), params,
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
                ))).get(0);
    }

    public ArrayList<Article> search(Unit query, Parameters params, Document filter){
        FindIterable<Document> found;
        if(params.isQuickResults()){
            found = this.findWithProjection(filter, new Document("id", 1)
                    .append("icon", 1)
                    .append("title", 1)
                    .append("description", 1)
                    .append("link", 1)
                    .append("tags", 1)
                    .append("priority", 1)
                    .append("attributes", 1));
        }else{
            found = this.find(filter);
        }
        ArrayList<Article> results = found.map(Article::new).into(new ArrayList<>());
        if(Andromeda.tools.has(Andromeda.PROXIMITY, query.text()) > -1 || params.getIndex().equals("locations")) {
            results.sort(new DistanceSorter(params.getLatitude(), params.getLongitude()));
        }else{
            results.sort(new CosmoScore(query.cleaned()));
        }
        return results;
    }

    public boolean delete(Article article) {
        return super.deleteOne(new Document("id", article.getId()));
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

    public Document stats() {
        Document groupFields = new Document("_id", "$description");
        groupFields.put("count", new Document("$sum", 1));
        groupFields.put("priority", new Document("$avg", "$priority"));
        Document groupStage = new Document("$group", groupFields);
        List<Document> iterable = super.aggregate(groupStage);
        Document stats = new Document();
        int total = super.count();
        stats.append("total", total);
        TreeMap<String, Document> sortedDescriptions = new TreeMap<>();
        for (Document document : iterable) {
            String description = document.getString("_id");
            int count = document.getInteger("count");
            Double averagePriority = document.getDouble("priority");
            if (description == null) {
                description = "Unclassified";
            }
            double percent = (double) count / total * 100;
            Document descriptionStats = new Document();
            descriptionStats.append("count", count);
            descriptionStats.append("percent", percent);
            descriptionStats.append("priority", averagePriority);
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
                Console.log("Failed converting URL in lookup");
            }
        }
        return false;
    }

    private boolean areSimilarURLs(URI uri1, URI uri2) {
        return uri1.getHost().equalsIgnoreCase(uri2.getHost()) && removeTrailingSlash(uri1.getPath()).equalsIgnoreCase(removeTrailingSlash(uri2.getPath()));
    }

    private String removeTrailingSlash(String path) {
        if (path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    private static class CosmoScore implements Comparator<Article> {

        private final String q;
        private final ArrayList<String> words;

        public CosmoScore(String q){
            this.q = q;
            this.words = new ArrayList<>(Arrays.asList(q.split("\\s+")));
            this.words.add(0, q);
        }

        @Override
        public int compare(Article a, Article b) {
            double relevanceA = relevance(a);
            double relevanceB = relevance(b);
            return Double.compare(relevanceB, relevanceA);
        }

        private double relevance(Article a) {
            double s = 0;
            s += (a.getLink() == null ? 0 : compareMatches(a.getLink(), words)); //Link Score
            s += (a.getTitle().trim().toLowerCase().equals(q) ? words.size() * s : 0); //Title Match
            s += compareMatches(a.getTitle(), words); //Title Score
            s += compareMatches(a.getDescription(), words); //Description Score
            for(String tag : a.getTags()){
                if(words.contains(tag)){
                    s += 2;
                }
            }
            return (a.isVerified() ? (s + 1) : s);
        }

        private double compareMatches(String text, ArrayList<String> words) {
            int m = 1;
            for(int i = 0; i < words.size(); i++){
                if(text.contains(words.get(i))) {
                    if(i == 0){
                        m = m * 3;
                    }
                    m++;
                }
            }
            return (double) m / words.size();
        }
    }

    private record DistanceSorter(double latitude, double longitude) implements Comparator<Article> {

        @Override
        public int compare(Article a, Article b) {
            double relevanceA = distance(Double.parseDouble(a.getAttribute("Latitude")), Double.parseDouble(a.getAttribute("Longitude")));
            double relevanceB = distance(Double.parseDouble(b.getAttribute("Latitude")), Double.parseDouble(b.getAttribute("Longitude")));
            return Double.compare(relevanceB, relevanceA);
        }

        private double distance(double latitude, double longitude) {
            final int R = 6371;
            double latDistance = Math.toRadians(latitude - this.latitude);
            double lonDistance = Math.toRadians(longitude - this.longitude);
            double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) + Math.cos(Math.toRadians(this.latitude)) * Math.cos(Math.toRadians(this.longitude)) * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            return R * c;
        }
    }
}