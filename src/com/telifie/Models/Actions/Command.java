package com.telifie.Models.Actions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.telifie.Models.*;
import com.telifie.Models.Connectors.Twilio;
import com.telifie.Models.Parser;
import com.telifie.Models.Clients.*;
import com.telifie.Models.Clients.Packages;
import com.telifie.Models.Utilities.*;
import com.telifie.Models.Utilities.Package;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Command {

    private final String command;
    private final String[] selectors;

    public Command(String command){
        this.command = command;
        this.selectors = this.command.split("(?!//)/");
    }

    private String get(int index){
        return this.selectors[index].replaceAll("/", "");
    }

    private boolean hasDomainPermission(User user, Session session, boolean allowPublicDomainAction){
        if((user.getPermissions() >= 12 && session.getDomain().equals("telifie") && !allowPublicDomainAction) || (session.getDomain().equals("telifie") && allowPublicDomainAction)){
            //TODO, share changes with data team for approval and change status on Article
            return true;
        }else if(session.getDomain().equals("telifie") && !allowPublicDomainAction){
            return false;
        }else if(!session.getDomain().equals("telifie")){
            //TODO check separate permissions
            return false;
        }
        return false;
    }

    public Result parseCommand(Session session, Document content){
        String selector = this.selectors[0];
        String objSelector = (this.selectors.length > 1 ? this.get(1) : "");
        String secSelector = (this.selectors.length > 2 ? this.get(2) : null);
        String terSelector = (this.selectors.length > 3 ? this.get(3) : null);
        if(selector.equals("search")){
            if(content != null){
                String query = (content.getString("query") == null ? "" : content.getString("query"));
                if(query.isEmpty()){
                    return new Result(428, this.command, "Query expected");
                }
                String targetDomain = (content.getString("domain") == null ? "telifie" : content.getString("domain"));
                if(!targetDomain.equals("telifie")){
                    try{
                        Domains domains = new Domains(session);
                        Domain domain = domains.withId(targetDomain);
                        session.setDomain(domain.id);
                    }catch (NullPointerException n){
                        return new Result(410, this.command, "NOT FOUND");
                    }
                }
                try {
                    Parameters params = new Parameters(content);
                    return new Search().execute(session, query, params);
                }catch(NullPointerException n){
//                    return new Result(505, this.command, "SEARCH ERROR");
                    throw new RuntimeException(n);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
            return new Result(428, this.command, "JSON BODY EXPECTED");
        }else if(selector.equals("domains")){
            if(this.selectors.length >= 2){ //telifie.com/domains/{owner|member|create|update}
                Domains domains = new Domains(session);
                ArrayList<Domain> foundDomains;
                if(objSelector.equals("owner")){
                    foundDomains = domains.mine();
                    return new Result(this.command, "domains", foundDomains);
                }else if(objSelector.equals("member")){ //Domains they're a member of
//                    User user = new UsersClient().getUserWithId(session.user);
//                    foundDomains = domains.forMember(user.getEmail());
//                    return new Result(this.command, "domains", foundDomains);
                }else if(objSelector.equals("protected")){ //Domains they're a member of
//                    User user = new UsersClient().getUserWithId(session.user);
//                    foundDomains = domains.forMember(user.getEmail());
//                    return new Result(this.command, "domains", foundDomains);
                }else if(objSelector.equals("create")){ //Creating a new domain
                    if(content != null){
                        String domainName;
                        if((domainName = content.getString("name")) != null && content.getInteger("permissions") != null){
                            Domain newDomain = new Domain(session.user, domainName, content.getInteger("permissions"));
                            if(domains.create(newDomain)){
                                return new Result(this.command, "domain", newDomain);
                            }
                            return new Result(505, this.command, "FAILED DOMAIN CREATION");
                        }
                        return new Result(428, this.command, "DOMAIN INFO NOT PROVIDED");
                    }
                    return new Result(428, this.command, "JSON BODY EXPECTED");
                }else if(this.selectors.length == 3){ //telifie.com/domains/{delete|id}/{domainID}
                    try{
                        Domain d = domains.withId(secSelector);
                        switch (objSelector) {
                            case "delete" -> {
                                if (d.owner.equals(session.user)) {
                                    if (domains.delete(d)) {
                                        return new Result(200, this.command, "DOMAIN DELETED");
                                    }
                                    return new Result(505, this.command, "FAILED DOMAIN DELETION");
                                }
                                return new Result(401, this.command, "DOMAIN ACCESS DENIED");
                            }
                            case "id" -> {
                                if(d.owner.equals(session.user)){
                                    return new Result(this.command, "domain", d);
                                }
                                return new Result(403, this.command, "DOMAIN ACCESS DENIED");
                            }
                            case "check" -> {
                                int p = d.getPermissions(session.user);
                                if(p == 0){
                                    return new Result(200, this.command, "OWNER");
                                }else if(p == 1){
                                    return new Result(206, this.command, "VIEWER");
                                }else if(p == 2){
                                    return new Result(207, this.command, "EDITOR");
                                }
                                return new Result(403, this.command, "DOMAIN ACCESS DENIED");
                            }
                            case "update" -> {
                                if (content != null) {
//                                    if (domains.update(d, content)) { //TODO content check
//                                        return new Result(this.command, "domain", d);
//                                    }
                                }
                                return new Result(428, this.command, "JSON BODY EXPECTED");
                            }
                        }
                        return new Result(404, this.command, "BAD DOMAIN OPTION");
                    }catch (NullPointerException n){
                        return new Result(404, this.command, "DOMAIN NOT FOUND");
                    }
                }else if(this.selectors.length >= 4){ //'/domains/{id}/users/{add|remove}'
                    try{
                        Domain d = domains.withId(objSelector);
                        if(content != null){
                            switch (terSelector) {
                                case "add" -> {
                                    if(domains.addUser(d, content.getString("user"), content.getInteger("permissions"))){
                                        return new Result(200, this.command, "ADDED USERS TO DOMAIN");
                                    }
                                    return new Result(505, this.command, "FAILED ADDING USER FROM DOMAIN");
                                }
                                case "remove" -> {
                                    if (domains.removeUser(d, this.selectors[4])) {
                                        return new Result(200, this.command, "REMOVED USERS FROM DOMAIN");
                                    }
                                    return new Result(505, this.command, "FAILED REMOVING USER FROM DOMAIN");
                                }
                                case "update" -> {
                                    if (domains.updateUser(d, this.selectors[4], 0)) {
                                        return new Result(200, this.command, "DOMAIN USERS UPDATED");
                                    }
                                    return new Result(505, this.command, "FAILED DOMAIN USER UPDATE");
                                }
                            }
                            return new Result(428, this.command, "BAD DOMAIN USER OPTION");
                        }
                        return new Result(428, this.command, "JSON BODY EXPECTED");
                    }catch (NullPointerException n){
                        return new Result(404, this.command, "DOMAIN NOT FOUND");
                    }
                }
            }
            return new Result(200, this.command, "BAD DOMAINS OPTION");
        }else if(selector.equals("articles")){
            User user = new Users().getUserWithId(session.user);
            if(content != null){
                if(content.getString("domain") != null){
                    String targetDomain = content.getString("domain");
                    try{
                        Domain domain = new Domains(session).withId(targetDomain);
                        session.setDomain(domain.id);
                    }catch (NullPointerException n) {
                        return new Result(404, this.command, "DOMAIN NOT FOUND");
                    }
                }
            }
            Articles articles = new Articles(session);
            if(this.selectors.length >= 3){
                try {
                    Article a = articles.withId(secSelector);
                    switch (objSelector) {
                        case "id" -> {
                            try {
                                if(hasDomainPermission(user, session, true)){
                                    CompletableFuture.runAsync(() -> Cache.history.log(session.user, a.getId()));
                                    return new Result(this.command, "article", articles.withId(secSelector));
                                }
                                return new Result(401, this.command, "INSUFFICIENT PERMISSIONS");
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
                                if(hasDomainPermission(user, session, false)){
                                    if (articles.update(a, ua)) {
                                        return new Result(200, this.command, ua.toString());
                                    }
                                    return new Result(204, this.command, "NO NEW ARTICLE");
                                }
                                return new Result(401, this.command, "INSUFFICIENT PERMISSIONS");
                            }
                            return new Result(428, this.command, "JSON BODY EXPECTED");
                        }
                        case "delete" -> {
                            if(hasDomainPermission(user, session, false)){
                                if (articles.delete(articles.withId(secSelector))) {
                                    return new Result(200, this.command, "");
                                }
                                return new Result(505, this.command, "FAILED ARTICLE DELETION");
                            }
                            return new Result(401, this.command, "INSUFFICIENT PERMISSIONS");
                        }
                        case "duplicate" -> {
                            if (this.selectors.length > 3) {
                                Domains domains = new Domains(session);
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
                        case "move" -> {
                            if (this.selectors.length > 3) {
                                Domains domains = new Domains(session);
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
                            if(hasDomainPermission(user, session, false)){
                                if (articles.verify(secSelector)) {
                                    return new Result(200, this.command, "");
                                }
                                return new Result(505, this.command, "FAILED ARTICLE UPDATE");
                            }
                            return new Result(401, this.command, "INSUFFICIENT PERMISSIONS");
                        }
                    }
                    return new Result(404, this.command, "BAD ARTICLES OPTION");
                }catch (NullPointerException n){
                    return new Result(404, this.command, "ARTICLE NOT FOUND");
                }
            }else if(objSelector.equals("create")){
                if(content != null){
                    try {
                        if(hasDomainPermission(user, session, false)){
                            Article na = new Article(content);
                            if(articles.create(na)){
                                return new Result(this.command, "article", na);
                            }
                            return new Result(505, this.command, "FAILED ARTICLE CREATION");
                        }
                        return new Result(401, this.command, "INSUFFICIENT PERMISSIONS");
                    }catch(JSONException e){
                        return new Result(505, this.command, "BAD ARTICLE JSON");
                    }
                }
                return new Result(428, this.command, "ARTICLE JSON DATA EXPECTED");
            }else if(objSelector.equals("audit")){
                String query = (content.getString("query") == null ? "" : content.getString("query"));
                ArrayList<Article> auditable = articles.get(new Document("$and", Arrays.asList(
                        Search.filter(query, new Parameters(content)),
                        new Document("verified", false)
                )), new Document("_id", -1));
                ArrayList<String> ids = new ArrayList<>();
                for(Article a : auditable){
                    ids.add(a.getId());
                }
                return new Result(this.command, "articles", new JSONArray(ids));
            }
            return new Result(this.command,"stats", Telifie.stats);
        }else if(selector.equals("shortcuts")){
            Shortcuts scs = new Shortcuts(session);
            if(this.selectors.length >= 4){ //Saving/Unsaving articles in shortcut
                try{
                    Shortcut c = scs.get(secSelector);
                    try{
                        Articles articles = new Articles(session);
                        Article a = articles.withId(terSelector);
                        if(objSelector.equals("save")){
                            if(scs.save(c, a)){
                                return new Result(200, this.command, "ARTICLE SAVED");
                            }
                            return new Result(505, this.command, "FAILED ARTICLE SAVE");
                        } else if(objSelector.equals("unsave")){
                            if(scs.unsave(c, a)){
                                return new Result(200, this.command, "ARTICLE UNSAVED");
                            }
                            return new Result(505, this.command, "FAILED ARTICLE UNSAVE");
                        }
                        return new Result(404, this.command, "BAD SHORTCUT OPTION");
                    }catch (NullPointerException n){
                        return new Result(404, this.command, "ARTICLE NOT FOUND");
                    }
                }catch (NullPointerException n){
                    return new Result(404, this.command, "SHORTCUT NOT FOUND");
                }
            }else if(this.selectors.length >= 2){ //Updating, deleting, getting shortcuts
                if(secSelector != null) {
                    try {
                        Shortcut c = scs.get(secSelector);
                        switch (objSelector) {
                            case "update" -> {
                                if (content != null) {
                                    if (content.getString("name") == null) {
                                        return new Result(428, this.command, "SHORTCUT NAME EXPECTED");
                                    }
                                    if(c.getUser().equals(session.user)){
                                        if (scs.update(c, content)) {
                                            return new Result(200, this.command, "SHORTCUT UPDATED");
                                        }
                                        return new Result(505, this.command, "FAILED SHORTCUT UPDATE");
                                    }
                                    return new Result(401, this.command, "NO SHORTCUT PERMISSIONS");
                                }
                                return new Result(428, this.command, "SHORTCUT JSON EXPECTED");
                            }
                            case "delete" -> {
                                if (scs.delete(c)) {
                                    return new Result(200, this.command, "SHORTCUT DELETED");
                                }
                                return new Result(505, this.command, "FAILED SHORTCUT DELETION");
                            }
                            case "id" -> {
                                if(c.getUser().equals(session.user)) {
                                    return new Result(this.command, "shortcut", scs.withArticles(secSelector));
                                }
                                return new Result(401, this.command, "NO SHORTCUT PERMISSIONS");
                            }
                        }
                        return new Result(404, this.command, "BAD SHORTCUTS OPTION");
                    }catch (NullPointerException n){
                        return new Result(404, this.command, "SHORTCUT NOT FOUND");
                    }
                }else if(objSelector.equals("create")){
                    if(content != null){
                        Shortcut newShortcut = new Shortcut(content);
                        Shortcut createdShortcut = scs.create(newShortcut);
                        if(createdShortcut != null){
                            return new Result(this.command, "shortcut", createdShortcut);
                        }
                        return new Result(505, this.command, "FAILED SHORTCUT CREATION");
                    }
                    return new Result(428, this.command, "JSON BODY EXPECTED");
                }
                return new Result(428, this.command, "SHORTCUT ID REQUIRED");
            }
            ArrayList<Shortcut> usersShortcuts = scs.forUser(session.user);
            return new Result(this.command, "shortcuts", usersShortcuts);
        }else if(selector.equals("collections")){

            if(content != null){

            }
            //


        }else if(selector.equals("users")){
            Users users = new Users();
            if(this.selectors.length >= 3){
                if(objSelector.equals("update") && content != null){
                    User changedUser = users.getUserWithId(session.user);
                    if(secSelector.equals("settings")){
                        if(users.updateSettings(changedUser, content.getString("settings"))){
                            return new Result(this.command, "user", changedUser);
                        }
                        return new Result(400, this.command, "Bad Request");
                    }
                    return new Result(404, this.command, "BAD USER OPTION");
                }
                return new Result(404, this.command, "BAD USER OPTION");

            }else if(objSelector.equals("create")){
                if(content != null){
                    User newUser = new User(content.getString("email"), content.getString("name"), content.getString("phone"));
                    if(newUser.getPermissions() == 0 && !newUser.getName().isEmpty() && !newUser.getEmail().isEmpty() && newUser.getName() != null && newUser.getEmail() != null) {
                        if (!users.existsWithEmail(newUser.getEmail())) {
                            //TODO
//                            if (users.create(newUser)) {
//                                return new Result(this.command, "user", newUser);
//                            }
                            return new Result(505, this.command, "FAILED USER CREATION");
                        }
                        return new Result(410, this.command, "EMAIL TAKEN");
                    }
                    return new Result(428, this.command, "NOT ENOUGH INFO");
                }
                return new Result(428, this.command, "JSON BODY EXPECTED");
            }
            Matcher matcher = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", Pattern.CASE_INSENSITIVE).matcher(objSelector);
            if (matcher.matches()) {
                if(users.existsWithEmail(objSelector)){
                    User found = users.getUserWithEmail(objSelector);
                    return new Result(this.command, "user", found);
                }
                return new Result(404, this.command, "USER NOT FOUND");
            }
            return new Result(404, this.command, "INVALID EMAIL");
        }else if(selector.equals("parser")){
            Articles articles = new Articles(session);
            if(content != null){
                String mode = content.getString("mode");
                if(mode != null){
                    String uri;
                    uri = URLDecoder.decode(content.getString("uri"), StandardCharsets.UTF_8);
                    switch (mode) {
                        case "batch" -> {
                            new Parser(session);
                            ArrayList<Article> parsed = Parser.engines.batch(uri, (content.getBoolean("insert") != null && content.getBoolean("insert")));
                            if (parsed != null) {
                                return new Result(this.command, "articles", parsed);
                            }
                            return new Result(404, this.command, "NO ARTICLES");
                        }
                        case "uri" -> {
                            if (uri != null && !uri.isEmpty()) {
                                if(articles.withLink(uri) == null){
                                    Article parsed = new Parser(session).parse(uri);
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
                            if (uri != null && !uri.isEmpty()) {
                                boolean allowExternalCrawl = (content.getBoolean("allow_external") != null && content.getBoolean("allow_external"));
                                new Parser(session);
                                Parser.engines.crawler(uri, allowExternalCrawl);
                                return new Result(200, this.command, "CRAWLING");
                            }
                            return new Result(428, this.command, "URI REQUIRED");
                        }
                    }
                }
                return new Result(428, this.command, "PARSER MODE REQUIRED");
            }
            return new Result(428, this.command, "JSON BODY EXPECTED");
        }else if(selector.equals("connect")){
            if(content != null){
                String email = content.getString("email");
                Users u = new Users();
                if(u.existsWithEmail(email)){
                    User user = u.getUserWithEmail(email);
                    if(user.getPermissions() == 0){
                        if(u.emailCode(user)){
                            return new Result(200, this.command, "EMAIL CODE SENT");
                        }
                        return new Result(501, this.command, "FAILED EMAIL CODE");
                    }else if(user.getPermissions() >= 1){
                        if(u.textCode(user)){
                            return new Result(200, this.command, "TEXT CODE SENT");
                        }
                        return new Result(501, this.command, "FAILED TEXT CODE");
                    }
                }
                return new Result(404, this.command, "USER NOT FOUND");
            }
            return new Result(428, this.command, "JSON BODY EXPECTED");
        }else if(selector.startsWith("verify")){
            if(content != null){
                String email = content.getString("email");
                String code = Telifie.md5(content.getString("code"));
                Users users = new Users();
                if(users.existsWithEmail(email)){
                    User user = users.getUserWithEmail(email);
                    if(user.hasToken(code)){
                        if(user.getPermissions() == 0 || user.getPermissions() == 1){
                            users.upgradePermissions(user, (user.getPermissions() + 1));
                            user.setPermissions(user.getPermissions() + 1);
                        }
                        Authentication auth = new Authentication(user);
                        auth.authenticate();
                        JSONObject json = new JSONObject(user.toString());
                        json.put("authentication", new JSONObject(auth.toString()));
                        return new Result(this.command, "user", json);
                    }
                    return new Result(403, this.command, "INVALID CODE");
                }
                return new Result(404, this.command, "USER NOT FOUND");
            }
            return new Result(404, this.command, "JSON BODY EXPECTED");
        }else if(selector.equals("timelines")){
            if(this.selectors.length >= 2){
                Timelines timelines = new Timelines(session);
                Timelines.Timeline timeline = timelines.getTimeline(objSelector);
                if(timeline != null){
                    return new Result(this.command, "timeline", timeline);
                }
                return new Result(404, this.command, "TIMELINE NOT FOUND");
            }
            return new Result(428, this.command, "OBJECT ID REQUIRED");
        }else if(selector.equals("connectors")){
            Connectors connectors = new Connectors(session);
            if(content != null){
                Connector connector = new Connector(content);
                connector.setUser(session.user);
                boolean connectorUsed = connectors.exists(connector);
                if(connector.getId().equals("com.telifie.connectors.spotify")){
                    return new Result(501, this.command, "FAILED PARSING SPOTIFY");
                }else{
                    if(connectorUsed){
                        return new Result(409, this.command, "CONNECTOR EXISTS");
                    }
                    connectors.create(connector);
                }
            }
            if(this.selectors.length >= 2){
                if(objSelector.equals("connected")){
                    return new Result(this.command, "connectors", connectors.mine());
                }else if(objSelector.equals("activate")){
                    if(!secSelector.isEmpty()){
                        Package p =  Packages.get(secSelector);
                        if(p != null){
                            return new Result(this.command, "auth", new JSONObject(p.activate()));
                        }
                        return new Result(404, this.command, "CONNECTOR NOT FOUND");
                    }
                }
                Connector connector = connectors.getConnector(objSelector);
                if(connector != null){
                    return new Result(this.command, "connector", connector);
                }
                return new Result(404, this.command, "CONNECTOR NOT FOUND");
            }
            return new Result(428, this.command, "CONNECTOR NAME REQUIRED");
        }else if(selector.equals("messaging")){
            String from  = content.getString("From");
            String message = content.getString("Body");
            Log.console("Income message -> " + message);
            Users users = new Users();
            User user = users.getUserWithPhone(from);
            if(user == null){
                Twilio.send(from, "+15138029566", "The number you are texting from is not registered to a Telifie account.");
                return new Result(404, this.command, "PHONE NUMBER NOT REGISTERED");
            }
            Twilio.send(user.getPhone(), "+15138029566", "Hello " + user.getName() + "!");
            return new Result(200, this.command, "MESSAGE RECEIVED");
        }else if(selector.equals("ping")){
            if(content != null){
                return new Result(200, this.command, "RECEIVED");
            }
            return new Result(428, this.command, "JSON BODY EXPECTED");
        }else if(selector.equals("packages")){
            if(this.selectors.length >= 2){
                try{
                    return new Result(this.command, "package", Packages.get(objSelector));
                }catch (NullPointerException e){
                    return new Result(404, this.command, "PACKAGE NOT FOUND");
                }
            }
            return new Result(this.command, "packages", Packages.getPublic());
        }
        return new Result(200, this.command, "NO COMMAND RECEIVED");
    }
}