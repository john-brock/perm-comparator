package models;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import models.PermissionSet.objectPermissions;

import org.apache.commons.collections.map.HashedMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.thoughtworks.xstream.converters.enums.EnumSetConverter;

public class PermissionSet {

	private String id = null;
	private String name = null;

	private Set<String> userPerms;
	private Set<String> uniqueUserPerms;
	private Set<String> commonUserPerms;
	private Set<String> differenceUserPerms;
	
	public enum objectPermissions {
		PermissionsRead, PermissionsEdit, PermissionsCreate, PermissionsDelete, 
		PermissionsViewAllRecords, PermissionsModifyAllRecords
	}
	
	public enum ObjPermCategory {
		original, unique, common, differing
	}
	
	private Map<ObjPermCategory, Map<String, EnumSet<objectPermissions>>> objPermMap;
	private Map<String, EnumSet<objectPermissions>> emptyMap;
	
	public PermissionSet() {}

	public PermissionSet(String permsetId) {
		id = permsetId;
		userPerms = new HashSet<String>();
		uniqueUserPerms = userPerms;
		commonUserPerms = userPerms;
		differenceUserPerms = userPerms;
		
		objPermMap = new HashMap();
		emptyMap = new HashMap();
		objPermMap.put(ObjPermCategory.original, emptyMap);
		objPermMap.put(ObjPermCategory.unique, emptyMap);
		objPermMap.put(ObjPermCategory.common, emptyMap);
		objPermMap.put(ObjPermCategory.differing, emptyMap);
	}
	
	public String getId() {
		return id;
	}
	public String getName() {
		return name;
	}

	public Set<String> getUserPerms() {
		return userPerms;
	}

	public Set<String> getUniqueUserPerms() {
		return uniqueUserPerms;
	}

	public Set<String> getCommonUserPerms() {
		return commonUserPerms;
	}

	public Set<String> getDifferenceUserPerms() {
		return differenceUserPerms;
	}
	
	public Map<String, EnumSet<objectPermissions>> getOjPermMap(ObjPermCategory category) {
		if (objPermMap.containsKey(category)) {
			return objPermMap.get(category);
		}
		else return null;
	}

	public void setId(String id) {
		this.id = id;
	}
	public void setName(String name) {
		this.name = name;
	}
	public void setUserPerms(Set<String> userPerms) {
		this.userPerms = userPerms;
	}
	public void setUniqueUserPerms(Set<String> userPerms) {
		this.uniqueUserPerms = userPerms;
	}
	public void setCommonUserPerms(Set<String> userPerms) {
		this.commonUserPerms = userPerms;
	}
	public void setDifferenceUserPerms(Set<String> userPerms) {
		this.differenceUserPerms = userPerms;
	}
	public void setObjPermMap(ObjPermCategory category, Map<String, EnumSet<objectPermissions>> objPermMap) {
		this.objPermMap.put(category, objPermMap);
	}
}
