package com.telifie.Models.Actions;

import com.telifie.Models.*;
import com.telifie.Models.Clients.*;
import com.telifie.Models.Connectors.SGrid;
import com.telifie.Models.Connectors.Connector;
import com.telifie.Models.Utilities.*;
import com.telifie.Models.Utilities.Parameters;
import org.bson.Document;
import org.json.JSONException;
import org.json.JSONObject;
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
        /*
         * Accessing Domains
         */
        if(primarySelector.equals("domains")){

            if(this.selectors.length >= 2){
                if(objectSelector.equals("owner")){ //Get domains for owner /domains/owner/[USER_ID]

                    DomainsClient domains = new DomainsClient(config);
                    ArrayList<Domain> foundDomains = domains.getOwnedDomains(this.get(2));
                    return new Result(this.command, "domains", foundDomains);
                }else if(objectSelector.equals("member")){ //Get domains for user /domains/member/[USER_ID]

                    //TODO get domains that user is attached too
                    DomainsClient domains = new DomainsClient(config);
                    ArrayList<Domain> foundDomains = domains.getDomainsForUser(this.get(2));
                    return new Result(this.command, "domains", foundDomains);
                }else if(objectSelector.equals("create")){

                    if(content != null){
                        String domainName, domainIcon;
                        if((domainName = content.getString("name")) != null && (domainIcon = content.getString("icon")) != null && content.getInteger("permissions") != null){
                            Domain newDomain = new Domain(config.getAuthentication().getUser(), domainName, domainIcon, content.getInteger("permissions"));
                            DomainsClient domains = new DomainsClient(config);
                            if(domains.create(newDomain)){
                                return new Result(this.command, "domain", newDomain);
                            }
                            return new Result(505, this.command, "\"Failed to make domain\"");
                        }
                        return new Result(428, this.command, "\"Required JSON properties not provided\"");
                    }
                    return new Result(428, this.command, "\"JSON body expected\"");

                }else if(objectSelector.equals("delete")){

                    String domainId = this.get(2);
                    DomainsClient domains = new DomainsClient(config);
                    Domain subjectDomain = domains.getWithId(domainId);
                    if(subjectDomain.getOwner().equals(config.getAuthentication().getUser())){ //Requesting user is owning user
                        if(domains.delete(config.getAuthentication().getUser(), domainId)){
                            return new Result(200, this.command, "\"Successfully deleted the domain\"");
                        }
                        return new Result(505, this.command, "\"Failed to delete domain\"");
                    }
                    return new Result(401, this.command, "\"This is not your domain!!!!!\"");

                }else if(objectSelector.equals("public")){ //Get public domains

                    //TODO get public domains
                }else if(objectSelector.equals("protected")){ //Get public domains

                    //TODO get protected domains
                }

                if(this.get(1) != null){

                    String domainId = this.get(1);
                    DomainsClient domains = new DomainsClient(config);
                    try{
                        Domain selectedDomain = domains.getWithId(domainId);
                        return new Result(this.command, "domain", selectedDomain);
                    }catch(NullPointerException n){
                        return new Result(404, this.command, "\"Domain with id '" + domainId + "' not found\"");
                    }
                }
                return new Result(404, this.command, "\"Invalid domains selector\"");
            }
            return new Result(200, this.command, "\"Invalid command for domains\"");
        }
        /*
         * Accessing Articles
         */
        else if(primarySelector.equals("articles")){

            UsersClient users = new UsersClient(config);
            if(this.selectors.length >= 3){ //Provide [ACTION]/[ARTICLE_ID]
                ArticlesClient articles = new ArticlesClient(config);
                String articleId = this.get(2).trim();
                if(objectSelector.equals("id")){ //Specifying Article with ID

                    try {
                        Article article = articles.get(articleId);
                        return new Result(this.command, "article", article);
                    }catch(NullPointerException n){

                        return new Result(404, this.command, "\"Article not found\"");
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
                            return new Result(505, this.command, "\"Failed to update Article\"");
                        }
                        //Check permissions of user in Domains
                        //TODO, share changes with data team for approval and change status on Article
                        return new Result(401, this.command, "\"Insufficient permissions to update Article in Public Domain\"");
                    }
                    return new Result(428, "\"No new Article JSON data provided\"");

                }else if(objectSelector.equals("delete")){

                    if(config.getUser().getPermissions() >= 12 && this.targetDomain.equals("telifie")){ //Update Article in Public Domain
                        if(articles.delete(articleId)){
                            return new Result(200, this.command, "\"\"");
                        }
                        return new Result(505, this.command, "\"Failed to delete Article\"");
                    }

                    //Check permissions of user in Domains
                    //TODO, share changes with data team for approval and change status on Article
                    return new Result(401, this.command, "\"Insufficient permissions to delete article in domain\"");

                }else if(objectSelector.equals("move")){

                }else if(objectSelector.equals("verify")){

                    if (config.getUser().getPermissions() >= 12 && this.targetDomain.equals("telifie")) { //Update Article in Public Domain
                        if(articles.verify(articleId)){
                            return new Result(200, this.command, "\"\"");
                        }
                        return new Result(505, this.command, "\"Failed to update Article\"");
                    }

                    //Check permissions of user in Domains
                    //TODO, share changes with data team for approval and change status on Article
                    return new Result(401, this.command, "\"Insufficient permissions to update in domain\"");
                }

                return new Result(404, this.command, "\"Unknown Articles action command\"");
            }else if(objectSelector.equals("create")){

                if(content != null){
                    User user = config.getUser();
                    if(this.targetDomain.equals("telifie") && user.getPermissions() < 12){ //Make sure that the user has the permissions
                        return new Result(403, this.command, "\"Insufficient permissions to publish to domain\"");
                    }
                    try {
                        Article newArticle = new Article(content);
                        ArticlesClient articles = new ArticlesClient(config);
                        if(articles.create(newArticle)){
                            return new Result(this.command, "article", newArticle);
                        }
                        return new Result(505, "Failed to create Article");
                    }catch(JSONException e){
                        return new Result(505, this.command, "\"Malformed Article JSON data provided\"");
                    }
                }
                return new Result(428, "\"Precondition Failed. No new Article provided (JSON.article) as body\"");
            }
            //Return stats of Articles in domain (summary)
            return new Result(this.command,"stats", "");
        }
        /*
         * Accessing Groups
         * Save, unsave, create, delete, update,
         */
        else if(primarySelector.equals("groups")){

            if(this.selectors.length >= 4){ //Ensure correct argument count

                String groupId = this.get(2), articleId = this.get(3);
                CollectionsClient groups = new CollectionsClient(config);

                //Saves Article to Collection given /groups/save/[GROUP_ID]/[ARTICLE_ID]
                if(objectSelector.equals("save")){

                    if(groups.save(config.getAuthentication().getUser(), groupId, articleId)){
                        return new Result(200, this.command, "\"Saved Article\"");
                    }
                    return new Result(505, this.command, "\"Failed to save Article\"");
                }
                //Removes article from group given /groups/unsave/[GROUP_NAME]/[ARTICLE_ID]
                else if(objectSelector.equals("unsave")){

                    if(groups.unsave(config.getAuthentication().getUser(), groupId, articleId)){
                        return new Result(200, this.command, "\"Unsaved Article\"");
                    }
                    return new Result(505, this.command, "\"Failed to unsave Article\"");
                }
                return new Result(404, this.command, "\"Invalid favorites command\"");
            }
            //Updates Collection given JSON POST. See reference
            else if(objectSelector.equals("update")){

                String groupId = (this.selectors.length == 3 ? this.get(2) : null);
                if(groupId != null){

                    if(content != null){
                        //Cannot make a folder named $pinned as its reserved
                        if(content.getString("name") != null && content.getString("name").equals("Pinned")){
                            return new Result(304, this.command, "\"'Pinned' is a reserved Collection name\"");
                        }

                        CollectionsClient groups = new CollectionsClient(config);
                        if(groups.update(groupId, content)){
                            return new Result(200, this.command, "\"Collection Update\"");
                        }
                        return new Result(505, this.command, "\"Failed to update Collection\"");
                    }
                    return new Result(428, this.command, "\"Update content for Collection not provided\"");
                }
                return new Result(428, "\"ID of Collection required to update\"");
            }
            //Creates Collection given /groups/create/[GROUP_NAME]
            else if(objectSelector.equals("create")){

                if(content != null){ //Creating Collection with JSON content
                    Collection newCollection = new Collection(content);
                    CollectionsClient groups = new CollectionsClient(config);
                    Collection createdCollection = groups.create(config.getAuthentication().getUser(), newCollection);
                    if(createdCollection != null){
                        return new Result(this.command, "group", createdCollection);
                    }
                    return new Result(505, this.command, "\"Failed to make Collection with JSON\"");
                }
                return new Result(428, "\"JSON request body expected\"");
            }
            // Deletes folder given /groups/delete/[GROUP_ID]
            else if(objectSelector.equals("delete")){

                String groupId = (this.selectors.length == 3 ? this.get(2) : null);
                if(groupId != null){
                    CollectionsClient groups = new CollectionsClient(config);
                    if(groups.delete(config.getAuthentication().getUser(), groupId)){
                        return new Result(200, this.command, "\"Collection '" + groupId + "' deleted\"");
                    }
                    return new Result(505, this.command, "\"Failed to delete group '" + groupId + "'\"");
                }
                return new Result(428, "Name of new group required.");
            }
            /*
             * Returns Collection given /groups/id/[GROUP_ID]
             */
            else if(objectSelector.equals("id")){

                String groupId = this.get(2);
                CollectionsClient groups = new CollectionsClient(config);
                if(groupId != null){
                    try{
                        Collection collection = groups.get(config.getAuthentication().getUser(), groupId);
                        return new Result(this.command, "collection", collection);
                    }catch(NullPointerException n){
                        return new Result(404, this.command, "\"Collection not found with ID '" + groupId + "'\"");
                    }
                }
                return new Result(428, this.command, "\"Collection ID is required to get\"");
            }
            CollectionsClient groups = new CollectionsClient(config);
            ArrayList<Collection> usersCollections = groups.groupsForUser(config.getAuthentication().getUser());
            return new Result(this.command, "groups", usersCollections);
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
                        return new Result(400, "\"Bad Request\"");
                    }else if(this.get(2).equals("photo")){
                        
                        if(content != null){
                            if(content.getString("photo") != null) {
                                String photoUri = content.getString("photo");
                                if (users.updateUserPhoto(changedUser, photoUri)) {
                                    return new Result(200, this.command, "\"User photo updated\"");
                                }
                                return new Result(505, this.command, "\"Failed to update user photo\"");
                            }
                            return new Result(428, this.command, "\"JSON parameter 'photo' expected\"");
                        }
                        return new Result(428, this.command, "\"JSON request body expected\"");
                    }
                    return new Result(404, this.command, "\"Invalid users update command\"");
                }
                return new Result(404, this.command, "\"Invalid command received\"");
            }else if(objectSelector.equals("create")){ //Creating User

                if(content != null){
                    User newUser = new User(
                            content.getString("email"),
                            content.getString("name"),
                            content.getString("phone")
                    );
                    if(newUser.getPermissions() == 0 && !newUser.getName().equals("") && !newUser.getEmail().equals("") && newUser.getName() != null && newUser.getEmail() != null) { //0 is permissions of new user
                        if (!users.userExistsWithEmail(newUser.getEmail())) {
                            if (users.createUser(newUser)) {
                                return new Result(this.command, "user", newUser);
                            }
                            return new Result(505, this.command, "\"Failed making user for email '" + newUser.getEmail() + "'\"");
                        }
                        return new Result(410, this.command, "\"User already exists for email '" + newUser.getEmail() + "'\"");
                    }
                    return new Result(428, this.command, "\"Name, email, phone number, and permissions are required to create a User\"");
                }
                return new Result(428, this.command, "\"New User details expected with JSON request body.\"");
            }
            Matcher matcher = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE).matcher(this.get(1));
            if (matcher.matches()) {
                if(users.userExistsWithEmail(this.get(1))){ //Check if user with email, return user
                    User found = users.getUserWithEmail(this.get(1));
                    return new Result(this.command, "user", found);
                }else { //No valid user action command provided
                    return new Result(404, this.command, "\"User not found\"");
                }
            }
        }
        /*
         * Accessing Parser
         */
        else if(primarySelector.equals("parser")){ //Accessing parser

            ArticlesClient articles = new ArticlesClient(config);
            if(content != null){
                String mode = content.getString("mode");
                if(mode != null){

                    if(mode.equals("batch")){ //Importing CSV as batch of Articles

                        String uri = this.command.replaceFirst("parser/batch", "");
                        ArrayList<Article> extractedArticles = Parser.engines.batch(uri, ",");
                        if(extractedArticles != null){
                            return new Result(this.command, "articles", extractedArticles);
                        }
                        return new Result(404, this.command, "\"No articles from batch upload\"");

                    }else if(mode.equals("dictionary")){ //Managing index dictionary for parsing

                        if(content != null && content.getString("words") != null){
                            String[] words = content.getString("words").split(" ");
                            new Parser.index();
                            Parser.index.dictionary dict = new Parser.index.dictionary(Telifie.Languages.ENGLISH);
                            dict.add(words);
                            return new Result(200, this.command, "\"Words added to dictionary\"");
                        }
                        return new Result(428, this.command, "\"JSON http request body expected\"");

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
                            return new Result(505, this.command, "\"Failed to parse Yelp\"");
                        }
                        return new Result(this.command, "articles", yelps);

                    }else if(mode.equals("uri")){ //Parsing URL or file with URI

                        String url = content.getString("url");
                        if(url != null && !url.equals("")){
                            Parser parser = new Parser();
                            Article parsed = Parser.engines.parse(url);
                            if(parser.getTraversable().size() > 1){
                                return new Result(this.command, "articles", parser.getTraversable());
                            }
                            return new Result(this.command, "article", parsed);
                        }
                        return new Result(428, this.command, "\"URI is required to parse in URI mode\"");
                    }
                }
                return new Result(428, this.command, "\"Please select a parser mode\"");
            }
            return new Result(428, this.command, "\"JSON http request body expected\"");
        }
        /*
         * Accessing Queue
         */
        else if(primarySelector.equals("queue")){

            if(this.selectors.length >= 2) {
                //User is uploading content to queue
                String uri = content.getString("uri");
                String connector = (content.getString("connector") == null ? null : content.getString("connector"));
                if (connector == null || connector.equals("")) {
                    //TODO return draft article
                    if (uri == null || uri.equals("")) {
                        return new Result(410, this.command, "\"No URI provided as {\"uri\" : \"URI\"}\"");
                    }
                    //Check if an article exists for this already
                    ArticlesClient articles = new ArticlesClient(config);
                    if (articles.get(new Document("link", uri)).size() > 0) {

                    } else {
                        QueueClient queue = new QueueClient(config);
                        Article queued = queue.add(uri);
                        if (queued != null) {
                            return new Result(this.command, "article", queued);
                        }
                        return new Result(505, "\"There was an error parsing the queue request\"");
                    }
                    return new Result(200, uri, "\"Queued\"");
                }
            }
            return new Result(404, "\"Invalid queue command\"");

        }
        /*
         * Accessing Search
         */
        else if(primarySelector.equals("search")){
            String query = this.selectorsString.replaceFirst("search/", "");
            if(content != null){
                try {
                    Parameters params = new Parameters(content);
                    return Search.execute(config, query, params);
                }catch(NullPointerException n){
                    return new Result(428, this.command, "\"JSON http request body expected\"");
                }
            }
            if(!this.targetDomain.equals("telifie") && !this.targetDomain.equals("")){
                config.setDomain(config.getDomain().setName(this.targetDomain));
            }
            return Search.execute(config, query, new Parameters(25, 0, "articles") );
        }
        /*
         * Accessing HttpServer
         */
        else if(primarySelector.equals("server")){ //Accessing server
            if(this.selectors.length >= 2){
                  if(this.get(2).equals("http")){
                    Telifie.console.out.string("Starting HTTP server [TESTING PURPOSES ONLY]...");
                    try{
                        new HttpServer();
                    }catch(Exception e){
                        throw new RuntimeException(e);
                    }
                    return new Result(this.command, "server", "HTTP");
                }
            }
            try{
                new Server();
            }catch(Exception e){
                throw new RuntimeException(e);
            }
        }
        /*
         * Authenticate an app for backend use to facilitate user authentication
         */
        else if(primarySelector.equals("authenticate")){ //authentication

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
                    return new Result(505, "\"Failed creating app authentication\"");
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
                    if(user.getPermissions() == 0){ //Email needs verified
                        ConnectorsClient connectors = new ConnectorsClient();
                        Connector sendgrid = connectors.getConnector("SendGrid");
                        if(sendgrid != null){
                            SGrid sg = new SGrid(sendgrid);
                            String code = Telifie.tools.make.shortEid();
                            if(users.lock(user, code)){
                                if(sg.sendAuthenticationCode(user.getEmail(), code)){

                                }
                                return new Result(505, this.command, "\"Failed to email code through SendGrid\"");
                            }
                            return new Result(505, this.command, "\"Failed to lock users account\"");
                        }
                        return new Result(410, "\"Please create SendGrid Connector information\"");

                    }else if(user.getPermissions() >= 1){ //Phone needs verified

                        if(users.sendCode(user)){
                            return new Result(200, "\"Authentication code sent\"");
                        }
                        return new Result(501, "\"Failed to send code\"");
                    }
                }
                return new Result(404, "\"Account not found\"");
            }
            return new Result(428, "\"Need more params\"");
        }
        /*
         * Verify: Unlocking account using one-time 2fa token
         */
        else if(primarySelector.startsWith("verify")){ //Provided /verify/[USER_EMAIL]/[VERIFICATION_CODE]

            if(this.selectors.length >= 3){
                String email = this.get(1), code = Telifie.tools.make.md5(this.get(2));
                UsersClient users = new UsersClient(config);
                User user = users.getUserWithEmail(email);
                if(user.hasToken(code)){ //Check if user has the right code
                    if(user.getPermissions() == 0 || user.getPermissions() == 1){ //User was verifying email
                        users.upgradePermissions(user);
                        user.setPermissions(user.getPermissions() + 1); //For request return purposes. It has been updated
                    }

                    Authentication auth = new Authentication(user);
                    AuthenticationClient auths = new AuthenticationClient(config);
                    auths.authenticate(auth);
                    JSONObject json = new JSONObject(user.toString());
                    json.put("authentication", auth.toJson());

                    return new Result(this.command, "user", json);
                }
                return new Result(403, "\"Invalid verification code provided\"");
            }
            return new Result(404, this.command, "\"Invalid command received\"");
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
                return new Result(404, this.command, "\"No timeline found for " + objectId + "\"");
            }
            return new Result(428, this.command, "\"Please provide object ID to get timeline\"");

        }else if(primarySelector.equals("messaging")){

            //TODO sending/receiving messages

        }else if(primarySelector.equals("netstat")){ //Pinging server for status

            //TODO diagnostics, logging, system stats, etc.

        }else if(primarySelector.equals("connectors")){

            if(this.selectors.length > 1){

                if(objectSelector.equals("create")){

                    if(content != null){

                        Connector newConnector = new Connector(content);
                        if(new ConnectorsClient().create(newConnector)){

                            return new Result(
                                    this.command,
                                    "connector",
                                    newConnector
                            );
                        }
                        return new Result(505, this.command, "\"Failed to create Connector\"");
                    }
                    return new Result(428, this.command, "\"JSON body expected to create connector\"");
                }
                new Result(404, this.command, "\"Invalid connectors action provided\"");
            }

            //Gets all available Connectors for use
            //TODO Do not get files everytime, do this once when the server starts
            ConnectorsClient connectors = new ConnectorsClient();
            ArrayList<Connector> all = connectors.getConnectors();
            return new Result(this.command, "connectors", all);
        }
        return new Result(200, this.command, "\"No command received\"");
    }
}