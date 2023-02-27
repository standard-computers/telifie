package com.telifie.Models.Clients;

import com.telifie.Models.Domain;
import com.telifie.Models.User;
import com.telifie.Models.Utilities.Tool;
import org.bson.Document;

public class UsersClient extends Client {

    public UsersClient(String mongoUri) {
        super(mongoUri);
        super.collection = "users";
    }

    public UsersClient(Domain domain) {
        super(domain);
        super.collection = "users";
    }

    public User getUserWithEmail(String email){

        return new User(this.findOne(new Document("email", email)));
    }

    public User getUserWithId(String id){

        return new User(this.findOne(new Document("id", id)));
    }

    public boolean userExists(String id){

        return this.findOne(new Document("id", id)) != null;
    }

    public boolean userExistsWithEmail(String email){

        return this.findOne(new Document("email", email)) != null;
    }

    public boolean lockUser(User user, String code){

        return this.updateOne(new Document("email", user.getEmail()), new Document("$set", new Document("token", Tool.md5(code))));
    }

    public boolean updateUserTheme(User user, Document update){

        return super.updateOne(
            new Document("email", user.getEmail()),
            new Document("$set",
                    new Document("theme", update.get("theme", Document.class))
            )
        );
    }

    public boolean createUser(User user){

        return super.insertOne(Document.parse(user.toString()));
    }

    public boolean upgradePermissions(User user){

        return super.updateOne(
            new Document("email", user.getEmail()),
                new Document("$set",
                    new Document("permissions", user.getPermissions() + 1)
                )
        );
    }
}
