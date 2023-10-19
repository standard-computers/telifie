package com.telifie.Models.Actions;

import com.telifie.Models.*;
import com.telifie.Models.Andromeda;
import com.telifie.Models.Parser;
import com.telifie.Models.Clients.*;
import com.telifie.Models.Connectors.Spotify;
import com.telifie.Models.Utilities.*;
import org.apache.hc.core5.http.ParseException;
import org.bson.Document;
import org.json.JSONException;
import org.json.JSONObject;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Command {

    private final String command;
    private final String[] selectors;

    public Command(String command){
        this.command = command;
        this.selectors = this.command.split("(?!//)/");
    }

    public String get(int index){
        return this.selectors[index].replaceAll("/", "");
    }

    public Result parseCommand(Session session, Document content){

        String primarySelector = this.selectors[0];
        String objectSelector = (this.selectors.length > 1 ? this.get(1) : "");
        String secSelector = (this.selectors.length > 2 ? this.get(2) : null);
        String terSelector = (this.selectors.length > 3 ? this.get(3) : null);
        String actingUser = session.getUser();

        if(primarySelector.equals("search")){
            if(content != null){
                String query = content.getString("query");
                if(query.isEmpty()){
                    return new Result(428, this.command, "Query expected");
                }
                String targetDomain = (content.getString("domain") == null ? "telifie" : content.getString("domain"));
                if(!targetDomain.equals("telifie")){
                    try{
                        DomainsClient domains = new DomainsClient(session);
                        Domain domain = domains.withId(targetDomain);
                        session.setDomain(domain.getId());
                    }catch (NullPointerException n){
                        return new Result(410, this.command, "NOT FOUND");
                    }
                }
                try {
                    Parameters params = new Parameters(content);
                    return new Search().execute(session, query, params);
                }catch(NullPointerException n){
                    return new Result(505, this.command, "SEARCH ERROR");
                }
            }
            return new Result(428, this.command, "JSON BODY EXPECTED");
        }
        /*
         * Domains: owner, member (add/remove), create, update
         */
        else if(primarySelector.equals("domains")){

            if(this.selectors.length >= 2){ //telifie.com/domains/{owner|member|create|update}
                DomainsClient domains = new DomainsClient(session);
                ArrayList<Domain> foundDomains;
                if(objectSelector.equals("owner")){ //Domains they own

                    foundDomains = domains.mine();
                    return new Result(this.command, "domains", foundDomains);
                }else if(objectSelector.equals("member")){ //Domains they're a member of
                    //TODO ensure it works
                    User user = new UsersClient().getUserWithId(session.getUser());
                    foundDomains = domains.forMember(user.getEmail());
                    return new Result(this.command, "domains", foundDomains);
                }else if(objectSelector.equals("create")){ //Creating a new domain
                    if(content != null){
                        String domainName, domainIcon;
                        if((domainName = content.getString("name")) != null && (domainIcon = content.getString("icon")) != null && content.getInteger("permissions") != null){
                            Domain newDomain = new Domain(actingUser, domainName, domainIcon, content.getInteger("permissions"));
                            if(domains.create(newDomain)){
                                return new Result(this.command, "domain", newDomain);
                            }
                            return new Result(505, this.command, "FAILED DOMAIN CREATION");
                        }
                        return new Result(428, this.command, "DOMAIN INFO NOT PROVIDED");
                    }
                    return new Result(428, this.command, "JSON BODY EXPECTED");
                }else if(this.selectors.length == 3){ //telifie.com/domains/{delete|id}/{domainID}
                    if(secSelector != null){
                        try{
                            Domain d = domains.withId(secSelector);
                            switch (objectSelector) {
                                case "delete" -> {
                                    if (d.getOwner().equals(actingUser)) {
                                        if (domains.delete(d)) {
                                            return new Result(200, this.command, "DOMAIN DELETED");
                                        }
                                        return new Result(505, this.command, "FAILED DOMAIN DELETION");
                                    }
                                    return new Result(401, this.command, "DOMAIN ACCESS DENIED");
                                }
                                case "id" -> {
                                    if(d.hasPermission(session.getUser())){
                                        return new Result(this.command, "domain", d);
                                    }
                                    return new Result(403, "DOMAIN ACCESS DENIED");
                                }
                                case "check" -> {
                                    int p = d.getPermissions(session.getUser());
                                    if(p == 0){
                                        return new Result(200, "OWNER");
                                    }else if(p == 1){
                                        return new Result(206, "VIEWER");
                                    }else if(p == 2){
                                        return new Result(207, "EDITOR");
                                    }
                                    return new Result(403, "DOMAIN ACCESS DENIED");
                                }
                                case "update" -> {
                                    if (content != null) {
                                        if (domains.update(d, content)) { //TODO content check
                                            return new Result(this.command, "domain", d);
                                        }
                                    }
                                    return new Result(428, this.command, "JSON BODY EXPECTED");
                                }
                            }
                            return new Result(404, this.command, "BAD DOMAIN SELECTOR");
                        }catch (NullPointerException n){
                            return new Result(404, this.command, "DOMAIN NOT FOUND");
                        }
                    }
                    return new Result(428, this.command, "DOMAIN ID EXPECTED");
                }else if(this.selectors.length == 4){ //telifie.com/domains/{id}/users/{add|remove}
                    try{
                        Domain d = domains.withId(objectSelector);
                        if(content != null){
                            ArrayList<Member> members = new ArrayList<>();
                            content.getList("users", Document.class).forEach(doc -> members.add(new Member(doc)));
                            switch (terSelector) {
                                case "add" -> {
                                    if (domains.addUsers(d, members)) {
                                        return new Result(200, this.command, "ADDED USERS TO DOMAIN");
                                    }
                                    return new Result(505, this.command, "FAILED ADDING USERS TO DOMAIN");
                                }
                                case "remove" -> {
                                    if (domains.removeUsers(d, members)) {
                                        return new Result(200, this.command, "REMOVED USERS FROM DOMAIN");
                                    }
                                    return new Result(505, this.command, "FAILED REMOVING USERS FROM DOMAIN");
                                }
                                case "update" -> {
                                    if (domains.updateUsers(d, members)) {
                                        return new Result(200, this.command, "DOMAIN USERS UPDATED");
                                    }
                                    return new Result(505, this.command, "FAILED DOMAIN USERS UPDATE");
                                }
                            }
                            return new Result(428, this.command, "BAD DOMAIN USER SELECTOR");
                        }
                        return new Result(428, this.command, "JSON BODY EXPECTED");
                    }catch (NullPointerException n){
                        return new Result(404, this.command, "DOMAIN NOT FOUND");
                    }
                }
            }
            return new Result(200, this.command, "BAD DOMAINS SELECTOR");
        }
        /*
         * Accessing Articles
         */
        else if(primarySelector.equals("articles")){

            User user = new UsersClient().getUserWithId(session.getUser());
            if(user.getPermissions() < 12 && !session.getDomain().equals("telifie")){
                //TODO, share changes with data team for approval and change status on Article
                return new Result(401, this.command, "INSUFFICIENT PERMISSIONS");
            }
            if(content != null){
                if(content.getString("domain") != null){
                    String targetDomain = content.getString("domain");
                    DomainsClient domains = new DomainsClient(session);
                    try{
                        Domain domain = domains.withId(targetDomain);
                        session.setDomain(domain.getId());
                        //TODO Check permissions with user
                    }catch (NullPointerException n) {
                        return new Result(404, this.command, "DOMAIN NOT FOUND");
                    }
                }
            }
            ArticlesClient articles = new ArticlesClient(session);
            if(this.selectors.length >= 3){
                try {
                    Article a = articles.withId(secSelector);
                    switch (objectSelector) {
                        case "id" -> {
                            try {
                                return new Result(this.command, "article", articles.withId(secSelector));
                            } catch (NullPointerException n) {
                                return new Result(404, this.command, "ARTICLE NOT FOUND");
                            }
                        }
                        case "update" -> {
                            if (content != null) {
                                if (content.getString("id") == null) {
                                    content.put("id", secSelector);
                                }
                                Article ua = new Article(content);
                                //TODO compare
                                if (articles.update(a, ua)) {
                                    return new Result(200, this.command, ua.toString());
                                }
                            }
                            return new Result(428, "NO NEW ARTICLE");
                        }
                        case "delete" -> {
                            if (articles.delete(articles.withId(secSelector))) {
                                return new Result(200, this.command, "");
                            }
                            return new Result(505, this.command, "FAILED ARTICLE DELETION");
                        }
                        case "duplicate" -> {
                            if (this.selectors.length > 3) {
                                DomainsClient domains = new DomainsClient(session);
                                try {
                                    if (articles.duplicate(a, domains.withId(terSelector))) {
                                        return new Result(200, this.command, "");
                                    }
                                    return new Result(505, this.command, "FAILED ARTICLE DUPLICATION");
                                } catch (NullPointerException n) {
                                    return new Result(404, this.command, "DOMAIN NOT FOUND");
                                }
                            }
                        }
                        case "archive" -> {
                            if (articles.archive(a)) {
                                return new Result(200, this.command, "ARTICLE UNARCHIVED");
                            }
                            return new Result(505, this.command, "FAILED ARTICLE ARCHIVE");
                        }
                        case "unarchive" -> {
                            ArchiveClient archive = new ArchiveClient(session);
                            if (archive.unarchive(a)) {
                                return new Result(200, this.command, "");
                            }
                            return new Result(505, this.command, "FAILED ARTICLE UNARCHIVE");
                        }
                        case "move" -> {
                            if (this.selectors.length > 3) {
                                DomainsClient domains = new DomainsClient(session);
                                try {
                                    if (articles.move(a, domains.withId(terSelector))) {
                                        return new Result(200, this.command, "");
                                    }
                                    return new Result(505, this.command, "FAILED ARTICLE MOVE");
                                } catch (NullPointerException n) {
                                    return new Result(404, this.command, "DOMAIN NOT FOUND");
                                }
                            }
                        }
                        case "verify" -> {
                            if (articles.verify(secSelector)) {
                                return new Result(200, this.command, "");
                            }
                            return new Result(505, this.command, "FAILED ARTICLE UPDATE");
                        }
                    }
                    return new Result(404, this.command, "BAD ARTICLED SELECTOR");
                }catch (NullPointerException n){
                    return new Result(404, this.command, "ARTICLE NOT FOUND");
                }

            }else if(objectSelector.equals("create")){
                if(content != null){
                    try {
                        Article na = new Article(content);
                        if(articles.create(na)){
                            return new Result(this.command, "article", na);
                        }
                        return new Result(505, this.command, "FAILED ARTICLE CREATION");
                    }catch(JSONException e){
                        return new Result(505, this.command, "BAD ARTICLE JSON");
                    }
                }
                return new Result(428, "ARTICLE JSON DATA EXPECTED");
            }
            return new Result(this.command,"stats", articles.stats());
        }
        /*
         * Accessing Collections
         * Save, unsave, create, delete, update,
         */
        else if(primarySelector.equals("collections")){

            CollectionsClient collections = new CollectionsClient(session);
            if(this.selectors.length >= 4){ //Saving/Unsaving articles in collection
                try{
                    Collection c = collections.get(secSelector);
                    try{
                        ArticlesClient articles = new ArticlesClient(session);
                        Article a = articles.withId(terSelector);
                        if(objectSelector.equals("save")){
                            if(collections.save(c, a)){
                                return new Result(200, this.command, "ARTICLE SAVED");
                            }
                            return new Result(505, this.command, "FAILED ARTICLE SAVE");
                        } else if(objectSelector.equals("unsave")){

                            if(collections.unsave(c, a)){
                                return new Result(200, this.command, "ARTICLE UNSAVED");
                            }
                            return new Result(505, this.command, "FAILED ARTICLE UNSAVE");
                        }
                        return new Result(404, this.command, "BAD COLLECTION SELECTOR");
                    }catch (NullPointerException n){
                        return new Result(404, this.command, "ARTICLE NOT FOUND");
                    }
                }catch (NullPointerException n){
                    return new Result(404, this.command, "COLLECTION NOT FOUND");
                }

            }else if(this.selectors.length >= 2){ //Updating, deleting, getting collections
                if(secSelector != null) {
                    try {
                        Collection c = collections.get(secSelector);
                        switch (objectSelector) {
                            case "update" -> {
                                if (content != null) {
                                    if (content.getString("name") != null && content.getString("name").equals("Pinned")) {
                                        return new Result(304, this.command, "'Pinned' is a reserved Collection name");
                                    }
                                    if(c.getUser().equals(session.getUser())){
                                        if (collections.update(c, content)) {
                                            return new Result(200, this.command, "COLLECTION UPDATED");
                                        }
                                        return new Result(505, this.command, "FAILED COLLECTION UPDATE");
                                    }
                                    return new Result(428, this.command, "NO COLLECTION PERMISSIONS");
                                }
                                return new Result(428, this.command, "COLLECTION JSON EXPECTED");
                            }
                            case "delete" -> {
                                if (collections.delete(c)) {
                                    return new Result(200, this.command, "COLLECTION DELETED");
                                }
                                return new Result(505, this.command, "FAILED COLLECTION DELETION");
                            }
                            case "id" -> {
                                if(c.getUser().equals(session.getUser())) {
                                    return new Result(this.command, "collection", collections.withArticles(secSelector));
                                }
                                return new Result(428, this.command, "NO COLLECTION PERMISSIONS");
                            }
                        }
                        return new Result(404, this.command, "BAD COLLECTIONS SELECTOR");
                    }catch (NullPointerException n){
                        return new Result(404, this.command, "COLLECTION NOT FOUND");
                    }
                }else if(objectSelector.equals("create")){
                    if(content != null){
                        Collection newCollection = new Collection(content);
                        Collection createdCollection = collections.create(newCollection);
                        if(createdCollection != null){
                            return new Result(this.command, "collection", createdCollection);
                        }
                        return new Result(505, this.command, "FAILED COLLECTION CREATION");
                    }
                    return new Result(428, "JSON BODY EXPECTED");
                }
                return new Result(428, "COLLECTION ID REQUIRED");
            }
            ArrayList<Collection> usersCollections = collections.forUser(actingUser);
            return new Result(this.command, "collections", usersCollections);
        }
        /*
         * Accessing Users
         */
        else if(primarySelector.equals("users")){

            UsersClient users = new UsersClient();
            if(this.selectors.length >= 3){
                if(objectSelector.equals("update") && content != null){
                    User changedUser = users.getUserWithId(session.getUser());
                    if(secSelector.equals("theme")){
                        Theme theme = new Theme(content);
                        if(users.updateUserTheme(changedUser, theme)){
                            return new Result(this.command, "user", changedUser);
                        }
                        return new Result(400, "Bad Request");
                    }else if(secSelector.equals("photo")){
                        if(content.getString("photo") != null) {
                            String photoUri = content.getString("photo");
                            if (users.updateUserPhoto(changedUser, photoUri)) {
                                return new Result(200, this.command, "USER PHOTO UPDATED");
                            }
                            return new Result(505, this.command, "FAILED USER PHOTO UPDATE");
                        }
                        return new Result(428, this.command, "JSON 'photo' EXPECTED");
                    }
                    return new Result(404, this.command, "BAD USERS SELECTOR");
                }
                return new Result(404, this.command, "BAD USERS SELECTOR");

            }else if(objectSelector.equals("create")){
                if(content != null){
                    User newUser = new User(content.getString("email"), content.getString("name"), content.getString("phone"));
                    if(newUser.getPermissions() == 0 && !newUser.getName().isEmpty() && !newUser.getEmail().isEmpty() && newUser.getName() != null && newUser.getEmail() != null) {
                        if (!users.userExistsWithEmail(newUser.getEmail())) {
                            if (users.createUser(newUser)) {
                                return new Result(this.command, "user", newUser);
                            }
                            return new Result(505, this.command, "FAILED USER CREATION");
                        }
                        return new Result(410, this.command, "EMAIL TAKEN");
                    }
                    return new Result(428, this.command, "NOT ENOUGH INFO");
                }
                return new Result(428, this.command, "JSON BODY EXPECTED");
            }
            Matcher matcher = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE).matcher(objectSelector);
            if (matcher.matches()) {
                if(users.userExistsWithEmail(objectSelector)){
                    User found = users.getUserWithEmail(objectSelector);
                    return new Result(this.command, "user", found);
                }
                return new Result(404, this.command, "USER NOT FOUND");
            }
            return new Result(404, this.command, "INVALID EMAIL");
        }
        /*
         * Accessing Parser
         */
        else if(primarySelector.equals("parser")){
            ArticlesClient articles = new ArticlesClient(session);
            if(content != null){
                String mode = content.getString("mode");
                if(mode != null){
                    switch (mode) {
                        case "batch" -> {
                            String uri = content.getString("uri");
                            double priority = (content.getDouble("priority") == null ? 1.01 : content.getDouble("priority"));
                            ArrayList<Article> extractedArticles = Parser.engines.batch(uri, priority);
                            if (extractedArticles != null) {
                                if (content.getBoolean("insert") != null && content.getBoolean("insert")) {
                                    articles.createMany(extractedArticles);
                                }
                                return new Result(this.command, "articles", extractedArticles);
                            }
                            return new Result(404, this.command, "NO ARTICLES");
                        }
                        case "uri" -> {
                            String uri = content.getString("uri");
                            if (uri != null && !uri.isEmpty()) {
                                if(articles.withLink(uri) == null){
                                    new Parser(session);
                                    Article parsed = Parser.engines.parse(uri);
                                    if(parsed != null){
                                        if (content.getBoolean("insert") != null && content.getBoolean("insert")) {
                                            articles.create(parsed);
                                        }
                                        return new Result(this.command, "article", parsed);
                                    }
                                    return new Result(this.command, "parser", "FAILED");
                                }
                                return new Result(410, this.command, "TAKEN");
                            }
                            return new Result(428, this.command, "URI REQUIRED");
                        }
                        case "crawl" -> {
                            String url = content.getString("uri");
                            String desc = (content.getString("description") == null ? "Webpage" : content.getString("description"));
                            int depth = (content.getInteger("depth") == null ? 1 : content.getInteger("depth"));
                            if (url != null && !url.isEmpty()) {
                                Parser parser = new Parser(session);
                                parser.purge();
                                int limit = (content.getInteger("limit") == null ? Integer.MAX_VALUE : content.getInteger("limit"));
                                boolean allowExternalCrawl = (content.getBoolean("allow_external") != null && content.getBoolean("allow_external"));
                                Parser.engines.crawl(url, limit, depth, desc, allowExternalCrawl);
                                return new Result(this.command, "articles", parser.getTraversable());
                            }
                            return new Result(428, this.command, "URI REQUIRED");
                        }
                        case "text" -> {
                            String text = content.getString("text");
                            Andromeda.encoder.tokenize(text, false);
                        }
                        case "audit" -> {
                            String q = (content.getString("q") == null ? "" : content.getString("q"));
                            ArrayList<String> ids = new ArrayList<>();
                            articles.getIds(q).forEach(a -> ids.add(new Article(a).getId()));
                            return new Result(this.command, "ids", "" + ids);
                        }
                        case "recursive" -> {
                            int start = (content.getInteger("start") == null ? 0 : content.getInteger("start"));
                            Parser.engines.recursive(session, start);
                        }
                    }
                }
                return new Result(428, this.command, "PARSER MODE REQUIRED");
            }
            return new Result(428, this.command, "JSON BODY EXPECTED");
        }
        /*
         * Connect: Creating or logging in user
         */
        else if(primarySelector.equals("connect")){
            if(content != null){
                String email = content.getString("email");
                UsersClient u = new UsersClient();
                if(u.userExistsWithEmail(email)){
                    User user = u.getUserWithEmail(email);
                    if(user.getPermissions() == 0){
                        if(u.emailCode(user)){
                            return new Result(200, "EMAIL CODE SENT");
                        }
                        return new Result(501, "FAILED EMAIL CODE");
                    }else if(user.getPermissions() >= 1){
                        if(u.textCode(user)){
                            return new Result(200, "TEXT CODE SENT");
                        }
                        return new Result(501, "FAILED TEXT CODE");
                    }
                }
                return new Result(404, "USER NOT FOUND");
            }
            return new Result(428, "JSON BODY EXPECTED");
        }
        /*
         * Verify: Unlocking account using one-time 2fa token
         */
        else if(primarySelector.startsWith("verify")){
            if(content != null){
                String email = content.getString("email");
                String code = Telifie.tools.make.md5(content.getString("code"));
                UsersClient users = new UsersClient();
                if(users.userExistsWithEmail(email)){
                    User user = users.getUserWithEmail(email);
                    if(user.hasToken(code)){
                        if(user.getPermissions() == 0 || user.getPermissions() == 1){
                            users.upgradePermissions(user);
                            user.setPermissions(user.getPermissions() + 1);
                        }
                        Authentication auth = new Authentication(user);
                        AuthenticationClient auths = new AuthenticationClient();
                        auths.authenticate(auth);
                        JSONObject json = new JSONObject(user.toString());
                        json.put("authentication", auth.toJson());
                        return new Result(this.command, "user", json);
                    }
                    return new Result(403, "INVALID CODE");
                }
                return new Result(404, "USER NOT FOUND");
            }
            return new Result(404, this.command, "JSON BODY EXPECTED");
        }
        /*
         * Timelines: Accessing the timelines (history) of objects
         */
        else if(primarySelector.equals("timelines")){

            if(this.selectors.length >= 2){
                TimelinesClient timelines = new TimelinesClient(session);
                Timeline timeline = timelines.getTimeline(objectSelector);
                if(timeline != null){
                    return new Result(this.command, "timeline", timeline);
                }
                return new Result(404, this.command, "TIMELINE NOT FOUND");
            }
            return new Result(428, this.command, "OBJECT ID REQUIRED");

        }
        /*
         * Connectors: Adding, managing, checking
         */
        else if(primarySelector.equals("connectors")){

            ConnectorsClient connectors = new ConnectorsClient(session);
            if(content != null){
                Connector connector = new Connector(content);
                connector.setUser(actingUser);
                boolean connectorUsed = connectors.exists(connector);
                if(connector.getId().equals("com.telifie.connectors.spotify")){
                    Spotify spotify;
                    try {
                        spotify = new Spotify(connector);
                        if(!connectorUsed){ //User hasn't used this connector before
                            connectors.create(spotify.getConnector());
                        }
                        spotify.parse(session);
                        return new Result(this.command, "spotify", "done");
                    } catch (IOException | ParseException | SpotifyWebApiException e) {
                        return new Result(501, this.command, "FAILED PARSING SPOTIFY");
                    }
                }else{
                    if(connectorUsed){
                        return new Result(409, this.command, "CONNECTOR EXISTS");
                    }
                    connectors.create(connector);
                }
            }
            if(this.selectors.length >= 2){
                if(objectSelector.equals("connected")){
                    return new Result(this.command, "connectors", connectors.mine());
                }
                Connector connector = connectors.getConnector(objectSelector);
                if(connector != null){
                    return new Result(this.command, "connector", connector);
                }
                return new Result(404, this.command, "CONNECTOR NOT FOUND");
            }
            return new Result(428, this.command, "CONNECTOR NAME REQUIRED");
        }
        /**
         * For receiving messages through Twilio
         */
        else if(primarySelector.equals("messaging")){
            String from  = content.getString("From");
            String message = content.getString("Body");

        }
        return new Result(200, this.command, "NO COMMAND RECEIVED");
    }
}