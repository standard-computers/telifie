package com.telifie.Models.Utilities;

import com.telifie.Models.*;
import com.telifie.Models.Actions.Event;
import com.telifie.Models.Actions.Out;
import com.telifie.Models.Actions.Search;
import com.telifie.Models.Actions.Timeline;
import com.telifie.Models.Clients.*;
import com.telifie.Models.Connectors.Available.SGrid;
import com.telifie.Models.Connectors.Connector;
import org.bson.Document;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
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
        this.targetDomain = spl.length <= 1 || spl[0].equals("") ? "telifie" : spl[0];
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

                if(objectSelector.equals("owner")){ //Get domains for owner /domains/owner/[USER_ID]

                    DomainsClient domains = new DomainsClient(config.defaultDomain());
                    ArrayList<Domain> foundDomains = domains.getOwnedDomains(this.get(2));
                    return new Result(
                            this.command,
                            "domains",
                            foundDomains.toString(),
                            foundDomains.size()
                    );
                }else if(objectSelector.equals("member")){ //Get domains for user /domains/member/[USER_ID]

                    //TODO get domains that user is attached too
                    DomainsClient domains = new DomainsClient(config.defaultDomain());
                    ArrayList<Domain> foundDomains = domains.getDomainsForUser(this.get(2));
                    return new Result(
                            this.command,
                            "domains",
                            foundDomains.toString(),
                            foundDomains.size()
                    );
                }else if(objectSelector.equals("create")){

                    if(content != null){

                        String domainName, domainIcon;
                        if((domainName = content.getString("name")) != null && (domainIcon = content.getString("icon")) != null && content.getInteger("permissions") != null){

                            Domain newDomain = new Domain(config.getAuthentication().getUser(), domainName, domainIcon, content.getInteger("permissions"));
                            DomainsClient domains = new DomainsClient(config.defaultDomain());

                            if(domains.create(newDomain)){

                                return new Result(
                                        this.command,
                                        "domain",
                                        newDomain.toString(),
                                        1
                                );
                            }else{

                                return new Result(505, this.command, "\"Failed to make domain\"");
                            }
                        }else{

                            return new Result(428, this.command, "\"Required JSON properties not provided\"");
                        }
                    }else{

                        return new Result(428, this.command, "\"JSON body expected\"");
                    }
                }else if(objectSelector.equals("delete")){

                    String domainId = this.get(2);
                    DomainsClient domains = new DomainsClient(config.defaultDomain());
                    Domain subjectDomain = domains.getWithId(domainId);

                    if(subjectDomain.getOwner().equals(config.getAuthentication().getUser())){ //Requesting user is owning user

                        if(domains.delete(config.getAuthentication().getUser(), domainId)){

                            return new Result(200, this.command, "\"Successfully deleted the domain\"");
                        }else{

                            return new Result(505, this.command, "\"Failed to delete domain\"");
                        }
                    }else{

                        return new Result(401, this.command, "\"This is not your domain!!!!!\"");
                    }
                }else if(objectSelector.equals("public")){ //Get public domains

                    //TODO get public domains
                }else if(objectSelector.equals("protected")){ //Get public domains

                    //TODO get protected domains
                }else{

                    if(this.get(1) != null){

                        String domainId = this.get(1);
                        DomainsClient domains = new DomainsClient(config.defaultDomain());
                        try{

                            Domain selectedDomain = domains.getWithId(domainId);
                            return new Result(
                                    this.command,
                                    "domain",
                                    selectedDomain.toString(),
                                    1
                            );
                        }catch(NullPointerException n){

                            return new Result(404, this.command, "\"Domain with id '" + domainId + "' not found\"");
                        }
                    }else{

                        return new Result(404, this.command, "\"Invalid domains selector\"");
                    }
                }
            }else{

                return new Result(200, this.command, "\"Invalid command for domains\"");
            }
        }
        /*
         * Accessing Articles
         */
        else if(primarySelector.equals("articles")){

            UsersClient users = new UsersClient(config.defaultDomain());

            if(this.selectors.length >= 3){ //Provide [ACTION]/[ARTICLE_ID]

                String articleId = this.get(2);
                if(objectSelector.equals("id")){ //Specifying Article with ID

                    ArticlesClient articles = new ArticlesClient(config.defaultDomain());
                    try {

                        Article article = articles.get(articleId);
                        return new Result(
                                this.command,
                                "article",
                                article.toString(),
                                1
                        );
                    }catch(NullPointerException n){

                        return new Result(404, this.command, "\"Article not found\"");
                    }
                }else if(objectSelector.equals("update")){

                    if(content != null){

                        if(content.getString("id") == null){
                            content.put("id", articleId);
                        }
                        Article updatedArticle = new Article(content);
                        ArticlesClient articles = new ArticlesClient(config.defaultDomain());
                        if(config.getUser().getPermissions() >= 12 && this.targetDomain.equals("telifie")){ //Update Article in Public Domain

                            Article ogArticle = articles.get(articleId);
                            ArrayList<Event> events = updatedArticle.compare(ogArticle);

//                            if(events.size() > 0){

                            for(int i = 0; i < events.size(); i++){

                                events.get(i).setUser(config.getUser().getId());
                            }

                            if(articles.update(ogArticle, updatedArticle)){

                                TimelinesClient timelines = new TimelinesClient(config.defaultDomain());
                                timelines.addEvents(articleId, events);

                                //TODO Decide to return changes made to article or return new article
                                return new Result(
                                        this.command,
                                        "events",
                                        events.toString(),
                                        events.size()
                                );
                            }else{

                                return new Result(505, this.command, "\"Failed to update Article\"");
                            }
//                            }else{
//
//                                return new Result(304, this.command, "\"No changes were made to the Article\"");
//                            }
                        }else{

                            //Check permissions of user in Domains
                            //TODO, share changes with data team for approval and change status on Article
                            return new Result(401, this.command, "\"Insufficient permissions to update Article in Public Domain\"");
                        }
                    }else {

                        return new Result(428, "\"No new Article JSON data provided\"");
                    }
                }else if(objectSelector.equals("delete")){



                }else if(objectSelector.equals("move")){

                }else{

                    return new Result(404, this.command, "\"Unknown Articles action command\"");
                }
            }else if(objectSelector.equals("create")){

                if(content != null){

                    User user = config.getUser();
                    if(this.targetDomain.equals("telifie") && user.getPermissions() < 12){ //Make sure that the user has the permissions

                        return new Result(403, this.command, "\"No sufficient permissions to publish to public domain\"");
                    }
                    try {

                        Article newArticle = new Article(content);
                        ArticlesClient articles = new ArticlesClient(config.defaultDomain());
                        if(articles.create(newArticle)){

                            return new Result(
                                    this.command,
                                    "article",
                                    newArticle.toString(),
                                    1
                            );
                        }else{

                            return new Result(505, "Failed to create Article");
                        }
                    }catch(JSONException e){

                        return new Result(505, this.command, "\"Malformed Article JSON data provided\"");
                    }
                }else{

                    return new Result(428, "\"Precondition Failed. No new Article provided (JSON.article) as body\"");
                }
            }else if(objectSelector.equals("")){ //Return stats of Articles in domain (summary)

                return new Result(this.command,"stats", "", 0);
            }else{

                return new Result(404, "\"No Article ID provided\"");
            }
        }

        /*
         * Accessing Groups
         * Save, unsave, create, delete, update,
         */
        else if(primarySelector.equals("groups")){

            if(this.selectors.length >= 4){ //Ensure correct argument count

                String groupId = this.get(2), articleId = this.get(3);
                GroupsClient groups = new GroupsClient(config.defaultDomain());

                //Saves Article to Group given /groups/save/[GROUP_ID]/[ARTICLE_ID]
                if(objectSelector.equals("save")){

                    if(groups.save(config.getAuthentication().getUser(), groupId, articleId)){

                        return new Result(200, this.command, "\"Saved Article\"");
                    }else{

                        return new Result(505, this.command, "\"Failed to save Article\"");
                    }
                }
                //Removes article from group given /groups/unsave/[GROUP_NAME]/[ARTICLE_ID]
                else if(objectSelector.equals("unsave")){

                    if(groups.unsave(config.getAuthentication().getUser(), groupId, articleId)){

                        return new Result(200, this.command, "\"Unsaved Article\"");
                    }else{

                        return new Result(505, this.command, "\"Failed to unsave Article\"");
                    }
                }else{

                    return new Result(404, this.command, "\"Invalid favorites command\"");
                }
            }
            //Updates Group given JSON POST. See reference
            else if(objectSelector.equals("update")){

                String groupId = (this.selectors.length == 3 ? this.get(2) : null);
                if(groupId != null){

                    if(content != null){

                        //Cannot make a folder named $pinned as its reserved
                        if(content.getString("name") != null && content.getString("name").equals("Pinned")){

                            return new Result(304, this.command, "\"'Pinned' is a reserved Group name\"");
                        }

                        GroupsClient groups = new GroupsClient(config.defaultDomain());
                        if(groups.update(groupId, content)){

                            return new Result(200, this.command, "\"Group Update\"");
                        }else{

                            return new Result(505, this.command, "\"Failed to update Group\"");
                        }
                    }else{

                        return new Result(428, this.command, "\"Update content for Group not provided\"");
                    }
                }else{

                    return new Result(428, "\"ID of Group required to update\"");
                }
            }
            //Creates Group given /groups/create/[GROUP_NAME]
            else if(objectSelector.equals("create")){

                if(content != null){ //Creating Group with JSON content

                    Group newGroup = new Group(content);
                    GroupsClient groups = new GroupsClient(config.defaultDomain());
                    Group createdGroup = groups.create(config.getAuthentication().getUser(), newGroup);
                    if(createdGroup != null){

                        return new Result(
                                this.command,
                                "group",
                                createdGroup.toString(),
                                1
                        );
                    }else{

                        return new Result(505, this.command, "\"Failed to make Group with JSON\"");
                    }
                }else{
                    String groupName = (this.selectors.length == 3 ? this.get(2) : null);
                    try {

                        groupName = URLDecoder.decode(groupName, "UTF-8");
                    } catch (UnsupportedEncodingException e) {

                    }
                    if(groupName != null){

                        GroupsClient groups = new GroupsClient(config.defaultDomain());
                        Group newGroup = groups.create(config.getAuthentication().getUser(), groupName);
                        if(newGroup != null){

                            return new Result(
                                    this.command,
                                    "group",
                                    newGroup.toString(),
                                    1
                            );
                        }else{

                            return new Result(505, this.command, "\"Failed to create group '" + groupName + "'\"");
                        }
                    }else{

                        return new Result(428, "\"Name of new group required\"");
                    }
                }
            }
            // Deletes folder given /groups/delete/[GROUP_ID]
            else if(objectSelector.equals("delete")){

                String groupId = (this.selectors.length == 3 ? this.get(2) : null);
                if(groupId != null){

                    GroupsClient groups = new GroupsClient(config.defaultDomain());
                    if(groups.delete(config.getAuthentication().getUser(), groupId)){

                        return new Result(200, this.command, "\"Group '" + groupId + "' deleted\"");
                    }else{

                        return new Result(505, this.command, "\"Failed to delete group '" + groupId + "'\"");
                    }
                }else{

                    return new Result(428, "Name of new group required.");
                }
            }
            /*
             * Returns Group given /groups/id/[GROUP_ID]
             */
            else if(objectSelector.equals("id")){

                String groupId = this.get(2);
                GroupsClient groups = new GroupsClient(config.defaultDomain());
                if(groupId != null){

                    try{

                        Group group = groups.get(config.getAuthentication().getUser(), groupId);
                        return new Result(
                                this.command,
                                "group",
                                group.toString(),
                                1
                        );
                    }catch(NullPointerException n){

                        return new Result(404, this.command, "\"Group not found with ID '" + groupId + "'\"");
                    }
                }else{

                    return new Result(428, this.command, "\"Group ID is required to get\"");
                }
            }else{

                GroupsClient groups = new GroupsClient(config.defaultDomain());
                ArrayList<Group> usersGroups = groups.groupsForUser(config.getAuthentication().getUser());
                return new Result(
                        this.command,
                        "groups",
                        usersGroups.toString(),
                        usersGroups.size()
                );
            }
        }
        /*
         * Accessing Users
         */
        else if(primarySelector.equals("users")){

            if(this.selectors.length > 3){

                if(this.get(1).equals("update") && content != null){

                    String userEmail = this.get(3);
                    UsersClient usersClient = new UsersClient(config.defaultDomain());
                    if(this.get(2).equals("theme")){

                        if(usersClient.updateUserTheme(usersClient.getUserWithEmail(userEmail), content)){

                            User changedUser = usersClient.getUserWithEmail(userEmail);
                            return new Result(
                                    this.command,
                                    "user",
                                    changedUser.toString(),
                                    1
                            );
                        }else{

                            return new Result(400, "\"Bad Request\"");
                        }
                    }
                }else{

                    return new Result(404, this.command, "\"Invalid command received\"");
                }
            }else if(objectSelector.equals("create")){ //Creating User

                if(content != null){

                    User newUser = new User(
                            content.getString("email"),
                            content.getString("name"),
                            content.getString("phone")
                    );
                    if(newUser.getPermissions() == 0 && !newUser.getName().equals("") && !newUser.getEmail().equals("") && newUser.getName() != null && newUser.getEmail() != null){ //0 is permissions of new user

                        UsersClient users = new UsersClient(config.defaultDomain());
                        if(!users.userExistsWithEmail(newUser.getEmail())){

                            if(users.createUser(newUser)){

                                return new Result(
                                        this.command,
                                        "user",
                                        newUser.toString(),
                                        1
                                );
                            }else{

                                return new Result(505, this.command, "\"Failed making user for email '" + newUser.getEmail() + "'\"");
                            }
                        }else{

                            return new Result(410, this.command, "\"User already exists for email '" + newUser.getEmail() + "'\"");
                        }
                    }else{

                        return new Result(428, this.command, "\"Name, email, phone number, and permissions are required to create a User\"");
                    }
                }else{

                    return new Result(428, this.command, "\"New User details expected with JSON request body.\"");
                }
            }else{ //Get user with email

                UsersClient usersClient = new UsersClient(config.defaultDomain());
                Matcher matcher = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE).matcher(this.get(1));
                if (matcher.matches()) {

                    if(usersClient.userExistsWithEmail(this.get(1))){ //Check if user with email, return user

                        User found = usersClient.getUserWithEmail(this.get(1));
                        return new Result(
                                this.command,
                                "user",
                                found.toString(),
                                1
                        );
                    }else { //No valid user action command provided

                        return new Result(404, this.command, "\"User not found\"");
                    }
                }
            }
        }else if(primarySelector.equals("parser")){ //Accessing parser

            if(this.selectors.length >= 2){

                if(objectSelector.equals("batch")){ //Importing CSV as batch of Articles

                    String uri = this.command.replaceFirst("parser/batch", "");
                    ArrayList<Article> extractedArticles = Parser.engines.batch(uri, ",");

                    if(extractedArticles != null){

                        return new Result(
                                this.command,
                                "articles",
                                extractedArticles.toString(),
                                extractedArticles.size()
                        );
                    }else{

                        return new Result(404, this.command, "\"No articles from batch upload\"");
                    }
                }else if(objectSelector.equals("dictionary")){ //Managing index dictionary for parsing

                    if(content != null && content.getString("words") != null){

                        String[] words = content.getString("words").split(" ");
                        Parser.index index = new Parser.index();
                        Parser.index.dictionary dict = new Parser.index.dictionary(Vars.Languages.ENGLISH);
                        dict.add(words);
                        return new Result(200, this.command, "\"Words added to dictionary\"").setCount(dict.getSize());
                    }else{

                        return new Result(428, this.command, "\"JSON http request body expected\"");
                    }
                }else{
                    String uri = this.command.replaceFirst(this.targetDomain + "://parser/", "");
                    return new Result(
                            this.command,
                            "article",
                            Parser.engines.parse(uri).toString(),
                            1
                    );
                }
            }else{
                //TODO accept file uploads instead of using URI
                return new Result(404, this.command, "\"Parser action command required\"");
            }
        }else if(primarySelector.equals("queue")){

            if(this.selectors.length >= 2){

            }else if(content != null){

                //User is uploading content to queue
                String uri = content.getString("uri");
                String connector = (content.getString("connector") == null ? null : content.getString("connector"));
                if(connector == null || connector.equals("")){

                    //TODO return draft article
                    if(uri == null || uri.equals("")){

                        return new Result(410, this.command, "\"No URI provided as {\"uri\" : \"URI\"}\"");
                    }else{

                        //Check if an article exists for this already
                        ArticlesClient articles = new ArticlesClient(config.defaultDomain());
                        if(articles.get(new Document("link", uri)).size() > 0){



                        }else{

                            QueueClient queue = new QueueClient(config.defaultDomain());
                            Article queued = queue.add(uri);
                            if(queued != null){

                                return new Result(
                                        this.command,
                                        "article",
                                        queued.toString(),
                                        1
                                );
                            }else{

                                return new Result(505, "\"There was an error parsing the queue request\"");
                            }
                        }

                        return new Result(200, uri, "\"Queued\"");
                    }
                }else{

                    //TODO Queue from connector
                    return new Result(200, this.command, "\"Queued from connector\"");
                }
            }else{

                return new Result(404, "\"Invalid queue command\"");
            }
        }else if(primarySelector.equals("search")){

            String query = this.selectorsString.replaceFirst("search/", "");
            if(!this.targetDomain.equals("telifie") && !this.targetDomain.equals("")){

                config.setDefaultDomain(config.defaultDomain().setName(this.targetDomain));
            }
            return new Search(config, query).result();
        }else if(primarySelector.equals("server")){ //Accessing server

            if(this.selectors.length >= 2){

                boolean threaded = false;
                if(this.get(2).equals("threaded") && this.get(3).equals("true")){

                    threaded = true;
                }

                if(this.get(1).equals("https")){

                    Out.console("Starting HTTPS server...");
                    new HttpsServer(config, threaded);
                }else if(this.get(1).equals("http")){

                    Out.console("Starting HTTP server...");
                    new Server(threaded);
                }
            }else{

                Out.console("Starting HTTP server...");
                boolean threaded = false;
                if(this.get(2).equals("threaded") && this.get(3).equals("true")){

                    threaded = true;
                }
                try{

                    new Server(threaded);
                }catch(Exception e){

                    new Server(threaded);
                }
            }
        }else if(primarySelector.equals("authenticate")){ //authentication

            if(this.selectors.length >= 3 && this.get(1).equals("app")){

                String appId = this.get(2);
                Authentication auth = new Authentication(appId);
                AuthenticationClient authentications = new AuthenticationClient(config.defaultDomain());
                if(authentications.authenticate(auth)){

                    //TODO

                }else{

                    Out.error("Failed to authorize app with ID: " + appId);
                }
            }

        }else if(primarySelector.equals("connect")){

            if(this.selectors.length >= 2){

                String email = this.get(1);
                UsersClient usersClient = new UsersClient(config.defaultDomain());
                if(usersClient.userExistsWithEmail(email)){

                    User user = usersClient.getUserWithEmail(email);
                    if(user.getPermissions() == 0){ //Email needs verified

                        ConnectorsClient connectors = new ConnectorsClient();
                        Connector sendgrid = connectors.getConnector("SendGrid");
                        if(sendgrid != null){

                            SGrid sg = new SGrid(sendgrid);
                            String code = Tool.shortEid();
                            if(usersClient.lockUser(user, code)){

                                if(sg.sendAuthenticationCode(user.getEmail(), code)){

                                }else{

                                    return new Result(505, this.command, "\"Failed to email code through SendGrid\"");
                                }
                            }else{

                                return new Result(505, this.command, "\"Failed to lock users account\"");
                            }
                        }else{

                            return new Result(410, "\"Please create SendGrid Connector information\"");
                        }

                    }else if(user.getPermissions() >= 1){ //Phone needs verified

                        if(user.lock()){

                            return new Result(200, "\"Authentication code sent\"");
                        }else{

                            return new Result(501, "\"Failed to send code\"");
                        }
                    }
                }else{

                    return new Result(404, "\"Account not found\"");
                }
            }
        }else if(primarySelector.startsWith("verify")){ //Provided /verify/[USER_EMAIL]/[VERIFICATION_CODE]

            if(this.selectors.length >= 3){

                String email = this.get(1), code = Tool.md5(this.get(2));
                UsersClient users = new UsersClient(config.defaultDomain());
                User user = users.getUserWithEmail(email);
                if(user.hasToken(code)){ //Check if user has the right code

                    if(user.getPermissions() == 0 || user.getPermissions() == 1){ //User was verifying email

                        users.upgradePermissions(user);
                        user.setPermissions(user.getPermissions() + 1); //For request return purposes. It has been updated
                    }

                    Authentication auth = new Authentication(user);
                    AuthenticationClient auths = new AuthenticationClient(config.defaultDomain());
                    auths.authenticate(auth);
                    JSONObject json = new JSONObject(user.toString());
                    json.put("authentication", auth.toJson());

                    return new Result(
                            this.command,
                            "user",
                            json.toString(),
                            1
                    );
                }else{

                    return new Result(403, "\"Invalid verification code provided\"");
                }
            }else{

                return new Result(404, this.command, "\"Invalid command received\"");
            }
        }else if(primarySelector.equals("timelines")){

            if(this.selectors.length >= 2){

                String objectId = this.get(1);
                TimelinesClient timelines = new TimelinesClient(config.defaultDomain());
                Timeline timeline = timelines.getTimeline(objectId);
                if(timeline != null){

                    return new Result(
                            this.command,
                            "timeline",
                            timeline.toString(),
                            1
                    );
                }else{

                    return new Result(404, this.command, "\"No timeline found for " + objectId + "\"");
                }

            }else{

                return new Result(428, this.command, "\"Please provide object ID to get timeline\"");
            }
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
                                    newConnector.toString(),
                                    1
                            );
                        }else{

                            return new Result(505, this.command, "\"Failed to create Connector\"");
                        }
                    }else{

                        return new Result(428, this.command, "\"JSON body expected to create connector\"");
                    }
                }else{

                    new Result(404, this.command, "\"Invalid connectors action provided\"");
                }
            }else{ //Gets all available Connectors for use

                //TODO Do not get files everytime, do this once when the server starts
                ConnectorsClient connectors = new ConnectorsClient();
                ArrayList<Connector> all = connectors.getConnectors();
                return new Result(
                        this.command,
                        "connectors",
                        all.toString(),
                        all.size()
                );
            }
        }else if(primarySelector.equals("sources")){

            if(this.selectors.length >= 2){

                SourcesClient sources = new SourcesClient(config.defaultDomain());
                if (objectSelector.equals("id")){ //Getting source by ID

                    String sourceId = this.get(2);
                    try{

                        Source source = sources.get(sourceId);
                        return new Result(
                                this.command,
                                "source",
                                source.toString(),
                                1
                        );
                    }catch(NullPointerException n){

                        return new Result(404, this.command, "\"No source found\"");
                    }

                }else if(objectSelector.equals("create")){

                    if(content != null){

                        Source newSource = new Source(content);
                        if(sources.create(newSource)){

                            return new Result(
                                    this.command,
                                    "source",
                                    newSource.toString(),
                                    1
                            );
                        }else{
                            return new Result(505, this.command, "\"Failed to make source\"");
                        }
                    }else{

                    }
                }else{ //Searching sources by title

                    String search = this.get(1);
                    ArrayList<Source> foundSources = sources.find(search);
                    if(foundSources.size() > 0){

                        return new Result(
                                this.command,
                                "sources",
                                foundSources.toString(),
                                foundSources.size()
                        );
                    }else{

                        return new Result(404, this.command, "\"No sources found\"");
                    }
                }

            }else{ //Fetching all available sources

            }
        }

        return new Result(200, this.command, "\"No command received\"");
    }
}