package com.telifie.Models.Utilities;

/**
 * Helper class for text normalization
 */
public class Statement {

    private String content, normalized;
    private int type = 0;
    public final int DECLARATIVE = 0, INTERROGATIVE = 1, IMPERATIVE = 2, EXCLAMATIVE = 3;
    public String[] interrogatives = {"who", "what", "when", "where", "why", "how", "does", "do"};
    public String[] exclamations = {"!", "wow", };
    public String[] stopWords = {};

    public Statement(String content) {
        this.content = content;
        this.normalized = content.toLowerCase().trim();
        if(this.startWith(interrogatives)){
            this.type = this.INTERROGATIVE;
        }
    }

    private boolean startWith(String[] values){
        for(int i = 0; i < values.length; i++){
            if(this.normalized.startsWith(values[i])){
                return true;
            }
        }
        return false;
    }

    public String getContent() {
        return content;
    }

    public String getNormalized() {
        return normalized;
    }

   public boolean isDeclarative(){
       return (this.type == 0 ? true : false);
   }

    public boolean isInterrogative(){
        return (this.type == 1 ? true : false);
    }

    public boolean isImperative(){
        return (this.type == 2 ? true : false);
    }

    public boolean isExclamatory(){
        return (this.type == 3 ? true : false);
    }

    public boolean isUrl(){
        if(this.normalized.startsWith("https://") || this.normalized.startsWith("www") || this.normalized.startsWith("http://")){
            return true;
        }
        return false;
    }

    public boolean isFile(){
        if(this.normalized.startsWith("file://") ||this.normalized.startsWith("/")){
            return true;
        }
        return false;
    }

}
