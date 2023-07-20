package com.telifie.Models.Actions;

import com.telifie.Models.*;
import com.telifie.Models.Andromeda;
import com.telifie.Models.Parser;
import com.telifie.Models.Clients.*;
import com.telifie.Models.Connectors.Spotify;
import com.telifie.Models.Utilities.*;
import org.apache.hc.core5.http.ParseException;
import org.bson.Document;
import org.bson.json.JsonWriterSettings;
import org.json.JSONException;
import org.json.JSONObject;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Command {

    private final String command;
    private final String targetDomain;
    private final String primarySelector;
    private final String[] selectors;

    public Command(String command){
        this.command = command;
        String[] spl = this.command.split("://");
        this.targetDomain = spl.length <= 1 || spl[0].equals("") || spl[0].contains("/") ? "telifie" : spl[0];
        this.selectors = this.command.replaceFirst(targetDomain + "://", "").split("(?!//)/");
        this.primarySelector = this.get(0);
    }

    public String get(int index){
        return this.selectors[index].replaceAll("/", "");
    }

    public Result parseCommand(Configuration config, Document content){

        String objectSelector = (this.selectors.length > 1 ? this.get(1) : "");
        String secSelector = (this.selectors.length > 2 ? this.get(2) : null);
        String terSelector = (this.selectors.length > 3 ? this.get(3) : null);
        String actingUser = config.getAuthentication().getUser();

        if(primarySelector.equals("search")){
            String query = this.command.replaceFirst("search/", "");
            DomainsClient domains = new DomainsClient(config);
            Domain domain;
            if(!this.targetDomain.equals("telifie")){
                query = this.command.replaceFirst(this.targetDomain + "://search/", "");
                try{
                    domain = domains.withAltId(this.targetDomain.trim());
                }catch (NullPointerException n){
                    domain = config.getDomain();
                }
                domain.setUri(config.getDomain().getUri());
                config.setDomain(domain);
            }
            if(content != null){
                try {
                    Parameters params = new Parameters(content);
                    return Search.execute(config, query, params);
                }catch(NullPointerException n){
                    return new Result(428, this.command, "Invalid search parameters");
                }
            }
            return Search.execute(config, query, new Parameters(25, 0, "articles"));

        }else if(primarySelector.equals("domains")){

            if(this.selectors.length >= 2){ //telifie.com/domains/{owner|member|create|update}

                DomainsClient domains = new DomainsClient(config);
                ArrayList<Domain> foundDomains;
                if(objectSelector.equals("owner")){ //Domains they own

                    foundDomains = domains.mine();
                    return new Result(this.command, "domains", foundDomains);
                }else if(objectSelector.equals("member")){ //Domains they're a member of

                    foundDomains = domains.forMember(config.getUser().getEmail());
                    return new Result(this.command, "domains", foundDomains);
                }else if(objectSelector.equals("create")){ //Creating a new domain

                    if(content != null){
                        String domainName, domainIcon;
                        if((domainName = content.getString("name")) != null && (domainIcon = content.getString("icon")) != null && content.getInteger("permissions") != null){
                            Domain newDomain = new Domain(actingUser, domainName, domainIcon, content.getInteger("permissions"));
                            if(domains.create(newDomain)){
                                return new Result(this.command, "domain", newDomain);
                            }
                            return new Result(505, this.command, "Failed to create domain");
                        }
                        return new Result(428, this.command, "Required domain info not provided");
                    }
                    return new Result(428, this.command, "JSON body expected");

                }else if(this.selectors.length == 3){ //telifie.com/domains/{delete|id}/{domainID}
                    if(secSelector != null){
                        try{
                            Domain d = domains.withAltId(secSelector);
                            switch (objectSelector) {
                                case "delete" -> {
                                    if (d.getOwner().equals(actingUser)) {
                                        if (domains.delete(d)) {
                                            return new Result(200, this.command, "Domain deleted");
                                        }
                                        return new Result(505, this.command, "Failed to delete domain");
                                    }
                                    return new Result(401, this.command, "This is not your domain");
                                }
                                case "id" -> {
                                    return new Result(this.command, "domain", d);
                                }
                                case "update" -> {
                                    if (content != null) {
                                        if (domains.update(d, content)) { //TODO content check
                                            return new Result(this.command, "domain", d);
                                        }
                                    }
                                    return new Result(428, this.command, "JSON body expected");
                                }
                            }
                            return new Result(404, this.command, "Bad domains selector");
                        }catch (NullPointerException n){
                            return new Result(404, this.command, "Domain not found");
                        }
                    }
                    return new Result(428, this.command, "Domain ID expected");
                }else if(this.selectors.length == 4){ //telifie.com/domains/{id}/users/{add|remove}
                    try{
                        Domain d = domains.withAltId(objectSelector);
                        if(content != null){

                            ArrayList<Member> members = new ArrayList<>();
                            content.getList("users", Document.class).forEach(doc -> members.add(new Member(doc)));
                            switch (terSelector) {
                                case "add" -> {
                                    if (domains.addUsers(d, members)) {
                                        return new Result(200, this.command, "Added " + members.size() + " user(s) to domain");
                                    }
                                    return new Result(505, this.command, "Failed adding " + members.size() + " user(s) to domain");
                                }
                                case "remove" -> {
                                    if (domains.removeUsers(d, members)) {
                                        return new Result(200, this.command, "Removed " + members.size() + " user(s) from domain");
                                    }
                                    return new Result(505, this.command, "Failed removing " + members.size() + " user(s) from domain");
                                }
                                case "update" -> {
                                    if (domains.updateUsers(d, members)) {
                                        return new Result(200, this.command, "Updated " + members.size() + " user(s) in domain");
                                    }
                                    return new Result(505, this.command, "Failed to update user in domain");
                                }
                            }
                            return new Result(428, this.command, "Bad domains users selector");
                        }
                        return new Result(428, this.command, "JSON body expected");
                    }catch (NullPointerException n){
                        return new Result(404, this.command, "Domain not found");
                    }
                }
            }
            return new Result(200, this.command, "Bad domains selector");
        }
        /*
         * Accessing Articles
         */
        else if(primarySelector.equals("articles")){

            if(config.getUser().getPermissions() < 12 && !this.targetDomain.equals("telifie")){
                //TODO, share changes with data team for approval and change status on Article
                return new Result(401, this.command, "Insufficient permissions");
            }
            ArticlesClient articles = new ArticlesClient(config);
            if(this.selectors.length >= 3){

                try {
                    Article a = articles.withId(secSelector);
                    switch (objectSelector) {
                        case "id" -> {
                            try {
                                return new Result(this.command, "article", articles.withId(secSelector));
                            } catch (NullPointerException n) {
                                return new Result(404, this.command, "Article not found");
                            }
                        }
                        case "update" -> {
                            if (content != null) {
                                if (content.getString("id") == null) {
                                    content.put("id", secSelector);
                                }
                                Article updatedArticle = new Article(content);
                                ArrayList<Event> events = updatedArticle.compare(a);
                                events.forEach(e -> e.setUser(config.getUser().getId()));
                                if (articles.update(a, updatedArticle)) {
                                    TimelinesClient timelines = new TimelinesClient(config);
                                    timelines.addEvents(secSelector, events);
                                    return new Result(this.command, "events", events.toString());
                                }
                                return new Result(505, this.command, "Failed to update Article");
                            }
                            return new Result(428, "No new Article JSON data provided");
                        }
                        case "delete" -> {
                            if (articles.delete(articles.withId(secSelector))) {
                                return new Result(200, this.command, "");
                            }
                            return new Result(505, this.command, "Failed to delete article");
                        }
                        case "duplicate" -> {
                            if (this.selectors.length > 3) {
                                DomainsClient domains = new DomainsClient(config);
                                try {
                                    if (articles.duplicate(a, domains.withAltId(terSelector))) {
                                        return new Result(200, this.command, "");
                                    }
                                    return new Result(505, this.command, "Failed to duplicate article");
                                } catch (NullPointerException n) {
                                    return new Result(404, this.command, "Domain not found");
                                }
                            }
                        }
                        case "archive" -> {
                            if (articles.archive(a)) {
                                return new Result(200, this.command, "Article unarchived");
                            }
                            return new Result(505, this.command, "Failed to archive article");
                        }
                        case "unarchive" -> {
                            ArchiveClient archive = new ArchiveClient(config);
                            if (archive.unarchive(a)) {
                                return new Result(200, this.command, "");
                            }
                            return new Result(505, this.command, "Failed to unarchive article");
                        }
                        case "move" -> {
                            if (this.selectors.length > 3) {
                                DomainsClient domains = new DomainsClient(config);
                                try {
                                    if (articles.move(a, domains.withAltId(terSelector))) {
                                        return new Result(200, this.command, "");
                                    }
                                    return new Result(505, this.command, "Failed to move article");
                                } catch (NullPointerException n) {
                                    return new Result(404, this.command, "Domain not found");
                                }
                            }
                        }
                        case "verify" -> {
                            if (articles.verify(articles.withId(secSelector))) {
                                return new Result(200, this.command, "");
                            }
                            return new Result(505, this.command, "Failed to update Article");
                        }
                    }
                    return new Result(404, this.command, "Unknown articles action");
                }catch (NullPointerException n){
                    return new Result(404, this.command, "Article not found");
                }

            }else if(objectSelector.equals("create")){

                if(content != null){
                    DomainsClient domains = new DomainsClient(config);
                    try{
                        Domain domain = domains.withAltId(this.targetDomain);
                        domain.setUri(config.getDomain().getUri());
                        config.setDomain(domain);
                        try {
                            Article na = new Article(content);
                            if(articles.create(na)){
                                return new Result(this.command, "article", na);
                            }
                            return new Result(505, "Failed to create Article");
                        }catch(JSONException e){
                            return new Result(505, this.command, "Malformed Article JSON data provided");
                        }
                    }catch (NullPointerException n) {
                        return new Result(404, this.command, "Domain not found");
                    }
                }
                return new Result(428, "Precondition Failed. No new Article provided (JSON) as body");
            }
            return new Result(this.command,"stats", articles.stats());
        }
        /*
         * Accessing Collections
         * Save, unsave, create, delete, update,
         */
        else if(primarySelector.equals("collections")){

            CollectionsClient collections = new CollectionsClient(config);
            if(this.selectors.length >= 4){ //Ensure correct argument count

                try{
                    Collection c = collections.get(actingUser, secSelector);
                    try{
                        ArticlesClient articles = new ArticlesClient(config);
                        Article a = articles.withId(terSelector);
                        if(objectSelector.equals("save")){
                            if(collections.save(c, a)){
                                return new Result(200, this.command, "Saved Article");
                            }
                            return new Result(505, this.command, "Failed to save Article");
                        } else if(objectSelector.equals("unsave")){

                            if(collections.unsave(c, a)){
                                return new Result(200, this.command, "Unsaved Article");
                            }
                            return new Result(505, this.command, "Failed to unsave Article");
                        }
                        return new Result(404, this.command, "Invalid collections action");
                    }catch (NullPointerException n){
                        return new Result(404, this.command, "Article not found");
                    }
                }catch (NullPointerException n){
                    return new Result(404, this.command, "Collection not found");
                }

            }else if(this.selectors.length >= 2){

                if(secSelector != null) {
                    try {
                        Collection c = collections.get(actingUser, secSelector);
                        switch (objectSelector) {
                            case "update" -> {
                                if (content != null) {
                                    if (content.getString("name") != null && content.getString("name").equals("Pinned")) {
                                        return new Result(304, this.command, "'Pinned' is a reserved Collection name");
                                    }
                                    if (collections.update(c, content)) {
                                        return new Result(200, this.command, "Collection Update");
                                    }
                                    return new Result(505, this.command, "Failed to update Collection");
                                }
                                return new Result(428, this.command, "Update content for Collection not provided");
                            }
                            case "delete" -> {
                                if (collections.delete(c)) {
                                    return new Result(200, this.command, "Collection '" + secSelector + "' deleted");
                                }
                                return new Result(505, this.command, "Failed to delete group '" + secSelector + "'");
                            }
                            case "id" -> {
                                return new Result(this.command, "collection", c);
                            }
                        }
                        return new Result(404, this.command, "Invalid collections command");

                    }catch (NullPointerException n){
                        return new Result(404, this.command, "Collection not found");
                    }
                }else if(objectSelector.equals("create")){

                    if(content != null){
                        Collection newCollection = new Collection(content);
                        Collection createdCollection = collections.create(newCollection);
                        if(createdCollection != null){
                            return new Result(this.command, "collection", createdCollection);
                        }
                        return new Result(505, this.command, "Failed to make Collection with JSON");
                    }
                    return new Result(428, "JSON request body expected");
                }
                return new Result(428, "ID of Collection required to update");
            }
            ArrayList<Collection> usersCollections = collections.forUser(actingUser);
            return new Result(this.command, "collections", usersCollections);
        }
        /*
         * Accessing Users
         */
        else if(primarySelector.equals("users")){

            UsersClient users = new UsersClient(config);
            if(this.selectors.length >= 3){

                if(objectSelector.equals("update") && content != null){

                    String userEmail = config.getUser().getEmail();
                    User changedUser = users.getUserWithEmail(userEmail);
                    if(secSelector.equals("theme")){
                        Theme theme = new Theme(content);
                        if(users.updateUserTheme(users.getUserWithEmail(userEmail), theme)){
                            return new Result(this.command, "user", changedUser);
                        }
                        return new Result(400, "Bad Request");
                    }else if(secSelector.equals("photo")){
                        
                        if(content.getString("photo") != null) {
                            String photoUri = content.getString("photo");
                            if (users.updateUserPhoto(changedUser, photoUri)) {
                                return new Result(200, this.command, "User photo updated");
                            }
                            return new Result(505, this.command, "Failed to update user photo");
                        }
                        return new Result(428, this.command, "JSON parameter 'photo' expected");
                    }
                    return new Result(404, this.command, "Invalid users update command");
                }
                return new Result(404, this.command, "Invalid command received");

            }else if(objectSelector.equals("create")){

                if(content != null){
                    User newUser = new User(content.getString("email"), content.getString("name"), content.getString("phone"));
                    if(newUser.getPermissions() == 0 && !newUser.getName().equals("") && !newUser.getEmail().equals("") && newUser.getName() != null && newUser.getEmail() != null) {
                        if (!users.userExistsWithEmail(newUser.getEmail())) {
                            if (users.createUser(newUser)) {
                                return new Result(this.command, "user", newUser);
                            }
                            return new Result(505, this.command, "Failed making user");
                        }
                        return new Result(410, this.command, "Email taken");
                    }
                    return new Result(428, this.command, "Not enough info");
                }
                return new Result(428, this.command, "JSON request body expected");
            }
            Matcher matcher = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE).matcher(objectSelector);
            if (matcher.matches()) {
                if(users.userExistsWithEmail(objectSelector)){
                    User found = users.getUserWithEmail(objectSelector);
                    return new Result(this.command, "user", found);
                }
                return new Result(404, this.command, "User not found");
            }
            return new Result(404, this.command, "Invalid user email");
        }
        /*
         * Accessing Parser
         */
        else if(primarySelector.equals("parser")){

            ArticlesClient articles = new ArticlesClient(config);
            if(content != null){
                String mode = content.getString("mode");
                if(mode != null){
                    switch (mode) {
                        case "batch" -> {
                            String uri = content.getString("uri");
                            double priority = (content.getDouble("priority") == null ? 1.01 : content.getDouble("priority"));
                            ArrayList<Article> extractedArticles = Parser.engines.batch(uri, priority);
                            if (extractedArticles != null) {
                                if (content.getBoolean("insert") != null && content.getBoolean("insert") == true) {
                                    articles.createMany(extractedArticles);
                                }
                                return new Result(this.command, "articles", extractedArticles);
                            }
                            return new Result(404, this.command, "No articles from batch upload");
                        }
                        case "yelp" -> {
                            String[] zips = content.getString("zips").split(",");
                            ArrayList<Article> yelps;
                            try {
                                yelps = Parser.connectors.yelp(zips, config);
                            } catch (UnsupportedEncodingException e) {
                                return new Result(505, this.command, "Failed to Yelp");
                            }
                            return new Result(this.command, "articles", yelps);
                        }
                        case "tmdb" -> {

                        }
                        case "uri" -> {
                            String url = content.getString("uri");
                            if (url != null && !url.equals("")) {
                                Parser parser = new Parser();
                                Article parsed = Parser.engines.parse(url);
                                if (content.getBoolean("insert") != null && content.getBoolean("insert")) {
                                    if (parsed != null) {
                                        articles.create(parsed);
                                    }
                                }
                                return new Result(this.command, "article", parsed);
                            }
                            return new Result(428, this.command, "URI is required");
                        }
                        case "crawl" -> {
                            String url = content.getString("uri");
                            if (url != null && !url.equals("")) {
                                Parser parser = new Parser();
                                parser.purge();
                                int limit = (content.getInteger("limit") == null ? Integer.MAX_VALUE : content.getInteger("limit"));
                                Parser.engines.crawl(config, url, limit, false);
                                return new Result(this.command, "articles", parser.getTraversable());
                            }
                            return new Result(428, this.command, "URI is required");
                        }
                        case "text" -> {
                            String text = content.getString("text");
                            List<Andromeda.unit> tokens = Andromeda.encoder.tokenize(text, false);
                        }
                        case "edit" -> {
                            ArrayList<String> ids = new ArrayList<>();
                            articles.getIds().forEach(a -> ids.add(new Article(a).getId()));
                            return new Result(this.command, "ids", "" + ids + "");
                        }
                    }
                }
                return new Result(428, this.command, "Select parser mode");
            }
            return new Result(428, this.command, "JSON request body expected");
        }
        /*
         * Authenticate an app for backend use to facilitate user authentication
         */
        else if(primarySelector.equals("authenticate")){

            if(this.selectors.length >= 3 && objectSelector.equals("app")){
                Authentication auth = new Authentication(secSelector);
                AuthenticationClient authentications = new AuthenticationClient(config);
                if(authentications.authenticate(auth)){
                    System.out.println("App authenticated with ID: " + secSelector);
                    Telifie.console.out.message(auth.toJson().toString(4));
                    return new Result(this.command, "authentication", auth.toJson());
                }
                return new Result(505, "Failed creating app authentication");
            }
            return new Result(428, "Need more params");
        }
        /*
         * Connect: Creating or logging in user
         */
        else if(primarySelector.equals("connect")){

            if(this.selectors.length >= 2){
                UsersClient u = new UsersClient(config);
                if(u.userExistsWithEmail(objectSelector)){
                    User user = u.getUserWithEmail(objectSelector);
                    if(user.getPermissions() == 0){
                        //TODO lock user with email
                    }else if(user.getPermissions() >= 1){
                        if(u.sendCode(user)){
                            return new Result(200, "Authentication code sent");
                        }
                        return new Result(501, "Failed to send code");
                    }
                }
                return new Result(404, "Account not found");
            }
            return new Result(428, "Need more params");
        }
        /*
         * Verify: Unlocking account using one-time 2fa token
         */
        else if(primarySelector.startsWith("verify")){

            if(this.selectors.length >= 3){
                String code = Telifie.tools.make.md5(secSelector);
                UsersClient users = new UsersClient(config);
                User user = users.getUserWithEmail(objectSelector);
                if(user.hasToken(code)){
                    if(user.getPermissions() == 0 || user.getPermissions() == 1){
                        users.upgradePermissions(user);
                        user.setPermissions(user.getPermissions() + 1);
                    }
                    Authentication auth = new Authentication(user);
                    AuthenticationClient auths = new AuthenticationClient(config);
                    auths.authenticate(auth);
                    JSONObject json = new JSONObject(user.toString());
                    json.put("authentication", auth.toJson());
                    return new Result(this.command, "user", json);
                }
                return new Result(403, "Invalid verification code provided");
            }
            return new Result(404, this.command, "Invalid command received");
        }
        /*
         * Timelines: Accessing the timelines (history) of objects
         */
        else if(primarySelector.equals("timelines")){

            if(this.selectors.length >= 2){
                TimelinesClient timelines = new TimelinesClient(config);
                Timeline timeline = timelines.getTimeline(objectSelector);
                if(timeline != null){
                    return new Result(this.command, "timeline", timeline);
                }
                return new Result(404, this.command, "No timeline found for " + objectSelector);
            }
            return new Result(428, this.command, "Please provide object ID to get timeline");

        }else if(primarySelector.equals("connectors")){

            ConnectorsClient connectors = new ConnectorsClient(config);
            if(content != null){

                Connector connector = new Connector(content);
                if(connector != null){ //Creating connector for user
                    connector.setUser(actingUser);
                    boolean connectorUsed = connectors.exists(connector);
                    if(connector.getId().equals("com.telifie.connectors.spotify")){
                        Spotify spotify;
                        try {
                            spotify = new Spotify(connector);
                            if(!connectorUsed){ //User hasn't used this connector before
                                connectors.create(spotify.getConnector());
                            }
                            spotify.parse(config);
                            return new Result(this.command, "spotify", "done");
                        } catch (IOException | ParseException | SpotifyWebApiException e) {
                            return new Result(501, this.command, "Failed to parse Spotify credentials");
                        }
                    }else{
                        if(connectorUsed){
                            return new Result(409, this.command, "Connector already exists");
                        }
                        connectors.create(connector);
                    }
                }
                return new Result(428, this.command, "JSON body expected to create connector");
            }
            if(this.selectors.length >= 2){
                if(objectSelector.equals("connected")){
                    return new Result(this.command, "connectors", connectors.mine());
                }
                Connector connector = connectors.getConnector(objectSelector);
                if(connector != null){
                    return new Result(this.command, "connector", connector);
                }
                return new Result(404, this.command, "No connector found for " + objectSelector);
            }
            return new Result(428, this.command, "Please provide connector name to get");
        }
        return new Result(200, this.command, "No command received");
    }
}