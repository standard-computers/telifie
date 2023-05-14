package com.telifie.Models.Actions;

import com.telifie.Models.*;
import com.telifie.Models.Clients.*;
import com.telifie.Models.Connectors.Spotify;
import com.telifie.Models.Utilities.*;
import org.apache.hc.core5.http.ParseException;
import org.bson.Document;
import org.json.JSONException;
import org.json.JSONObject;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
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
        if(primarySelector.equals("domains")){

            if(this.selectors.length >= 2){

                DomainsClient domains = new DomainsClient(config);
                ArrayList<Domain> foundDomains;
                if(objectSelector.equals("owner")){

                    foundDomains = domains.mine();
                    return new Result(this.command, "domains", foundDomains);
                }else if(objectSelector.equals("member")){ //Domains they're a member of

                    foundDomains = domains.forMember(config.getUser().getEmail());
                    return new Result(this.command, "domains", foundDomains);
                }else if(objectSelector.equals("create")){

                    if(content != null){
                        String domainName, domainIcon;
                        if((domainName = content.getString("name")) != null && (domainIcon = content.getString("icon")) != null && content.getInteger("permissions") != null){
                            Domain newDomain = new Domain(actingUser, domainName, domainIcon, content.getInteger("permissions"));
                            if(domains.create(newDomain)){
                                return new Result(this.command, "domain", newDomain);
                            }
                            return new Result(505, this.command, "Failed to make domain");
                        }
                        return new Result(428, this.command, "Required JSON properties not provided");
                    }
                    return new Result(428, this.command, "JSON body expected");

                }else if(objectSelector.equals("delete")){

                    try {
                        Domain d = domains.withId(secSelector);
                        if (d.getOwner().equals(actingUser)) {
                            if (domains.delete(d)) {
                                return new Result(200, this.command, "Successfully deleted the domain");
                            }
                            return new Result(505, this.command, "Failed to delete domain");
                        }
                        return new Result(401, this.command, "This is not your domain");
                    }catch (NullPointerException n){
                        return new Result(404, this.command, "Domain not found");
                    }
                }else if(objectSelector.equals("id")){

                    try{
                        Domain d = domains.withAltId(secSelector);
                        return new Result(this.command, "domain", d);
                    }catch(NullPointerException n){
                        return new Result(404, this.command, "Domain not found");
                    }
                }else if(this.selectors.length >= 3){

                    try {
                        Domain d = domains.withId(secSelector);
                        if(command.equals("users") && this.selectors.length >= 4){

                            if(content != null){

                                ArrayList<Member> members = new ArrayList<>();
                                content.getList("users", Document.class).forEach(doc -> members.add(new Member(doc)));
                                if(terSelector.equals("add")){

                                    if(domains.addUsers(d, members)){
                                        return new Result(200, this.command, "Added " + members.size() + " user(s) to domain");
                                    }
                                    return new Result(505, this.command, "Failed adding " + members.size() + " user(s) to domain");
                                }else if(terSelector.equals("remove")){

                                    if(domains.removeUsers(d, members)){
                                        return new Result(200, this.command, "Removed " + members.size() + " user(s) from domain");
                                    }
                                    return new Result(505, this.command, "Failed removing " + members.size() + " user(s) from domain");
                                }else if(terSelector.equals("update")) {

                                    if (domains.updateUsers(d, members)) {
                                        return new Result(200, this.command, "Updated " + members.size() + " user(s) in domain");
                                    }
                                    return new Result(505, this.command, "Failed to update user in domain");
                                }
                                return new Result(428, this.command, "Bad domain user action");
                            }
                            return new Result(428, this.command, "JSON body expected");
                        }
                        return new Result(this.command, "domain", d);
                    }catch(NullPointerException n){
                        return new Result(404, this.command, "Domain not found");
                    }
                }
                return new Result(404, this.command, "Invalid domains selector");
            }
            return new Result(200, this.command, "Invalid command for domains");
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
                if(objectSelector.equals("id")){

                    try {
                        return new Result(this.command, "article", articles.withId(secSelector));
                    }catch(NullPointerException n){
                        return new Result(404, this.command, "Article not found");
                    }
                }else if(objectSelector.equals("update")){

                    if(content != null) {
                        if (content.getString("id") == null) {
                            content.put("id", secSelector);
                        }
                        Article updatedArticle = new Article(content);
                        try {
                            Article a = articles.withId(secSelector);
                            ArrayList<Event> events = updatedArticle.compare(a);
                            events.forEach(e -> e.setUser(config.getUser().getId()));
                            if (articles.update(a, updatedArticle)) {
                                TimelinesClient timelines = new TimelinesClient(config);
                                timelines.addEvents(secSelector, events);
                                return new Result(this.command, "events", events.toString());
                            }
                            return new Result(505, this.command, "Failed to update Article");
                        }catch (NullPointerException n){
                            return new Result(404, this.command, "Article not found");
                        }
                    }
                    return new Result(428, "No new Article JSON data provided");

                }else if(objectSelector.equals("delete")){

                    try{
                        if(articles.delete(articles.withId(secSelector))){
                            return new Result(200, this.command, "");
                        }
                        return new Result(505, this.command, "Failed to delete article");
                    }catch(NullPointerException n){
                        return new Result(404, this.command, "Article not found");
                    }

                }else if(objectSelector.equals("duplicate")){

                    try{
                        Article a = articles.withId(secSelector);
                        if(this.selectors.length > 3){
                            DomainsClient domains = new DomainsClient(config);
                            try{
                                if(articles.duplicate(a, domains.withAltId(terSelector))){
                                    return new Result(200, this.command, "");
                                }
                                return new Result(505, this.command, "Failed to duplicate article");
                            }catch(NullPointerException n){
                                return new Result(404, this.command, "Domain not found");
                            }
                        }
                    }catch(NullPointerException n){
                        return new Result(404, this.command, "Article not found");
                    }

                }else if(objectSelector.equals("move")){

                    try{
                        Article a = articles.withId(secSelector);
                        if(this.selectors.length > 3){
                            DomainsClient domains = new DomainsClient(config);
                            try{
                                if(articles.move(a, domains.withAltId(terSelector))){
                                    return new Result(200, this.command, "");
                                }
                                return new Result(505, this.command, "Failed to move article");
                            }catch(NullPointerException n){
                                return new Result(404, this.command, "Domain not found");
                            }
                        }
                    }catch(NullPointerException n){
                        return new Result(404, this.command, "Article not found");
                    }

                }else if(objectSelector.equals("verify")){

                    try{
                        if(articles.verify(articles.withId(secSelector))){
                            return new Result(200, this.command, "");
                        }
                        return new Result(505, this.command, "Failed to update Article");
                    }catch (NullPointerException n){
                        return new Result(404, this.command, "Article not found");
                    }
                }
                return new Result(404, this.command, "Unknown articles action");

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
                        if(objectSelector.equals("update")){

                            if(content != null){
                                if(content.getString("name") != null && content.getString("name").equals("Pinned")){
                                    return new Result(304, this.command, "'Pinned' is a reserved Collection name");
                                }
                                if(collections.update(c, content)){
                                    return new Result(200, this.command, "Collection Update");
                                }
                                return new Result(505, this.command, "Failed to update Collection");
                            }
                            return new Result(428, this.command, "Update content for Collection not provided");
                        } else if(objectSelector.equals("delete")){

                            if(collections.delete(c)){
                                return new Result(200, this.command, "Collection '" + secSelector + "' deleted");
                            }
                            return new Result(505, this.command, "Failed to delete group '" + secSelector + "'");
                        } else if(objectSelector.equals("id")){

                            return new Result(this.command, "collection", c);
                        }
                        return new Result(404, this.command, "Invalid collections command");

                    }catch (NullPointerException n){
                        return new Result(404, this.command, "Collection not found");
                    }
                }
                return new Result(428, "ID of Collection required to update");
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
                    if(newUser.getPermissions() == 0 && !newUser.getName().equals("") && !newUser.getEmail().equals("") && newUser.getName() != null && newUser.getEmail() != null) { //0 is permissions of new user
                        if (!users.userExistsWithEmail(newUser.getEmail())) {
                            if (users.createUser(newUser)) {
                                return new Result(this.command, "user", newUser);
                            }
                            return new Result(505, this.command, "Failed making user for email '" + newUser.getEmail() + "'");
                        }
                        return new Result(410, this.command, "User already exists for email '" + newUser.getEmail() + "'");
                    }
                    return new Result(428, this.command, "Name, email, phone number, and permissions are required to create a User");
                }
                return new Result(428, this.command, "New User details expected with JSON request body.");

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
                    if(mode.equals("batch")){

                        String uri = content.getString("uri");
                        double priority = (content.getDouble("priority") == null ? 1.01 : content.getDouble("priority"));
                        ArrayList<Article> extractedArticles = Parser.engines.batch(uri, priority);
                        if(extractedArticles != null){
                            if(content.getBoolean("insert") != null && content.getBoolean("insert")){
                                extractedArticles.forEach(article -> articles.create(article));
                            }
                            return new Result(this.command, "articles", extractedArticles);
                        }
                        return new Result(404, this.command, "No articles from batch upload");

                    }else if(mode.equals("wikipedia")){

                        Article wikiArticle = Parser.connectors.wikipedia(secSelector);
                        articles.create(wikiArticle);
                        return new Result(this.command, "article", wikiArticle);

                    }else if(mode.equals("yelp")){
                        String[] zips = content.getString("zips").split(",");
                        ArrayList<Article> yelps = null;
                        try {
                            yelps = Parser.connectors.yelp(zips, config);
                        } catch (UnsupportedEncodingException e) {
                            return new Result(505, this.command, "Failed to Yelp");
                        }
                        return new Result(this.command, "articles", yelps);

                    }else if(mode.equals("uri")){

                        String url = content.getString("url");
                        if(url != null && !url.equals("")){
                            Parser parser = new Parser();
                            Article parsed = Parser.engines.parse(url);
                            if(parser.getTraversable().size() > 1){
                                return new Result(this.command, "articles", parser.getTraversable());
                            }
                            return new Result(this.command, "article", parsed);
                        }
                        return new Result(428, this.command, "URI is required to parse in URI mode");
                    }
                }
                return new Result(428, this.command, "Please select a parser mode");
            }
            return new Result(428, this.command, "JSON http request body expected");
        }
        /*
         * Accessing Queue
         */
        else if(primarySelector.equals("queue")){
            QueueClient queue = new QueueClient(config);
            if(content != null){
                String uri = content.getString("uri");
                if(uri != null && !uri.equals("")){
                    //TODO parse and then add to queue
                }else{
                    if(queue.create(new Article(content))){
                        return new Result(this.command, "queue", content);
                    }else{
                        return new Result(505, this.command, "Failed to create queue");
                    }
                }
                return new Result(428, this.command, "URI is required to create a queue");
            }
            //TODO return user's queue
        }
        /*
         * Accessing Search
         */
        else if(primarySelector.equals("search")){
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
            return Search.execute(config, query, new Parameters(25, 0, "articles") );
        }
        /*
         * Authenticate an app for backend use to facilitate user authentication
         */
        else if(primarySelector.equals("authenticate")){

            if(this.selectors.length >= 3 && objectSelector.equals("app")){
                Authentication auth = new Authentication(secSelector);
                AuthenticationClient authentications = new AuthenticationClient(config);
                if(authentications.authenticate(auth)){
                    Telifie.console.out.string("App authenticated with ID: " + secSelector);
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
                UsersClient users = new UsersClient(config);
                if(users.userExistsWithEmail(objectSelector)){
                    User user = users.getUserWithEmail(objectSelector);
                    if(user.getPermissions() == 0){
                        //TODO lock user with email
                    }else if(user.getPermissions() >= 1){
                        if(users.sendCode(user)){
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
                return new Result(404, this.command, "No timeline found for " + objectSelector + "");
            }
            return new Result(428, this.command, "Please provide object ID to get timeline");

        }else if(primarySelector.equals("messaging")){

            //TODO sending/receiving messages

        }else if(primarySelector.equals("netstat")){

            //TODO diagnostics, logging, system stats, etc.
            return new Result(this.command, "netstat", "ok");

        }else if(primarySelector.equals("connectors")){

            ConnectorsClient connectors = new ConnectorsClient(config);
            if(content != null){

                Connector connector = new Connector(content);
                if(connector != null){ //Creating connector for user
                    connector.setUser(actingUser);
                    boolean connectorUsed = connectors.exists(connector);
                    if(connector.getId().equals("com.telifie.connectors.spotify")){
                        Spotify spotify = null;
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
                return new Result(404, this.command, "No connector found for " + objectSelector + "");
            }
            return new Result(428, this.command, "Please provide connector name to get");
        }
        return new Result(200, this.command, "No command received");
    }
}