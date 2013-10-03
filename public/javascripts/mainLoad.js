Ext.require(['*']);

Ext.onReady(function() {
	
	// Users, Permission Sets - reader for menu items
	var menuItemReader = new Ext.data.JsonReader({
		totalProperty: 'totalSize',
		successProperty: 'done',
		root: 'records',
		fields : [
			{name: 'Name', type: 'string'},
			{name: 'Id', type: 'string'}
		]
	});	

    Ext.define('menuItem', {
        extend: 'Ext.data.Model',
        fields: [
                 {name: 'Name'},
                 {name: 'Id'}
            	]
    });
    
	// Permsets use Labels as name
	var permsetItemReader = new Ext.data.JsonReader({
		totalProperty: 'totalSize',
		successProperty: 'done',
		root: 'records',
		fields : [
			{name: 'Name', type: 'string'},
			{name: 'Id', type: 'string'}
		]
	});	
	
    Ext.define('permsetItem', {
        extend: 'Ext.data.Model',
        fields: [
                 {name: 'Name', mapping: 'Label'},
                 {name: 'Id'}
            	]
    });
	
	// Profiles different since get profile name, permsetId
	var profileItemReader = new Ext.data.JsonReader({
		totalProperty: 'totalSize',
		successProperty: 'done',
		root: 'records',
		fields : [
			{name: 'Name', type: 'string'},
			{name: 'Id', type: 'string'}
		]
	});	

    Ext.define('profileItem', {
        extend: 'Ext.data.Model',
        fields: [
                 {name: 'Name', mapping: 'Profile.Name'},
                 {name: 'Id'}
            	]
    });

    var userMenuStore = makeStore("Users", menuItemReader, menuItem);
    userMenuStore.load();

    var permsetMenuStore = makeStore("Permsets", permsetItemReader, permsetItem);
    permsetMenuStore.load();

    var profilePermsetMenuStore = makeStore("ProfilePermsets", profileItemReader, profileItem);
    profilePermsetMenuStore.load();
    
    var userView = makeMenuView(userMenuStore);
    var permsetView = makeMenuView(permsetMenuStore);
    var profilePermsetView = makeMenuView(profilePermsetMenuStore);

    var userMenu = Ext.create('Ext.Panel', {
    	title: 'Users',
    	items: userView,
    	autoScroll: true
    });
    
    var permsetMenu = Ext.create('Ext.Panel', {
    	title: 'Permission Sets',
    	items: permsetView,
    	autoScroll: true
    });
    
    var profileMenu = Ext.create('Ext.Panel', {
    	title: 'Profiles',
    	items: profilePermsetView,
    	autoScroll: true
    });
    
    var selectionMenu = Ext.create('Ext.Panel', {
    	cls: 'menu-accordion',
    	region: 'west',
        width: 225,
        margins: '0 0 5 5',
        layout: 'accordion',
        layoutConfig: {animate: true},
        items: [userMenu, permsetMenu, profileMenu]
    });

    // id fields that are set when item dropped and API call made
    id1 = 'blank';
    id2 = 'blank';
    id3 = 'blank';
    id4 = 'blank';
    
    // id elements of each drop column's rowbody for clearing when reseting
    idItem1 = 'blank';
    idItem2 = 'blank';
    idItem3 = 'blank';
    idItem4 = 'blank';
    
    // -- Build drag and drop header --
    var column1Info = [{
    	cName: 'Item 1'
    }];
    var column2Info = [{
    	cName: 'Item 2'
    }];
    var column3Info = [{
    	cName: 'Item 3'
    }];
    var column4Info = [{
    	cName: 'Item 4'
    }];
    
    var dropColumn1 = makeDropColumn(column1Info);
    var dropColumn2 = makeDropColumn(column2Info);
    var dropColumn3 = makeDropColumn(column3Info);
    var dropColumn4 = makeDropColumn(column4Info);
    var dragDropHeader = makeColumnPanel('', dropColumn1, dropColumn2, dropColumn3, dropColumn4, '0');

    // -- Views to fill compare panels --
    // create stores to hold user perms
    var uPermStore1 = makePermStore('permset1_User_Unique');
    var uPermStore2 = makePermStore('permset2_User_Unique');
    var uPermStore3 = makePermStore('permset3_User_Unique');
    var uPermStore4 = makePermStore('permset4_User_Unique');
    
    var uPermCommonStore1 = makePermStore('permset1_User_Common');
    var uPermCommonStore2 = makePermStore('permset2_User_Common');
    var uPermCommonStore3 = makePermStore('permset3_User_Common');
    var uPermCommonStore4 = makePermStore('permset4_User_Common');
    
    var uPermDifferencesStore1 = makePermStore('permset1_User_Differences');
    var uPermDifferencesStore2 = makePermStore('permset2_User_Differences');
    var uPermDifferencesStore3 = makePermStore('permset3_User_Differences');
    var uPermDifferencesStore4 = makePermStore('permset4_User_Differences');
    
    // create stores to hold obj perms
    var objPermStore1 = makeTreeStore('permset1_Object_Unique', 'objPerms1Unique');
    var objPermStore2 = makeTreeStore('permset2_Object_Unique', 'objPerms2Unique');
    var objPermStore3 = makeTreeStore('permset3_Object_Unique', 'objPerms3Unique');
    var objPermStore4 = makeTreeStore('permset4_Object_Unique', 'objPerms4Unique');

    var objPermCommonStore1 = makeTreeStore('permset1_Object_Common', 'objPerms1Common');
    var objPermCommonStore2 = makeTreeStore('permset2_Object_Common', 'objPerms2Common');
    var objPermCommonStore3 = makeTreeStore('permset3_Object_Common', 'objPerms3Common');
    var objPermCommonStore4 = makeTreeStore('permset4_Object_Common', 'objPerms4Common');

    var objPermDifferencesStore1 = makeTreeStore('permset1_Object_Differences', 'objPerms1Diff');
    var objPermDifferencesStore2 = makeTreeStore('permset2_Object_Differences', 'objPerms2Diff');
    var objPermDifferencesStore3 = makeTreeStore('permset3_Object_Differences', 'objPerms3Diff');
    var objPermDifferencesStore4 = makeTreeStore('permset4_Object_Differences', 'objPerms4Diff');

    // main store that will load data to specific user perm stores from 1 main json
    userPermStore = new Ext.data.Store({
    	storeId: 'userPermStoreId',
    	proxy: {
    		type: 'ajax',
    		url : 'permsetDiffs/' + id1 + '/' + id2 + '/' + id3 + '/' + id4 + '/getUserPerms',
	        reader: {		
	    		type: 'json',
	        	fields : [ {name: 'name', type: 'string'} ]
	        }
    	},
    	fields:  [{name: 'name'} /*, {name: 'enabled'}*/],
    	listeners: {
    		load: function(store, records, successful) {
    			// load user perm stores
    			uPermStore1.loadRawData(store.proxy.reader.jsonData);
    			uPermStore2.loadRawData(store.proxy.reader.jsonData);
    			uPermStore3.loadRawData(store.proxy.reader.jsonData);
    			uPermStore4.loadRawData(store.proxy.reader.jsonData);
    			
    			uPermCommonStore1.loadRawData(store.proxy.reader.jsonData);
    			uPermCommonStore2.loadRawData(store.proxy.reader.jsonData);
    			uPermCommonStore3.loadRawData(store.proxy.reader.jsonData);
    			uPermCommonStore4.loadRawData(store.proxy.reader.jsonData);
    			
    			uPermDifferencesStore1.loadRawData(store.proxy.reader.jsonData);
    			uPermDifferencesStore2.loadRawData(store.proxy.reader.jsonData);
    			uPermDifferencesStore3.loadRawData(store.proxy.reader.jsonData);
    			uPermDifferencesStore4.loadRawData(store.proxy.reader.jsonData);
    		}
    	}
    });
    // load store Id so URL can be changed when items dropped for comparison
    userPermStoreId = Ext.StoreMgr.lookup("userPermStoreId");

  // main store that will load data to specific object perm stores from 1 main json
    objPermStore = new Ext.data.Store({
    	storeId: 'objectPermStoreId',
    	proxy: {
    		type: 'ajax',
    		url : 'permsetDiffs/' + id1 + '/' + id2 + '/' + id3 + '/' + id4 + '/getObjPerms'
//	        No reader since getting "raw" jsonData to load to appropriate individual stores
//    		reader: {		
//	    		type: 'json',
//	    		fields: [{name: 'success', type: 'boolean'}, {name: 'text', type:'string'}, {name: 'children', type:'auto'}, {name: 'leaf', type: 'boolean'}, {name: 'expanded', type: 'boolean'}, {name: 'loaded', type:'boolean'}]
//	        }
    	},
		fields: [{name: 'success'}, {name: 'text'}, {name: 'children'}, {name: 'leaf'}, {name: 'expanded'}, {name: 'loaded'}],
    	listeners: {
    		load: function(store, records, successful) {
    			// load object perm stores
    			objPermCommonStore1.proxy.data = store.proxy.reader.jsonData;
    			objPermCommonStore1.load();
    			objPermCommonStore2.proxy.data = store.proxy.reader.jsonData;
    			objPermCommonStore2.load();
    			objPermCommonStore3.proxy.data = store.proxy.reader.jsonData;
    			objPermCommonStore3.load();
    			objPermCommonStore4.proxy.data = store.proxy.reader.jsonData;
    			objPermCommonStore4.load();
    			
    			objPermStore1.proxy.data = store.proxy.reader.jsonData;
    			objPermStore1.load();
    			objPermStore2.proxy.data = store.proxy.reader.jsonData;
    			objPermStore2.load();
    			objPermStore3.proxy.data = store.proxy.reader.jsonData;
    			objPermStore3.load();
    			objPermStore4.proxy.data = store.proxy.reader.jsonData;
    			objPermStore4.load();

    			objPermDifferencesStore1.proxy.data = store.proxy.reader.jsonData;
    			objPermDifferencesStore1.load();
    			objPermDifferencesStore2.proxy.data = store.proxy.reader.jsonData;
    			objPermDifferencesStore2.load();
    			objPermDifferencesStore3.proxy.data = store.proxy.reader.jsonData;
    			objPermDifferencesStore3.load();
    			objPermDifferencesStore4.proxy.data = store.proxy.reader.jsonData;
    			objPermDifferencesStore4.load();
    			
                comparePanel = Ext.getCmp('comparePanelId'); 	// get the actual panel object
                comparePanel.setLoading(false);					// remove loading mask once data is loaded
    		}
    	}
    });
    
    objectPermStoreId = Ext.StoreMgr.lookup("objectPermStoreId");
    
    // views for displaying user perms
    var uPermView1 = makePermView(uPermStore1);
    var uPermView2 = makePermView(uPermStore2);
    var uPermView3 = makePermView(uPermStore3);
    var uPermView4 = makePermView(uPermStore4);
 
    var uPermCommonView1 = makePermView(uPermCommonStore1);
    var uPermCommonView2 = makePermView(uPermCommonStore2);
    var uPermCommonView3 = makePermView(uPermCommonStore3);
    var uPermCommonView4 = makePermView(uPermCommonStore4);
    
    var uPermDifferencesView1 = makePermView(uPermDifferencesStore1);
    var uPermDifferencesView2 = makePermView(uPermDifferencesStore2);
    var uPermDifferencesView3 = makePermView(uPermDifferencesStore3);
    var uPermDifferencesView4 = makePermView(uPermDifferencesStore4);

    // views for displaying obj perms
    var objPermView1 = makeTreeView(objPermStore1);
    var objPermView2 = makeTreeView(objPermStore2);
    var objPermView3 = makeTreeView(objPermStore3);
    var objPermView4 = makeTreeView(objPermStore4);
 
    var objPermCommonView1 = makeTreeView(objPermCommonStore1);
    var objPermCommonView2 = makeTreeView(objPermCommonStore2);
    var objPermCommonView3 = makeTreeView(objPermCommonStore3);
    var objPermCommonView4 = makeTreeView(objPermCommonStore4);
    
    var objPermDifferencesView1 = makeTreeView(objPermDifferencesStore1);
    var objPermDifferencesView2 = makeTreeView(objPermDifferencesStore2);
    var objPermDifferencesView3 = makeTreeView(objPermDifferencesStore3);
    var objPermDifferencesView4 = makeTreeView(objPermDifferencesStore4);
    
    // -- Make compare panels --
    // column panels to hold perms for each of the comparison categories
    var userPermUniques = makeColumnPanel('User Permissions', uPermView1, uPermView2, uPermView3, uPermView4);
    var userPermSims = makeColumnPanel('User Permissions', uPermCommonView1, uPermCommonView2, uPermCommonView3, uPermCommonView4);
    var userPermDiffs = makeColumnPanel('User Permissions', uPermDifferencesView1, uPermDifferencesView2, uPermDifferencesView3, uPermDifferencesView4);

    // column panels to hold object perms for each comparison categories
    var objPermUniques = makeColumnPanel('Object Permissions', objPermView1, objPermView2, objPermView3, objPermView4);
    var objPermSims = makeColumnPanel('Object Permissions', objPermCommonView1, objPermCommonView2, objPermCommonView3, objPermCommonView4);
    var objPermDiffs = makeColumnPanel('Object Permissions', objPermDifferencesView1, objPermDifferencesView2, objPermDifferencesView3, objPermDifferencesView4);
    
    // panels specific to unique, common, differing
    var permsetUniques = makeComparisonPanel('Unique Perms', [userPermUniques, objPermUniques]);
    var permsetSimilarities = makeComparisonPanel('Common Perms', [userPermSims, objPermSims]);
    var permsetDifferences = makeComparisonPanel('Differing Perms', [userPermDiffs, objPermDiffs]);
    
    // main compare panel for body of page
    var comparePanel = makeComparisonPanelWithId('', [permsetSimilarities, permsetUniques, permsetDifferences], 'comparePanelId');

    // -- Create body panel then whole page with header, navigation, and body panels --
    var mainPanel = Ext.create('Ext.Panel', {
    	region: 'center',
    	margins: '0 5 5 5',
    	layout: {
    		type: 'vbox',
    		align: 'stretch'
    	},
    	items: [ dragDropHeader, comparePanel ]
    });

    // clear assignments button
    var clearAssignments = Ext.create('Ext.button.Button', {
  	   text: 'Clear Assignments',
 	   tooltip: 'Clear selections',
 	   width: 150,
 	   height: 24,
 	   handler: clearAssignmentsFunction
	});
    
    var logoutButton = Ext.create('Ext.button.Button', {
 	   text: 'Logout',
	   tooltip: 'Logout',
	   width: 100,
	   height: 24,
	   handler: function() {
		   window.location.href='/logout';
	   }
    });
    
    var header = Ext.create('Ext.Panel', {
    	cls: 'app-header',
    	layout: 'column',
        region: 'north',
        height: 30,
        margins: '5 5 0 5',
        
        items: [{
            html: '<h1>PermComparator</h1>',
            columnWidth: 0.6,
        	cls: 'app-header-column'
        },
        {
        	items: [clearAssignments],
        	columnWidth: 0.2,
        	style: 'text-align: right;',
        	cls: 'app-header-column'
        },
        {
        	items: [logoutButton],
        	columnWidth: 0.2,
        	style: 'text-align: right;',
        	cls: 'app-header-column'
        }
        ]
    });
    
    // -- Main Viewport Display for whole Page --
    Ext.create('Ext.Viewport', {
        layout: 'border',
        items: [
                header,
                selectionMenu, 
                mainPanel
        ] 
    });
});


