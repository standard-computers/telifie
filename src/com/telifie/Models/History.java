package com.telifie.Models;

import org.bson.Document;
import org.json.JSONObject;
import java.util.ArrayList;

public class History {

    public class Action {

        private Type type;
        private int origin;
        private String user, content;

        public enum Type {
            UPDATE, POST, GET, SEARCH, MESSAGE, EMAIL, TEXT, FLAG, DELETE
        }

        public Action(Type type, int origin, String user, String content) {
            this.type = type;
            this.origin = origin;
            this.user = user;
            this.content = content;
        }

        public Type getType() {
            return type;
        }

        public String getUser() {
            return user;
        }

        public int getOrigin() {
            return origin;
        }

        public String getContent() {
            return content;
        }

        @Override
        public String toString() {
            return "{" +
                    "\"type\" : \"" + type + '\"' +
                    ", \"user\" : \"" + user + '\"' +
                    ", \"origin\" : " + origin +
                    ", \"content\" : \"'" + content + '\"' +
                    '}';
        }

        public JSONObject toJson(){
            return new JSONObject(this.toString());
        }

    }

    private String objectId;
    private ArrayList<Action> actions;

    public History(String objectId) {
        this.objectId = objectId;
    }

    public History(Document document){
        this.objectId = document.getString("object_id");
    }

    public String getObjectId() {
        return objectId;
    }


}
