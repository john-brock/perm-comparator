package models;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.thoughtworks.xstream.converters.enums.EnumSetConverter;

public class PermissionSet {

	private String id = null;
	private String name = null;

	public enum validUserPerms {
		PermissionsApiEnabled, PermissionsAuthorApex, PermissionsBulkApiHardDelete, 
		PermissionsCanUseNewDashboardBuilder, PermissionsConvertLeads, PermissionsCreatePackaging, 
		PermissionsCustomizeApplication, PermissionsCustomSidebarOnAllPages, PermissionsDistributeFromPersWksp, 
		PermissionsEditCaseComments, PermissionsEditEvent, PermissionsEditOppLineItemUnitPrice, 
		PermissionsEditPublicDocuments, PermissionsEditReadonlyFields, PermissionsEditReports, PermissionsEditTask, 
		PermissionsEmailAdministration, PermissionsEmailTemplateManagement, PermissionsEnableNotifications, 
		PermissionsFlowUFLRequired, PermissionsImportLeads, 
		PermissionsInstallPackaging, PermissionsManageAnalyticSnapshots, PermissionsManageAuthProviders, 
		PermissionsManageBusinessHourHolidays, PermissionsManageCallCenters, PermissionsManageCases, 
		PermissionsManageCategories, PermissionsManageCustomReportTypes, PermissionsManageDashboards, 
		PermissionsManageDataCategories, PermissionsManageDataIntegrations, PermissionsManageEmailClientConfig, 
		PermissionsManageLeads, PermissionsManageMobile, PermissionsManageRemoteAccess, PermissionsManageSolutions, 
		PermissionsManageUsers, PermissionsMassInlineEdit, PermissionsModifyAllData, PermissionsNewReportBuilder, 
		PermissionsPasswordNeverExpires, PermissionsPublishPackaging, 
		PermissionsResetPasswords, PermissionsRunReports, PermissionsScheduleReports, 
		PermissionsSendSitRequests, PermissionsSolutionImport, PermissionsTransferAnyCase, PermissionsTransferAnyEntity, 
		PermissionsTransferAnyLead, PermissionsUseTeamReassignWizards, PermissionsViewAllData, PermissionsViewDataCategories, 
		PermissionsViewMyTeamsDashboards, PermissionsViewSetup
	};
	// permissionApiUserOnly, InboundTools, OutboundTools, ScheduleJobs did not retrieve from Query - invalid field error

	private EnumSet<validUserPerms> userPerms;
	private EnumSet<validUserPerms> uniqueUserPerms;
	private EnumSet<validUserPerms> commonUserPerms;

	public PermissionSet() {}

	public PermissionSet(String permsetId) {
		id = permsetId;
		userPerms = EnumSet.noneOf(validUserPerms.class);
		uniqueUserPerms = userPerms;
	}
	
	public PermissionSet(String permsetId, String permsetName) {
		id = permsetId;
		name = permsetName;
		userPerms = EnumSet.noneOf(validUserPerms.class);
		uniqueUserPerms = userPerms;
	}
	
	public String getId() {
		return id;
	}
	public String getName() {
		return name;
	}
	public validUserPerms getValueOfPerm(String perm) {
		return validUserPerms.valueOf(perm);
	}
	public EnumSet<validUserPerms> getUserPerms() {
		return userPerms;
	}
	public EnumSet<validUserPerms> getUniqueUserPerms() {
		return uniqueUserPerms;
	}
	public EnumSet<validUserPerms> getCommonUserPerms() {
		return commonUserPerms;
	}
	

	public void setId(String id) {
		this.id = id;
	}
	public void setName(String name) {
		this.name = name;
	}
	public void setUserPerms(EnumSet<validUserPerms> userPerms) {
		this.userPerms = userPerms;
	}
	public void setUniqueUserPerms(EnumSet<validUserPerms> userPerms) {
		this.uniqueUserPerms = userPerms;
	}
	public void setCommonUserPerms(EnumSet<validUserPerms> userPerms) {
		this.commonUserPerms = userPerms;
	}
}