// ** Utility functions **
//
// make store used to head menu items (users, permsets, ect.) 
function makeStore(itemType, menuItemReader, menuItem) {
	return new Ext.data.Store({
	    proxy: new Ext.data.HttpProxy({
	        api: {
	            read :    itemType+'/get'
	        },
	        reader: menuItemReader
	    }),
	    model:  menuItem,
		autoSave: false
	});
}

// make store used for storing permissions for each item/permset
// use 'memory' store since loading from 1 json through main store
function makePermStore(nameOfJsonRoot) {
	return new Ext.create('Ext.data.Store', {
    	proxy: {
    		type: 'memory',
    		reader: {
    			type: 'json',
    			root: nameOfJsonRoot
    		}
    	},
    	fields: [{name: 'name'}]
    });
}

//make store used for storing object permissions for each item/permset
function makeTreeStore(nameOfJsonRoot, storeId) {
	return new Ext.create('Ext.data.TreeStore', {
		storeId: storeId,
		proxy: {
			type: 'memory',
			reader: {
				type: 'json',
				root: nameOfJsonRoot
			}
		},
		fields: [{name: 'success'}, {name: 'text'}, {name: 'children'}, {name: 'leaf'}, {name: 'expanded'}, {name: 'loaded'}],
	    folderSort: true,
	    sorters: [{
	        property: 'name',
	        direction: 'ASC'
	    }]
	});
}

