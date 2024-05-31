package com.telifie.Models.Utilities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.telifie.Models.*;
import com.telifie.Models.Parser;
import com.telifie.Models.Clients.*;
import com.telifie.Models.Clients.Packages;
import com.telifie.Models.Utilities.Network.SQL;
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

    public Result parseCommand(Session session, Document content){
        String selector = this.selectors[0];
        String objSelector = (this.selectors.length > 1 ? this.get(1) : "");
        String secSelector = (this.selectors.length > 2 ? this.get(2) : null);
        String terSelector = (this.selectors.length > 3 ? this.get(3) : null);
        if(selector.equals("search")){
            if(content != null){
                String query = (content.getString("query") == null ? "" : content.getString("query"));
                if(query.isEmpty()){
                    return new Result(428, this.command, "QUERY EXPECTED");
                }
                String targetDomain = (content.getString("domain") == null ? "telifie" : content.getString("domain").trim().toLowerCase());
                Domains domains = new Domains(session);
                Domain domain = domains.withAlias("telifie");
                if(!targetDomain.equals("telifie")){
                    try{
                        domain = domains.withAlias(targetDomain);
                        session.setDomain(targetDomain);
                    }catch (NullPointerException n){
                        return new Result(410, this.command, "DOMAIN NOT FOUND");
                    }
                }
                try {
                    if(domain.hasViewPermissions(session.user)){
                        Parameters params = new Parameters(content);
                        return new Search().execute(session, query, params);
                    }
                    return new Result(401,this.command, "INSUFFICIENT PERMISSIONS");
                }catch(NullPointerException n){
                    return new Result(505, this.command, "SEARCH ERROR");
//                    throw new RuntimeException(n);
                } catch (JsonProcessingException e) {
                    return new Result(428, this.command, "MALFORMED PARAMS");
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
//                    foundDomains = domains.viewable();
//                    return new Result(this.command, "domains", foundDomains);
                }else if(objSelector.equals("protected")){ //Domains they can view

                    foundDomains = domains.viewable();
                    return new Result(this.command, "domains", foundDomains);
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
                                    return new Result(200, this.command, "domain", d);
                                }else if(p == 1){
                                    return new Result(206, this.command, "domain", d);
                                }else if(p == 2){
                                    return new Result(207, this.command, "domain", d);
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
                                        return new Result(200, this.command, "ADDED USER TO DOMAIN");
                                    }
                                    return new Result(505, this.command, "FAILED ADDING USER FROM DOMAIN");
                                }
                                case "remove" -> {
                                    if (domains.removeUser(d, this.selectors[4])) {
                                        return new Result(200, this.command, "REMOVED USER FROM DOMAIN");
                                    }
                                    return new Result(505, this.command, "FAILED REMOVING USER FROM DOMAIN");
                                }
                                case "update" -> {
                                    if (domains.updateUser(d, this.selectors[4], 0)) {
                                        return new Result(200, this.command, "DOMAIN USER UPDATED");
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
            Domains domains = new Domains(session);
            String index = (content.getString("index") == null ? "articles" : content.getString("index").trim().toLowerCase());
            //TODO check index validity
            Domain domain = domains.withAlias("telifie");
            if(content != null){
                if(content.getString("domain") != null){
                    String td = content.getString("domain");
                    try{
                        domain = new Domains(session).withAlias(td);
                        session.setDomain(td);
                    }catch (NullPointerException n) {
                        return new Result(404, this.command, "DOMAIN NOT FOUND");
                    }
                }
            }
            Articles articles = new Articles(session, index);
            if(this.selectors.length >= 3){
                try {
                    Article a = articles.withId(secSelector);
                    switch (objSelector) {
                        case "id" -> {
                            try {
                                if(domain.hasViewPermissions(session.user)){
                                    CompletableFuture.runAsync(() -> SQL.history.log(session.user, a.getId()));
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
                                //TODO compare for timeline
                                if(domain.hasEditPermissions(session.user)){
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
                            if(domain.hasEditPermissions(session.user)){
                                if (articles.delete(articles.withId(secSelector))) {
                                    return new Result(200, this.command, "");
                                }
                                return new Result(505, this.command, "FAILED ARTICLE DELETION");
                            }
                            return new Result(401, this.command, "INSUFFICIENT PERMISSIONS");
                        }
                        case "duplicate" -> {
                            if (this.selectors.length > 3) {
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
                            if(domain.hasEditPermissions(session.user)){
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
                try {
                    Article na = new Article(content);
                    if(domain.hasEditPermissions(session.user)){
                        if(articles.create(na)){
                            return new Result(this.command, "article", na);
                        }
                        return new Result(505, this.command, "FAILED ARTICLE CREATION");
                    }
                    return new Result(401, this.command, "INSUFFICIENT PERMISSIONS");
                }catch(JSONException e){
                    return new Result(505, this.command, "BAD ARTICLE JSON");
                }
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
            return new Result(this.command,"stats", articles.stats());
        }else if(selector.equals("shortcuts")){
            Shortcuts scs = new Shortcuts(session);
            if(selectors.length >= 2){
                String object = secSelector;
                switch (objSelector) {
                    case "save" -> {
                        if(!content.isEmpty()){
                            if(scs.save(new Shortcut(content))){
                                return new Result(200, this.command, "SAVED");
                            }
                            return new Result(50, this.command, "FAILED SAVING SHORTCUT");
                        }
                        return new Result(428, this.command, "JSON BODY EXPECTED");
                    }
                    case "unsave" -> {
                        if(selectors.length >= 3){
                            if(scs.unsave(object)){
                                return new Result(200, this.command, "UNSAVED");
                            }
                            return new Result(50, this.command, "FAILED UNSAVING SHORTCUT");
                        }
                        return new Result(403, this.command, "BAD SHORTCUTS COMMAND");
                    }
                }
            }
            return new Result(this.command, "shortcuts", scs.getShortcuts());
        }else if(selector.equals("indexes")){
            //TODO check permissions
            if(content != null){
                Indexes indexes = new Indexes(session);
                String domainId = content.getString("domain"); //Get domain and check validity
                Log.console(domainId);
                Domains domains = new Domains(session);
                Domain domain = domains.withId(domainId);
                if(!domainId.isEmpty()){
                    if(this.selectors.length >= 2){
                        switch (objSelector) {
                            case "create" -> {
                                String domainName = content.getString("name").toLowerCase().replaceAll(" ", "-"); //Get domain and check validity
                                Index i = indexes.withAlias(domainId, domainName);
                                if(i == null){
                                    if(indexes.create(domain, new Index(content))){
                                        return new Result(200, this.command, "INDEX CREATED");
                                    }
                                    return new Result(500, this.command, "FAILED CREATING INDEX");
                                }
                                return new Result(409, this.command, "INDEX ALREADY EXISTS");
                            }
                            case "id" -> {
                                Index i = indexes.get(secSelector);
                                if(i != null){
                                    return new Result(this.command, "index", i);
                                }
                                return new Result(404, this.command, "INDEX NOT FOUND");
                            }
                        }
                    }
                }
                return new Result(400, this.command, "DOMAIN REQUIRED AS ID");
            }

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
            Articles articles = new Articles(session, "articles");
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