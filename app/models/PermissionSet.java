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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.thoughtworks.xstream.converters.enums.EnumSetConverter;

public class PermissionSet {

	private String id = null;
	private String name = null;

	public enum ObjPermCategory {
		original, unique, common, differing
	}
	
	// ObjectPerm data structures and enums
	public enum objectPermissions {
		PermissionsRead, PermissionsEdit, PermissionsCreate, PermissionsDelete, 
		PermissionsViewAllRecords, PermissionsModifyAllRecords
	}
	
	public enum SetupEntityTypes {
		APEX_CLASS("01p", "ApexClass", "Apex Classes"), 
		TABSET("02u", "AppMenuItem", "Apps"),
		CONN_APP("0H4", "AppMenuItem", "Connected Apps"), 
		/*EXT_DS("0??", "ExternalDataSource", "External Data Sources"),*/ 
		APEX_PAGE("066", "ApexPage", "Visualforce Pages");
		
		private final String prefix;
		private final String apiFieldName;
		private final String displayName;
		
		private SetupEntityTypes(final String prefix, final String apiFieldName, final String displayName) {
			this.prefix = prefix;
			this.apiFieldName = apiFieldName;
			this.displayName = displayName;
		}
		
		public String getPrefix() {
			return this.prefix;
		}
		public String getFieldName() {
			return this.apiFieldName;
		}
		public String getDisplayName() {
			return this.displayName;
		}
	}

	// UserPerm data structures
	private Set<String> userPerms;
	private Set<String> uniqueUserPerms;
	private Set<String> commonUserPerms;
	private Set<String> differenceUserPerms;
	
	private Map<ObjPermCategory, Map<String, EnumSet<objectPermissions>>> objPermMap;
	private Map<String, EnumSet<objectPermissions>> emptyMap;

	// SetupEntityAccess data structures - mimics UserPerms
	private Map<ObjPermCategory, Map<SetupEntityTypes, Set<String>>> seaPermMap;
	private Map<SetupEntityTypes, Set<String>> emptySeaMap;
	
	public PermissionSet() {}

	public PermissionSet(String permsetId) {
		id = permsetId;
		userPerms = new HashSet<String>();
		uniqueUserPerms = userPerms;
		commonUserPerms = userPerms;
		differenceUserPerms = userPerms;
		
		objPermMap = Maps.newHashMap();
		emptyMap = Maps.newHashMap();
		objPermMap.put(ObjPermCategory.original, emptyMap);
		objPermMap.put(ObjPermCategory.unique, emptyMap);
		objPermMap.put(ObjPermCategory.common, emptyMap);
		objPermMap.put(ObjPermCategory.differing, emptyMap);
		
		seaPermMap = Maps.newHashMap();
		emptySeaMap = Maps.newHashMap();
		for (SetupEntityTypes type : SetupEntityTypes.values()) {
			emptySeaMap.put(type, Sets.<String>newHashSet());
		}
		seaPermMap.put(ObjPermCategory.original, Maps.newHashMap(emptySeaMap));
		seaPermMap.put(ObjPermCategory.unique, Maps.newHashMap(emptySeaMap));
		seaPermMap.put(ObjPermCategory.common, Maps.newHashMap(emptySeaMap));
		seaPermMap.put(ObjPermCategory.differing, Maps.newHashMap(emptySeaMap));
	}
	
	public String getId() {
		return id;
	}
	public String getName() {
		return name;
	}

	public Set<String> getUserPerms() {
		return this.userPerms;
	}

	public Set<String> getUniqueUserPerms() {
		return this.uniqueUserPerms;
	}

	public Set<String> getCommonUserPerms() {
		return this.commonUserPerms;
	}

	public Set<String> getDifferenceUserPerms() {
		return this.differenceUserPerms;
	}
	
	public Map<String, EnumSet<objectPermissions>> getOjPermMap(ObjPermCategory category) {
		if (this.objPermMap.containsKey(category)) {
			return this.objPermMap.get(category);
		}
		else return null;
	}
	
	public Map<SetupEntityTypes, Set<String>> getSeaPermMap(ObjPermCategory category) {
		if (this.seaPermMap.containsKey(category)) {
			return this.seaPermMap.get(category);
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
	public void setSeaPermMap(ObjPermCategory category, Map<SetupEntityTypes, Set<String>> seaPermMap) {
		this.seaPermMap.put(category, seaPermMap);
	}
}