//make tree panel used to display object perms for each permset
function makeTreeView(permStore) {
    return new Ext.create('Ext.tree.Panel', {
        store: permStore,
        cls: 'tree-view',
        trackMouseOver: false,
        animate: false,
        enableDD: false,
        useArrows: false,
        rootVisible: false,
        border: false,
        lines: false,
        dockedItems: [{
            xtype: 'toolbar',
            scope: this,
            items: [{
                text: 'Expand All',
                handler: function(button){
                	var toolbar = button.up('toolbar'), treepanel = toolbar.up('treepanel');
                    treepanel.expandAll();
                }
            }, {
                text: 'Collapse All',
                handler: function(button){
                	var toolbar = button.up('toolbar'), treepanel = toolbar.up('treepanel');
                    treepanel.collapseAll();
                }
            }]
        }]
    });
}

// view used for displaying Name and Id for Users, Permsets, and Profiles in left-hand Menu
// - makes the items dragable with initializeDragZone call
// EXTJS4 Drag Drop example was VERY helpful
// -- http://dev.sencha.com/deploy/ext-4.0.0/examples/dd/dragdropzones.html
function makeMenuView(itemStore) {
	return Ext.create('Ext.view.View', {
		cls: 'item-view',
		tpl: '<tpl for=".">' +
            	'<div class="item-source"><table><tbody>' +
                	'<tr><td class="item-label">Name</td><td class="item-name">{Name}</td></tr>' +
                	/*'<tr><td class="item-label">Id</td><td class="item-name">{Id}</td></tr>' +*/
                '</tbody></table></div>' +
            '</tpl>',
         itemSelector: 'div.item-source',
         overItemCls: 'item-over',
         selectedItemClass: 'item-selected',
         singleSelect: true,
         store: itemStore,
         listeners: {
        	 render: initializeItemDragZone
         }
	});
}
	
