package com.telifie.Models.Clients;

import com.telifie.Models.Connectors.SendGrid;
import com.telifie.Models.Connectors.Twilio;
import com.telifie.Models.User;
import com.telifie.Models.Utilities.Telifie;
import org.bson.Document;

public class UsersClient extends Client {

    public UsersClient() {
        super(null);
        super.collection = "users";
    }

    public User getUserWithEmail(String email){
        return new User(this.findOne(new Document("email", email)));
    }

    public User getUserWithId(String id){
        return new User(this.findOne(new Document("id", id)));
    }

    public User getUserWithPhone(String phone){
        return new User(this.findOne(new Document("phone", phone)));
    }

    public boolean existsWithEmail(String email){
        return this.findOne(new Document("email", email)) != null;
    }

    public boolean lock(User user, String code){
        return this.updateOne(new Document("email", user.getEmail()), new Document("$set", new Document("token", Telifie.md5(code))));
    }

    public boolean emailCode(User user){
        String code = Telifie.digitCode();
        SendGrid.sendCode(user.getEmail(), code);
        return this.lock(user, code);
    }

    public boolean textCode(User user){
        String code = Telifie.digitCode();
        Twilio.send(user.getPhone(), "+15138029566", "Hello \uD83D\uDC4B It's Telifie! Your login code is " + code);
        return this.lock(user, code);
    }

    public boolean updateSettings(User user, String settings){
        return super.updateOne(new Document("id", user.getId()), new Document("$set", new Document("settings", settings)));
    }

    public boolean create(User user){
        return super.insertOne(Document.parse(user.toString()));
    }

    public void upgradePermissions(User user){
        super.updateOne(new Document("email", user.getEmail()), new Document("$set", new Document("permissions", user.getPermissions() + 1)));
    }
}