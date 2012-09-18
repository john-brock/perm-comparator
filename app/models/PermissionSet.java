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
	// NOTE: currenly having issue with following perms being retrieved: (invalid field message)
	// 			permissionApiUserOnly, InboundTools, OutboundTools, ScheduleJobs

	private EnumSet<validUserPerms> userPerms;
	private EnumSet<validUserPerms> uniqueUserPerms;
	private EnumSet<validUserPerms> commonUserPerms;
	private EnumSet<validUserPerms> differenceUserPerms;

	public PermissionSet() {}

	public PermissionSet(String permsetId) {
		id = permsetId;
		userPerms = EnumSet.noneOf(validUserPerms.class);
		uniqueUserPerms = userPerms;
		commonUserPerms = userPerms;
		differenceUserPerms = userPerms;
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
	public EnumSet<validUserPerms> getDifferenceUserPerms() {
		return differenceUserPerms;
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
	public void setDifferenceUserPerms(EnumSet<validUserPerms> userPerms) {
		this.differenceUserPerms = userPerms;
	}
}