// make view for displaying perms - param: store to use
function makePermView(permStore) {
	return new Ext.create('Ext.grid.Panel', {
    	store: permStore,
    	hideHeaders: true,
    	padding: '0 0 0 0',
    	border: false,
    	disableSelection: true,
    	columns: [{
    		dataIndex: 'name',
    		flex: 1
    	}]
    });
}

// make sub-panels used to fill main comparison panel - columns to compare attributes
// ie: userPermission panel for differences
// also used for dragDropHeader
function makeColumnPanel(permSectionName, col1Items, col2Items, col3Items, col4Items) {
    return Ext.create('Ext.Panel', {
    	title: permSectionName,
    	cls: 'sub-menu',
    	layout: 'hbox',
    	margins: '0 0 0 0',
    	autoScroll: true,
    	items: [{
            flex: 1,
            baseCls:'x-plain',
            items: col1Items
        },{
            flex: 1,
            baseCls:'x-plain',
            items: col2Items
        },{
            flex: 1,
            baseCls:'x-plain',
            items: col3Items
        },{
            flex: 1,
            baseCls:'x-plain',
            items: col4Items
        }]
    });
}

// make main comparison panel (differences and similarities)
function makeComparisonPanel(panelName, panelItems) {
	return Ext.create('Ext.Panel', {
		title: panelName,
    	cls: 'menu-accordion',
		layout: 'accordion',
		layoutConfig: {animate: true},
		flex: 1,
		items: panelItems,
	});
}

