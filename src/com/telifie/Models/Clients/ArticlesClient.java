package com.telifie.Models.Clients;

import com.telifie.Models.Actions.Out;
import com.telifie.Models.Article;
import com.telifie.Models.Domain;
import org.bson.Document;
import java.util.ArrayList;
import java.util.Arrays;

public class ArticlesClient extends Client {

    public ArticlesClient(Domain domain){
        super(domain);
        super.collection = "articles";
    }

    public boolean update(Article article, Article newArticle){
        Out.console(newArticle.toString());
        return super.updateOne(
                new Document("id", article.getId()),
                new Document("$set", Document.parse(newArticle.toString()))
        );
    }

    public boolean create(Article article){

        Document document = Document.parse(article.toString());
        if(super.insertOne(document)){

            return true;
        }else{

            return false;
        }
    }

    public Article get(String articleId){

        return new Article(this.findOne(new Document("id", articleId)));
    }

    public ArrayList<Article> get(Document filter){
        ArrayList<Document> found = this.find(filter);
        ArrayList<Article> articles = new ArrayList<>();
        for(Document doc : found){
            articles.add(new Article(doc));
        }
        return articles;
    }


    public ArrayList<Article> getArticlesWithAttribute(String key, String value){
        ArrayList<Document> found = this.find(
            new Document("$and",
                Arrays.asList(
                        new Document("attributes.key", key ),
                        new Document("attributes.value", value )
                )
            )
        );
        ArrayList<Article> articles = new ArrayList<>();
        for(Document doc : found){
            articles.add(new Article(doc));
        }
        return articles;
    }

    /**
     * Returns total count of Articles in Domain
     * @return int count of articles
     */
    public int count(){
        //TODO
        return 0;
    }

}
