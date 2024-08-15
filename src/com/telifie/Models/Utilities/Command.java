package com.telifie.Models.Utilities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.telifie.Models.*;
import com.telifie.Models.Parser;
import com.telifie.Models.Clients.*;
import com.telifie.Models.Utilities.Network.SQL;
import org.bson.Document;
import org.json.JSONException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

    public Result parseCommand(Session session, Document content, String type){
        String selector = this.selectors[0];
        String objSelector = (this.selectors.length > 1 ? this.get(1) : "");
        String secSelector = (this.selectors.length > 2 ? this.get(2) : null);
        String terSelector = (this.selectors.length > 3 ? this.get(3) : null);
        String targetIndex = ""; //Index Alias
        Domains domains = new Domains();
        Domain targetDomain = null;
        if(content != null){
            String sdn = (content.getString("domain") == null ? "telifie" : content.getString("domain").trim().toLowerCase());
            targetIndex = (content.getString("index") == null ? "articles" : content.getString("index").trim().toLowerCase());
            if(!sdn.equals("telifie") && !sdn.isEmpty()){
                    try{
                        try {
                            if(!targetDomain.hasViewPermissions(session.user)){
                                return new Result(401,this.command, "INSUFFICIENT PERMISSIONS");
                            }
                            targetDomain = domains.withAlias(sdn);
                            session.setDomain(sdn);
                        } catch(NullPointerException n){
                            return new Result(505, this.command, "SEARCH ERROR");
                        }
                    }catch (NullPointerException n){
                        return new Result(410, this.command, "DOMAIN NOT FOUND");
                    }
            }else{
                targetDomain = domains.withAlias("telifie");
            }
        }

        if(selector.isEmpty()){
            if(content != null){
                String q = (content.getString("query") == null ? "" : content.getString("query").toLowerCase().trim());
                if(!Telifie.tools.startsWith(new String[]{"who", "what", "when", "where", "why", "how"}, q) && !q.startsWith("@")){
                    Articles ar = new Articles(session, "articles");
                    ArrayList<Article> a = ar.get(new Document("title", Pattern.compile("^" + Pattern.quote(q) + "$", Pattern.CASE_INSENSITIVE)));
                    return new Result(this.command, "articles", a);
                }else if((q.contains("*") || q.contains("+") || q.contains("-") || q.contains("/")) || Telifie.tools.contains(Telifie.NUMERALS, q)){
                    String mathExpressionPattern = "[\\d\\s()+\\-*/=xX^sincoaet]+";
                    Pattern pattern = Pattern.compile(mathExpressionPattern);
                    Matcher matcher = pattern.matcher(q);
                    if(matcher.find()) {
                        return new Result(210, this.command, "NO MATH SERVICE INSTALLED");
                    }
                }else{
                    try {
                        Parameters params = new Parameters(content);
                        return new Search().execute(session, q, params);
                    } catch(NullPointerException n){
                        return new Result(505, this.command, "SEARCH ERROR");
                    }
                    catch (JsonProcessingException e) {
                        return new Result(428, this.command, "MALFORMED PARAMS");
                    }
                }
            }
            return new Result(428, this.command, "NO QUERY PROVIDED");

        }else if(selector.equals("search")){
            if(content != null){
                String q = (content.getString("query") == null ? "" : content.getString("query"));
                try {
                    Parameters params = new Parameters(content);
                    return new Search().execute(session, q, params);
                } catch(NullPointerException n){
                    return new Result(505, this.command, "SEARCH ERROR");
                } catch (JsonProcessingException e) {
                    return new Result(428, this.command, "MALFORMED PARAMS");
                }
            }
            return new Result(428, this.command, "JSON BODY EXPECTED");

        }else if(selector.equals("articles")){
            Articles articles = new Articles(session, targetIndex);
            if(this.selectors.length >= 3){
                try {
                    Article a = articles.withId(secSelector);
                    switch (objSelector) {
                        case "id" -> {
                            try {
                                if(targetDomain.hasViewPermissions(session.user)){
                                    CompletableFuture.runAsync(() -> SQL.log(session.user, a.getId()));
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
                                if(targetDomain.hasEditPermissions(session.user)){
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
                            if(targetDomain.hasEditPermissions(session.user)){
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
                            if(targetDomain.hasEditPermissions(session.user)){
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
                    if(targetDomain.hasEditPermissions(session.user)){
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
            return new Result(this.command,"stats", articles.stats());

        }else if(selector.equals("parser")){
            Articles articles = new Articles(session, targetIndex);
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

                }
                return new Result(404, this.command, "USER NOT FOUND");
            }
            return new Result(428, this.command, "JSON BODY EXPECTED");
        }
        return new Result(200, this.command, "RECEIVED");
    }
}