function makeComparisonPanelWithId(panelName, panelItems, panelId) {
	return Ext.create('Ext.Panel', {
		title: panelName,
		id: panelId,
    	cls: 'menu-accordion',
		layout: 'accordion',
		layoutConfig: {animate: true},
		flex: 1,
		items: panelItems,
	});
}

// drop columns used for dropping items for comparisons
function makeDropColumn(columnInfo) {
    return Ext.create('Ext.grid.Panel', {
        region: 'center',
        margins: '0 5 5 5',
        border: false,
        sortableColumns: false,
        hideHeaders: true,
    	disableSelection: true,
    	overCls: 'none',
        store: {
        	fields: [{name: 'cName'}],
           	data: columnInfo
        },
        columns: [{
            dataIndex: 'cName',
            align: 'center',
            flex: 1
        }],
        features: [{
            ftype:'rowbody',
            rowBodyDivCls: 'destination-target',
            getAdditionalData: function() {
                return Ext.apply(Ext.grid.feature.RowBody.prototype.getAdditionalData.apply(this, arguments), {
                    rowBody: 'Drop Here',
                });
            }
        }],
        viewConfig: {
        	listeners: {render: initializeDestinationDropZone}
        }
    });
}

// Functions used to clear drop zones 
function clearAssignmentsFunction() {
    var blank = 'blank';

	if (idItem1.indexOf(blank) == -1) {
		clearRowBody(idItem1);
	}
	if (idItem2.indexOf(blank) == -1) {
		clearRowBody(idItem2);
	}
	if (idItem3.indexOf(blank) == -1) {
		clearRowBody(idItem3);
	}
	if (idItem4.indexOf(blank) == -1) {
		clearRowBody(idItem4);
	}
    // reset ids for new API calls
	id1 = 'blank';
	id2 = 'blank';
	id3 = 'blank';
	id4 = 'blank';
	
    comparePanel = Ext.getCmp('comparePanelId'); // get the actual panel object
    comparePanel.setLoading(true, true);
	userPermStoreId.getProxy().url = 'permsetDiffs/' + id1 + '/' + id2 + '/' + id3 + '/' + id4 + '/getUserPerms';
	userPermStoreId.load();
	objectPermStoreId.getProxy().url = 'permsetDiffs/' + id1 + '/' + id2 + '/' + id3 + '/' + id4 + '/getObjPerms';
	objectPermStoreId.load();
}

