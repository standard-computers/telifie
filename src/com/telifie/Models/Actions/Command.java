package com.telifie.Models.Actions;

import com.telifie.Models.*;
import com.telifie.Models.Clients.*;
import com.telifie.Models.Connector;
import com.telifie.Models.Connectors.Spotify;
import com.telifie.Models.Utilities.*;
import com.telifie.Models.Utilities.Parameters;
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
    private final String selectorsString;
    private final String targetDomain;
    private final String primarySelector;
    private final String[] selectors;

    public Command(String command){

        this.command = command;
        String[] spl = this.command.split("://");
        this.targetDomain = spl.length <= 1 || spl[0].equals("") || spl[0].contains("/") ? "telifie" : spl[0];
        this.selectorsString = this.command.replaceFirst(targetDomain + "://", "");
        this.selectors = this.selectorsString.split("(?!//)/");
        this.primarySelector = this.get(0).replaceAll("/", "");
    }

    public String getSelector(int index){
        return this.selectors[index];
    }

    public String get(int index){
        return this.getSelector(index);
    }

    public Result parseCommand(Configuration config){
        return this.parseCommand(config, null);
    }

    public Result parseCommand(Configuration config, Document content){

        String objectSelector = (this.selectors.length > 1 ? this.get(1) : "");

        if(primarySelector.equals("domains")){

            if(this.selectors.length >= 2){
                DomainsClient domains = new DomainsClient(config);
                if(objectSelector.equals("owner")){

                    ArrayList<Domain> foundDomains = domains.mine();
                    return new Result(this.command, "domains", foundDomains);
                }else if(objectSelector.equals("member")){ //Domains they're a member of

                    //TODO get domains that user is attached too
                    ArrayList<Domain> foundDomains = domains.forMember(config.getUser().getEmail());
                    return new Result(this.command, "domains", foundDomains);
                }else if(objectSelector.equals("create")){

                    if(content != null){
                        String domainName, domainIcon;
                        if((domainName = content.getString("name")) != null && (domainIcon = content.getString("icon")) != null && content.getInteger("permissions") != null){
                            Domain newDomain = new Domain(config.getAuthentication().getUser(), domainName, domainIcon, content.getInteger("permissions"));
                            if(domains.create(newDomain)){
                                return new Result(this.command, "domain", newDomain);
                            }
                            return new Result(505, this.command, "Failed to make domain");
                        }
                        return new Result(428, this.command, "Required JSON properties not provided");
                    }
                    return new Result(428, this.command, "JSON body expected");

                }else if(objectSelector.equals("delete")){

                    String domainId = this.get(2);
                    Domain subjectDomain = domains.withId(domainId);
                    if(subjectDomain.getOwner().equals(config.getAuthentication().getUser())){
                        if(domains.delete(domainId)){
                            return new Result(200, this.command, "Successfully deleted the domain");
                        }
                        return new Result(505, this.command, "Failed to delete domain");
                    }
                    return new Result(401, this.command, "This is not your domain");

                }else if(objectSelector.equals("protected")){

                    //TODO get protected domains
                }else if(objectSelector.equals("id")){
                    try{
                        String domainId = this.get(2);
                        Domain selectedDomain = domains.withAltId(domainId);
                        return new Result(this.command, "domain", selectedDomain);
                    }catch(NullPointerException n){
                        return new Result(404, this.command, "Domain not found");
                    }
                }else if(this.selectors.length >= 3){

                    String domainId = this.get(1);
                    Domain domain = domains.withId(domainId);
                    String command = this.get(2);
                    if(command.equals("users")){

                        if(this.selectors.length >= 4){
                            String action = this.get(3);
                            if(content != null){

                                ArrayList<Member> members = new ArrayList<>();
                                ArrayList<Document> mems = (ArrayList<Document>) content.getList("users", Document.class);
                                if (mems != null) {
                                    for (Document doc : mems) {
                                        members.add(new Member(doc));
                                    }
                                }else{
                                    return new Result(428, this.command, "JSON body expected");
                                }

                                if(action.equals("add")){

                                    if(domains.addUsers(domainId, members)){
                                        return new Result(200, this.command, "Added " + members.size() + " user(s) to domain");
                                    }
                                    return new Result(505, this.command, "Failed adding " + members.size() + " user(s) to domain");
                                }else if(action.equals("remove")){

                                    if(domains.removeUsers(domainId, members)){
                                        return new Result(200, this.command, "Removed " + members.size() + " user(s) from domain");
                                    }
                                    return new Result(505, this.command, "Failed removing " + members.size() + " user(s) from domain");
                                }else if(action.equals("update")) {

                                    if (domains.updateUsers(domainId, members)) {
                                        return new Result(200, this.command, "Updated " + members.size() + " user(s) in domain");
                                    }
                                    return new Result(505, this.command, "Failed to update user in domain");
                                }
                                return new Result(428, this.command, "Bad domain user action");
                            }
                            return new Result(428, this.command, "JSON body expected");
                        }
                        return new Result(this.command, "users", domain.getUsers());
                    }else if(command.equals("transfer")){

                        //TODO Transfer domain given usersEmail and ID given in JSON body
                    }else{
                        Domain selectedDomain = domains.withId(command);
                        return new Result(this.command, "domain", selectedDomain);
                    }
                }
                //TODO Get public domains for guests
                return new Result(404, this.command, "Invalid domains selector");
            }
            return new Result(200, this.command, "Invalid command for domains");
        }
        /*
         * Accessing Articles
         */
        else if(primarySelector.equals("articles")){

            if(this.selectors.length >= 3){
                ArticlesClient articles = new ArticlesClient(config);
                String articleId = this.get(2).trim();
                if(objectSelector.equals("id")){

                    try {
                        Article article = articles.get(articleId);
                        return new Result(this.command, "article", article);
                    }catch(NullPointerException n){
                        return new Result(404, this.command, "Article not found");
                    }
                }else if(objectSelector.equals("update")){

                    if(content != null) {
                        if (content.getString("id") == null) {
                            content.put("id", articleId);
                        }
                        Article updatedArticle = new Article(content);
                        if (config.getUser().getPermissions() >= 12 && this.targetDomain.equals("telifie")) { //Update Article in Public Domain
                            Article ogArticle = articles.get(articleId);
                            ArrayList<Event> events = updatedArticle.compare(ogArticle);
                            for (int i = 0; i < events.size(); i++) {
                                events.get(i).setUser(config.getUser().getId());
                            }
                            if (articles.update(ogArticle, updatedArticle)) {
                                TimelinesClient timelines = new TimelinesClient(config);
                                timelines.addEvents(articleId, events);

                                //TODO Decide to return changes made to article or return new article
                                return new Result(this.command, "events", events.toString());
                            }
                            return new Result(505, this.command, "Failed to update Article");
                        }
                        //Check permissions of user in Domains
                        //TODO, share changes with data team for approval and change status on Article
                        return new Result(401, this.command, "Insufficient permissions to update Article in Public Domain");
                    }
                    return new Result(428, "No new Article JSON data provided");

                }else if(objectSelector.equals("delete")){

                    if(config.getUser().getPermissions() >= 12 && this.targetDomain.equals("telifie")){ //Update Article in Public Domain
                        if(articles.delete(articleId)){
                            return new Result(200, this.command, "");
                        }
                        return new Result(505, this.command, "Failed to delete Article");
                    }
                    //TODO, share changes with data team for approval and change status on Article
                    return new Result(401, this.command, "Insufficient permissions to delete article in domain");

                }else if(objectSelector.equals("move")){

                }else if(objectSelector.equals("verify")){

                    if (config.getUser().getPermissions() >= 12 && this.targetDomain.equals("telifie")) { //Update Article in Public Domain
                        if(articles.verify(articleId)){
                            return new Result(200, this.command, "");
                        }
                        return new Result(505, this.command, "Failed to update Article");
                    }
                    //TODO, share changes with data team for approval and change status on Article
                    return new Result(401, this.command, "Insufficient permissions to update in domain");
                }

                return new Result(404, this.command, "Unknown Articles action command");
            }else if(objectSelector.equals("create")){

                if(content != null){
                    User user = config.getUser();
                    if(this.targetDomain.equals("telifie")){ //Make sure that the user has the permissions
                        if(user.getPermissions() < 12){
                            return new Result(403, this.command, "Insufficient permissions to publish to domain");
                        }
                    }else{
                        //TODO User blocking when they shouldn't be putting into domain
                        DomainsClient domains = new DomainsClient(config);
                        Domain domain = domains.withAltId(this.targetDomain);
                        if(domain == null){
                            return new Result(404, this.command, "Domain not found");
                        }
                        domain.setUri(config.getDomain().getUri());
                        config.setDomain(domain);
                    }
                    try {
                        Article newArticle = new Article(content);
                        ArticlesClient articles = new ArticlesClient(config);
                        if(articles.create(newArticle)){
                            return new Result(this.command, "article", newArticle);
                        }
                        return new Result(505, "Failed to create Article");
                    }catch(JSONException e){
                        return new Result(505, this.command, "Malformed Article JSON data provided");
                    }
                }
                return new Result(428, "Precondition Failed. No new Article provided (JSON.article) as body");
            }
            //Return stats of Articles in domain (summary)
            return new Result(this.command,"stats", "");
        }
        /*
         * Accessing Collections
         * Save, unsave, create, delete, update,
         */
        else if(primarySelector.equals("collections")){

            String collectionId = (this.selectors.length == 3 ? this.get(2) : null);
            CollectionsClient collections = new CollectionsClient(config);
            if(this.selectors.length >= 4){ //Ensure correct argument count

                String articleId = this.get(3);
                if(objectSelector.equals("save")){

                    if(collections.save(articleId, articleId)){
                        return new Result(200, this.command, "Saved Article");
                    }
                    return new Result(505, this.command, "Failed to save Article");
                } else if(objectSelector.equals("unsave")){

                    if(collections.unsave(articleId, articleId)){
                        return new Result(200, this.command, "Unsaved Article");
                    }
                    return new Result(505, this.command, "Failed to unsave Article");
                }
                return new Result(404, this.command, "Invalid favorites command");
            } else if(objectSelector.equals("update")){

                String groupId = (this.selectors.length == 3 ? this.get(2) : null);
                if(groupId != null){

                    if(content != null){
                        if(content.getString("name") != null && content.getString("name").equals("Pinned")){
                            return new Result(304, this.command, "'Pinned' is a reserved Collection name");
                        }
                        if(collections.update(groupId, content)){
                            return new Result(200, this.command, "Collection Update");
                        }
                        return new Result(505, this.command, "Failed to update Collection");
                    }
                    return new Result(428, this.command, "Update content for Collection not provided");
                }
                return new Result(428, "ID of Collection required to update");
            } else if(objectSelector.equals("create")){

                if(content != null){ //Creating Collection with JSON content
                    Collection newCollection = new Collection(content);
                    Collection createdCollection = collections.create(newCollection);
                    if(createdCollection != null){
                        return new Result(this.command, "collection", createdCollection);
                    }
                    return new Result(505, this.command, "Failed to make Collection with JSON");
                }
                return new Result(428, "JSON request body expected");
            } else if(objectSelector.equals("delete")){

                if(collectionId != null){
                    if(collections.delete(collectionId)){
                        return new Result(200, this.command, "Collection '" + collectionId + "' deleted");
                    }
                    return new Result(505, this.command, "Failed to delete group '" + collectionId + "'");
                }
                return new Result(428, "Name of new group required.");
            } else if(objectSelector.equals("id")){

                if(collectionId != null){
                    try{
                        Collection collection = collections.get(config.getAuthentication().getUser(), collectionId);
                        return new Result(this.command, "collection", collection);
                    }catch(NullPointerException n){
                        return new Result(404, this.command, "Collection not found with ID '" + collectionId + "'");
                    }
                }
                return new Result(428, this.command, "Collection ID is required to get");
            }
            ArrayList<Collection> usersCollections = collections.forUser(config.getAuthentication().getUser());
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
                    if(this.get(2).equals("theme")){
                        Theme theme = new Theme(content);
                        if(users.updateUserTheme(users.getUserWithEmail(userEmail), theme)){
                            return new Result(this.command, "user", changedUser);
                        }
                        return new Result(400, "Bad Request");
                    }else if(this.get(2).equals("photo")){
                        
                        if(content != null){
                            if(content.getString("photo") != null) {
                                String photoUri = content.getString("photo");
                                if (users.updateUserPhoto(changedUser, photoUri)) {
                                    return new Result(200, this.command, "User photo updated");
                                }
                                return new Result(505, this.command, "Failed to update user photo");
                            }
                            return new Result(428, this.command, "JSON parameter 'photo' expected");
                        }
                        return new Result(428, this.command, "JSON request body expected");
                    }
                    return new Result(404, this.command, "Invalid users update command");
                }
                return new Result(404, this.command, "Invalid command received");
            } else if(objectSelector.equals("create")){

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
            Matcher matcher = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE).matcher(this.get(1));
            if (matcher.matches()) {
                if(users.userExistsWithEmail(this.get(1))){
                    User found = users.getUserWithEmail(this.get(1));
                    return new Result(this.command, "user", found);
                }
                return new Result(404, this.command, "User not found");
            }
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
                            if(content.getBoolean("insert") != null && content.getBoolean("insert") == true){
                                extractedArticles.forEach(article -> articles.create(article));
                            }
                            return new Result(this.command, "articles", extractedArticles);
                        }
                        return new Result(404, this.command, "No articles from batch upload");

                    }else if(mode.equals("dictionary")){

                        if(content != null && content.getString("words") != null){
                            String[] words = content.getString("words").split(" ");
                            new Parser.index();
                            Parser.index.dictionary dict = new Parser.index.dictionary(Telifie.Languages.ENGLISH);
                            dict.add(words);
                            return new Result(200, this.command, "Words added to dictionary");
                        }
                        return new Result(428, this.command, "JSON http request body expected");

                    }else if(mode.equals("wikipedia")){

                        String wikiTitle = this.get(2);
                        Article wikiArticle = Parser.connectors.wikipedia(wikiTitle);
                        articles.create(wikiArticle);
                        return new Result(this.command, "article", wikiArticle);

                    }else if(mode.equals("yelp")){
                        String[] zips = content.getString("zips").split(",");
                        ArrayList<Article> yelps = null;
                        try {
                            yelps = Parser.connectors.yelp(zips, config);
                        } catch (UnsupportedEncodingException e) {
                            return new Result(505, this.command, "Failed to parse Yelp");
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

            if(this.selectors.length >= 2) {

                ArticlesClient articles = new ArticlesClient(config);
                String uri = content.getString("uri");
                if (uri == null || uri.equals("")) {
                    return new Result(410, this.command, "No URI provided as {uri : URI}");
                }
                if (articles.get(new Document("link", uri)).size() > 0) { //Article already exists
                    return new Result(410, this.command, "Article already exists");
                }
                QueueClient queue = new QueueClient(config);
                Article queued = queue.add(uri);
                if (queued != null) {
                    return new Result(this.command, "article", queued);
                }
                return new Result(505, "There was an error parsing the queue request");
            }
            return new Result(404, "Invalid queue command");
        }
        /*
         * Accessing Search
         */
        else if(primarySelector.equals("search")){
            String query = this.selectorsString.replaceFirst("search/", "");
            DomainsClient domains = new DomainsClient(config);
            Domain domain = domains.withAltId(this.targetDomain);
            if(domain == null){
                return new Result(404, this.command, "Domain not found");
            }
            domain.setUri(config.getDomain().getUri());
            config.setDomain(domain);
            if(content != null){
                try {
                    Parameters params = new Parameters(content);
                    return Search.execute(config, query, params);
                }catch(NullPointerException n){
                    return new Result(428, this.command, "JSON http request body expected");
                }
            }
            if(!this.targetDomain.equals("telifie") && !this.targetDomain.equals("")){
                config.setDomain(config.getDomain().setName(this.targetDomain));
            }
            return Search.execute(config, query, new Parameters(25, 0, "articles") );
        }
        /*
         * Authenticate an app for backend use to facilitate user authentication
         */
        else if(primarySelector.equals("authenticate")){

            if(this.selectors.length >= 3 && this.get(1).equals("app")){
                String appId = this.get(2);
                Authentication auth = new Authentication(appId);
                AuthenticationClient authentications = new AuthenticationClient(config);
                if(authentications.authenticate(auth)){
                    Telifie.console.out.string("App authenticated with ID: " + appId);
                    Telifie.console.out.string("One-time see authentication information below:");
                    Telifie.console.out.string("==============================================");
                    Telifie.console.out.string(auth.toJson().toString(4));
                    Telifie.console.out.string("==============================================");
                    return new Result(this.command, "authentication", auth.toJson());
                }else{
                    return new Result(505, "Failed creating app authentication");
                }
            }
        }
        /*
         * Connect: Creating or logging in user
         */
        else if(primarySelector.equals("connect")){

            if(this.selectors.length >= 2){
                String email = this.get(1);
                UsersClient users = new UsersClient(config);
                if(users.userExistsWithEmail(email)){
                    User user = users.getUserWithEmail(email);
                    if(user.getPermissions() == 0){

//                        ConnectorsClient connectors = new ConnectorsClient(config);
//                        Connector sendgrid = connectors.getConnector("com.telifie.connectors.SendGrid");
//                        if(sendgrid != null){
//                            SGrid sg = new SGrid(sendgrid);
//                            String code = Telifie.tools.make.shortEid();
//                            if(users.lock(user, code)){
//                                if(sg.sendAuthenticationCode(user.getEmail(), code)){
//
//                                }
//                                return new Result(505, this.command, "Failed to email code through SendGrid");
//                            }
//                            return new Result(505, this.command, "Failed to lock users account");
//                        }
//                        return new Result(410, "Please create SendGrid Connector information");

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
                String email = this.get(1), code = Telifie.tools.make.md5(this.get(2));
                UsersClient users = new UsersClient(config);
                User user = users.getUserWithEmail(email);
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
                String objectId = this.get(1);
                TimelinesClient timelines = new TimelinesClient(config);
                Timeline timeline = timelines.getTimeline(objectId);
                if(timeline != null){
                    return new Result(this.command, "timeline", timeline);
                }
                return new Result(404, this.command, "No timeline found for " + objectId + "");
            }
            return new Result(428, this.command, "Please provide object ID to get timeline");

        }else if(primarySelector.equals("messaging")){

            //TODO sending/receiving messages

        }else if(primarySelector.equals("netstat")){ //Pinging server for status

            //TODO diagnostics, logging, system stats, etc.

        }else if(primarySelector.equals("connectors")){

            ConnectorsClient connectors = new ConnectorsClient(config);
            if(content != null){

                Connector connector = new Connector(content);
                if(connector != null){ //User providing credentials with connector request to parse
                    connector.setUser(config.getAuthentication().getUser());
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
                    }
                }
                return new Result(428, this.command, "JSON body expected to create connector");
            }
            if(this.selectors.length >= 2){
                String connectorId = this.get(1);
                if(connectorId.equals("connected")){
                    return new Result(this.command, "connectors", connectors.mine());
                }
                Connector connector = connectors.getConnector(connectorId);
                if(connector != null){
                    return new Result(this.command, "connector", connector);
                }
                return new Result(404, this.command, "No connector found for " + connectorId + "");
            }
            return new Result(428, this.command, "Please provide connector name to get");
        }
        return new Result(200, this.command, "No command received");
    }
}