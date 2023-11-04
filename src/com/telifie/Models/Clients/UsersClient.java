package com.telifie.Models.Clients;

import com.telifie.Models.Connectors.SendGrid;
import com.telifie.Models.Connectors.Twilio;
import com.telifie.Models.User;
import com.telifie.Models.Utilities.Telifie;
import com.telifie.Models.Utilities.Theme;
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

    public boolean userExistsWithEmail(String email){
        return this.findOne(new Document("email", email)) != null;
    }

    public boolean lock(User user, String code){
        return this.updateOne(new Document("email", user.getEmail()), new Document("$set", new Document("token", Telifie.md5(code))));
    }

    public boolean emailCode(User user){
        String code = Telifie.simpleCode();
        SendGrid.sendCode(user.getEmail(), code);
        return this.lock(user, code);
    }

    public boolean textCode(User user){
        String code = Telifie.simpleCode();
        Twilio.send(user.getPhone(), "+15138029566", "Hello \uD83D\uDC4B It's Telifie! Your login code is " + code);
        return this.lock(user, code);
    }

    public boolean updateUserTheme(User user, Theme theme){
        return super.updateOne(new Document("id", user.getId()), new Document("$set", new Document("theme", Document.parse(theme.toString()))));
    }

    public boolean updateUserPhoto(User user, String photoUri){
        return super.updateOne(new Document("id", user.getId()), new Document("$set", new Document("photo", photoUri)));
    }

    public boolean createUser(User user){
        return super.insertOne(Document.parse(user.toString()));
    }

    public void upgradePermissions(User user){
        super.updateOne(new Document("email", user.getEmail()), new Document("$set", new Document("permissions", user.getPermissions() + 1)));
    }
}