function clearRowBody(target) {
	var text = 'Drop Here';
	var rowBody = Ext.fly(target).findParent('.x-grid-rowbody-tr', null, false),
		targetEl = Ext.get(target);
	
    Ext.fly(target).removeCls('destination-target-dropped');
    targetEl.update(text);
}

// -----------------------------------------------------------------------
// ExtJS 4 Drag 'n Drop Example
//-- http://dev.sencha.com/deploy/ext-4.0.0/examples/dd/dragdropzones.html
// -----------------------------------------------------------------------
/*
 * Here is where we "activate" the DataView.
 * We have decided that each node with the class "patient-source" encapsulates a single draggable
 * object.
 *
 * So we inject code into the DragZone which, when passed a mousedown event, interrogates
 * the event to see if it was within an element with the class "patient-source". If so, we
 * return non-null drag data.
 *
 * Returning non-null drag data indicates that the mousedown event has begun a dragging process.
 * The data must contain a property called "ddel" which is a DOM element which provides an image
 * of the data being dragged. The actual node clicked on is not dragged, a proxy element is dragged.
 * We can insert any other data into the data object, and this will be used by a cooperating DropZone
 * to perform the drop operation.
 */
function initializeItemDragZone(v) {
    v.dragZone = Ext.create('Ext.dd.DragZone', v.getEl(), {

//      On receipt of a mousedown event, see if it is within a draggable element.
//      Return a drag data object if so. The data object can contain arbitrary application
//      data, but it should also contain a DOM element in the ddel property to provide
//      a proxy to drag.
        getDragData: function(e) {
            var sourceEl = e.getTarget(v.itemSelector, 10), d;
            if (sourceEl) {
                d = sourceEl.cloneNode(true);
                d.id = Ext.id();
                return v.dragData = {
                    sourceEl: sourceEl,
                    repairXY: Ext.fly(sourceEl).getXY(),
                    ddel: d,
                    itemData: v.getRecord(sourceEl).data
                };
            }
        },

//      Provide coordinates for the proxy to slide back to on failed drag.
//      This is the original XY coordinates of the draggable element.
        getRepairXY: function() {
            return this.dragData.repairXY;
        }
    });
}

