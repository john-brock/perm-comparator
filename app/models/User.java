package models;

import javax.persistence.Entity;

import play.Logger;
import play.db.jpa.Model;

import com.google.gson.JsonObject;

@Entity
public class User extends Model{

    public String Name;
    public String UserId;

    public User() {
    }
	
	public static User get(String userId) {
        return find("id", userId).first();
    }
	
	public void parseFromJson(JsonObject obj)
	{
		Logger.info(obj.toString());
		Name = obj.get("Name").getAsString();
		UserId = obj.get("Id").getAsString().substring(0, 15);
	}
}
