package models;

import javax.persistence.Entity;

import play.Logger;
import play.db.jpa.Model;

import com.google.gson.JsonObject;

@Entity
public class Account extends Model{

    public String Name;
    public String recordId;
    public String AccountNumber;
    public Integer NumberOfEmployees;

    public Account() {
    }
	
	public static Account get(String acctId) {
        return find("id", acctId).first();
    }
	
	public void parseFromJson(JsonObject obj)
	{
		Name = obj.get("Name").getAsString();
		recordId = obj.get("Id").getAsString();
		AccountNumber = (obj.get("AccountNumber")!= null)?obj.get("AccountNumber").getAsString():null;
		NumberOfEmployees = (obj.get("NumberOfEmployees")!= null)?obj.get("NumberOfEmployees").getAsInt():null;
	}
}