/*
 * Here is where we "activate" the GridPanel.
 * We have decided that the element with class "hospital-target" is the element which can receieve
 * drop gestures. So we inject a method "getTargetFromEvent" into the DropZone. This is constantly called
 * while the mouse is moving over the DropZone, and it returns the target DOM element if it detects that
 * the mouse if over an element which can receieve drop gestures.
 *
 * Once the DropZone has been informed by getTargetFromEvent that it is over a target, it will then
 * call several "onNodeXXXX" methods at various points. These include:
 *
 * onNodeEnter
 * onNodeOut
 * onNodeOver
 * onNodeDrop
 *
 * We provide implementations of each of these to provide behaviour for these events.
 */
function initializeDestinationDropZone(v) {
    var gridView = v,
        grid = gridView.up('gridpanel');

    grid.dropZone = Ext.create('Ext.dd.DropZone', v.el, {

//      If the mouse is over a target node, return that node. This is
//      provided as the "target" parameter in all "onNodeXXXX" node event handling functions
        getTargetFromEvent: function(e) {
            return e.getTarget('.destination-target');
        },

//      On entry into a target node, highlight that node.
        onNodeEnter : function(target, dd, e, data){
            Ext.fly(target).addCls('destination-target-hover');
        },

//      On exit from a target node, unhighlight that node.
        onNodeOut : function(target, dd, e, data){
            Ext.fly(target).removeCls('destination-target-hover');
        },

//      While over a target node, return the default drop allowed class which
//      places a "tick" icon into the drag proxy.
        onNodeOver : function(target, dd, e, data){
            return Ext.dd.DropZone.prototype.dropAllowed;
        },

//      On node drop, we can interrogate the target node to find the underlying
//      application object that is the real target of the dragged data.
//      In this case, it is a Record in the GridPanel's Store.
//      We can use the data set up by the DragZone's getDragData method to read
//      any data we decided to attach.
        onNodeDrop : function(target, dd, e, data){
        	var rowBody = Ext.fly(target).findParent('.x-grid-rowbody-tr', null, false),
                mainRow = rowBody.previousSibling,
                h = gridView.getRecord(mainRow),
                targetEl = Ext.get(target);
            targetEl.update(data.itemData.Name);
            Ext.fly(target).addCls('destination-target-dropped');

            // Personal customization mixed in, but main part came from example code on ExtJS website
            var item = h.data.cName;
            switch(item)
            {
            case 'Item 1':
            	id1 = data.itemData.Id;
            	idItem1 = target.id;
            	break;
            case 'Item 2':
        		id2 = data.itemData.Id;
        		idItem2 = target.id;
            	break;
            case 'Item 3':
        		id3 = data.itemData.Id;
        		idItem3 = target.id;
            	break;
            case 'Item 4':
        		id4 = data.itemData.Id;
        		idItem4 = target.id;
            	break;
            }

            comparePanel = Ext.getCmp('comparePanelId'); 	// get the actual panel object
            comparePanel.setLoading(true, true);			// set loading mask while we load data
            
            userPermStoreId.getProxy().url = 'permsetDiffs/' + id1 + '/' + id2 + '/' + id3 + '/' + id4 + '/getUserPerms';
            userPermStoreId.load();

            objectPermStoreId.getProxy().url = 'permsetDiffs/' + id1 + '/' + id2 + '/' + id3 + '/' + id4 + '/getObjPerms';
            objectPermStoreId.load();
            
            return true;
        }
    });
}