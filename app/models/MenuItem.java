package models;

import javax.persistence.Entity;

import play.Logger;
import play.db.jpa.Model;

import com.google.gson.JsonObject;

@Entity
public class MenuItem extends Model{

    public String Name;
    public String Id;

    public MenuItem() {
    }
	
	public static MenuItem get(String userId) {
        return find("id", userId).first();
    }
	
	public void parseFromJson(JsonObject obj)
	{
		Logger.info(obj.toString());
		Name = obj.get("Name").getAsString();
		Id = obj.get("Id").getAsString().substring(0, 15);
	}
}
