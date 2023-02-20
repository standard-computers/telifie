package com.telifie.Models.Utilities;

import com.telifie.Models.*;
import com.telifie.Models.Actions.Out;
import com.telifie.Models.Actions.Search;
import com.telifie.Models.Clients.*;
import org.bson.Document;
import org.json.JSONException;
import org.json.JSONObject;
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
        this.targetDomain = spl[0];
        this.selectorsString = this.command.replaceFirst(targetDomain + "://", "");
        this.selectors = this.selectorsString.split("(?!//)/");
        this.primarySelector = this.get(0).replaceAll("/", "");

    }

    public String primarySelector() {
        return primarySelector;
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

                String userId = this.get(2);
                ArrayList<Domain> foundDomains;
                if(objectSelector.equals("owner") || objectSelector.equals("")){

                    DomainsClient domains = new DomainsClient(config.defaultDomain());
                    foundDomains = domains.getOwnedDomains(userId);
                    return new Result(
                            this.command,
                            "domains",
                            foundDomains.toString(),
                            foundDomains.size()
                    );

                }else if(objectSelector.equals("user")){

                    DomainsClient domains = new DomainsClient(config.defaultDomain());
                    foundDomains = domains.getDomainsForUser(userId);
                    return new Result(
                            this.command,
                            "domains",
                            foundDomains.toString(),
                            foundDomains.size()
                    );

                }

            }else{

                return new Result(200, this.command, "none");

            }

        }else if(primarySelector.equals("articles")){ //Access Articles

            if(this.selectors.length >= 3){ //Provide [ACTION]/[ARTICLE_ID]

                String articleId = this.get(2);

                if(objectSelector.equals("id")){ //Specifying Article with ID

                }else if(objectSelector.equals("update")){

                    if(content != null){



                    }else{

                        return new Result(428, "No new Article JSON data provided");

                    }

                }else if(objectSelector.equals("delete")){

                }else if(objectSelector.equals("move")){

                }else{

                    return new Result(404, this.command, "\"Unknown Articles action command\"");

                }

            }else if(objectSelector.equals("create")){

                if(content != null){

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
                //Saves Article to Group given /groups/unsave/[GROUP_ID]/[ARTICLE_ID]
                if(objectSelector.equals("save")){

                    if(groups.save(config.getAuthentication().getUser(), groupId, articleId)){

                        return new Result(200, this.command, "\"Saved Article\"");

                    }else{

                        return new Result(505, this.command, "\"Failed to save Article\"");

                    }

                }
                //Removes Article from Group given /groups/save/[GROUP_NAME]/[ARTICLE_ID]
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
                        if(content.getString("name") != null && content.getString("name").equals("$pinned")){
                            
                            return new Result(304, this.command, "\"'$pinned' is a reserved Group name\"");
                            
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

                String groupName = (this.selectors.length == 3 ? this.get(2) : null);
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
            // Deletes folder given /groups/delete/[GROUP_NAME]
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

                    Group group = groups.get(config.getAuthentication().getUser(), groupId);
                    return new Result(
                        this.command,
                        "group",
                        group.toString(),
                        1
                    );

                }else{

                    return new Result(428, this.command, "\"Group ID is required to get\"");

                }

            }
            /*
             *
             */
            else{

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

                    String userEmail = content.getString("email");
                    if(content.containsKey("id") && content.containsKey("email")){

                        UsersClient usersClient = new UsersClient(config.defaultDomain());

                        if(this.selectors.length >= 3 && this.get(2).equals("theme")){

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

                        return new Result(403, this.command, "\"User information not included\"");

                    }

                }else{

                    return new Result(404, this.command, "\"Invalid command received\"");

                }

            }else{

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

                    }else{ //No valid user action command provided

                        return new Result(404, this.command, "\"User not found\"");

                    }

                }

            }

        }else if(primarySelector.equals("parser")){ //Accessing parser

            if(this.selectors.length >= 2){

                String uri;
                if(this.get(1).equals("csv")){ //Importing CSV as batch of Articles

                    uri = this.command.replaceFirst(this.targetDomain + "://parser/csv/", "");
                    //Todo batch uploader

                }else{

                    uri = this.command.replaceFirst(this.targetDomain + "://parser/", "");
                    Parser parser = new Parser(uri);
                    return new Result(
                            this.command,
                            "article",
                            parser.parse().toString(),
                            1
                    );

                }

            }else{

                return new Result(404, this.command, "\"Invalid parser action command\"");

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
                    //Queue from connector
                    return new Result(200, this.command, "\"Queued from connector\"");
                }

            }else{

                return new Result(404, "\"Invalid queue command\"");

            }

        }else if(primarySelector.equals("search")){ //Search

            String query = this.selectorsString.replace("search/", "");
            Search search = new Search(config, query);
            return search.result();

        }else if(primarySelector.equals("server")){ //Accessing server

            if(this.selectors.length >= 2){

                if(this.get(1).equals("https")){

                    Out.console("Starting HTTPS server...");
                    HttpsServer https_server;
                    boolean threaded = false;
                    if(this.selectors.length >= 3){

                        if(this.get(2).equals("threaded") && this.get(3).equals("false")){

                            threaded = false;

                        }else if(this.get(2).equals("threaded") && this.get(3).equals("true")){

                            threaded = true;

                        }
                    }
                    https_server = new HttpsServer(config, threaded);

                }else if(this.get(1).equals("http")){

                    Out.console("Starting HTTP server...");
                    Server http_server;
                    boolean threaded = false;
                    if(this.selectors.length >= 3){
                        if(this.get(2).equals("threaded") && this.get(3).equals("false")){
                            threaded = false;
                        }else if(this.get(2).equals("threaded") && this.get(3).equals("true")){
                            threaded = true;
                        }
                    }
                    http_server = new Server(threaded);

                }

            }else{

                Out.console("Starting HTTP server...");
                Server http_server;
                boolean threaded = false;
                if(this.selectors.length >= 3){

                    if(this.get(2).equals("threaded") && this.get(3).equals("false")){

                        threaded = false;

                    }else if(this.get(2).equals("threaded") && this.get(3).equals("true")){

                        threaded = true;

                    }

                }
                http_server = new Server(threaded);

            }

        }else if(primarySelector.equals("exit") || primarySelector.equals("close")){

            Out.console("Exiting telifie...");
            System.exit(0);

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

        }else if(primarySelector.startsWith("connect")){
            //TODO pass through request type from server if possible
            //TODO make sure that it's a post request
            if(this.selectors.length >= 2){

                String email = this.get(1);
                UsersClient usersClient = new UsersClient(config.defaultDomain());
                if(usersClient.userExistsWithEmail(email)){

                    User user = usersClient.getUserWithEmail(email);
                    if(user.lock()){

                        return new Result(200, "Authentication code sent");

                    }else{

                        return new Result(501, "Failed to send code");

                    }

                }else{

                    return new Result(404, "Account not found");

                }

            }

            //TODO check authentication with Authentication and MongoDB

        }else if(primarySelector.startsWith("verify")){

            //TODO use Twilio or Email/SendGrid for account verification
            if(this.selectors.length >= 3){

                String email = this.get(1);
                String code = Tool.md5(this.get(2));
                UsersClient users = new UsersClient(config.defaultDomain());
                User user = users.getUserWithEmail(email);
                if(user.hasToken(code)){ //Check if user has the right code

                    //TODO create authentication with client
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

                    return new Result(403, "Invalid verification code provided");

                }

            }else{

                return new Result(404, this.command, "Invalid command received");

            }

        }else if(primarySelector.equals("messaging")){

            //TODO sending/receiving messages

        }else if(primarySelector.equals("netstat")){ //Pinging server for status

            //TODO diagnostics, logging, system stats, etc.

        }else{

            //TODO GraphQL with POST method as query
            return new Result(404, this.command, "\"Invalid command\"");

        }

        return new Result(200, this.command, "\"No command received\"");

    }

}