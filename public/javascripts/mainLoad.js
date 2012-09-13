Ext.require(['*']);

Ext.onReady(function() {
	
	itemCompareCount = 0;
	
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

    var permsetMenuStore = makeStore("Permsets", menuItemReader, menuItem);
    permsetMenuStore.load();

    var profilePermsetMenuStore = makeStore("ProfilePermsets", profileItemReader, profileItem);
    profilePermsetMenuStore.load();
    
    var userView = Ext.create('Ext.view.View', {
        cls: 'patient-view',
        tpl: '<tpl for=".">' +
                '<div class="patient-source"><table><tbody>' +
                    '<tr><td class="patient-label">Name</td><td class="patient-name">{Name}</td></tr>' +
                    '<tr><td class="patient-label">UserId</td><td class="patient-name">{Id}</td></tr>' +
                '</tbody></table></div>' +
             '</tpl>',
        itemSelector: 'div.patient-source',
        overItemCls: 'patient-over',
        selectedItemClass: 'patient-selected',
        singleSelect: true,
        store: userMenuStore,
        listeners: {
            render: initializePatientDragZone
        }
    });
    
    var permsetView = Ext.create('Ext.view.View', {
        cls: 'patient-view',
        tpl: '<tpl for=".">' +
                '<div class="patient-source"><table><tbody>' +
                    '<tr><td class="patient-label">Name</td><td class="patient-name">{Name}</td></tr>' +
                    '<tr><td class="patient-label">Permset Id</td><td class="patient-name">{Id}</td></tr>' +
                '</tbody></table></div>' +
             '</tpl>',
        itemSelector: 'div.patient-source',
        overItemCls: 'patient-over',
        selectedItemClass: 'patient-selected',
        singleSelect: true,
        store: permsetMenuStore,
        listeners: {
            render: initializePatientDragZone
        }
    });
    
    var profilePermsetView = Ext.create('Ext.view.View', {
        cls: 'patient-view',
        tpl: '<tpl for=".">' +
                '<div class="patient-source"><table><tbody>' +
                    '<tr><td class="patient-label">Profile Name</td><td class="patient-name">{Name}</td></tr>' +
                    '<tr><td class="patient-label">Id</td><td class="patient-name">{Id}</td></tr>' +
                '</tbody></table></div>' +
             '</tpl>',
        itemSelector: 'div.patient-source',
        overItemCls: 'patient-over',
        selectedItemClass: 'patient-selected',
        singleSelect: true,
        store: profilePermsetMenuStore,
        listeners: {
            render: initializePatientDragZone
        }
    });
    
    var helpWindow = Ext.create('Ext.Window', {
        title: 'Source code',
        width: 920,
        height: 500,
        closeAction: 'hide',
        renderTpl: [
            '<textarea readonly class="{baseCls}-body<tpl if="bodyCls"> {bodyCls}</tpl><tpl if="frame"> {baseCls}-body-framed</tpl><tpl if="ui"> {baseCls}-body-{ui}</tpl>"<tpl if="bodyStyle"> style="{bodyStyle}"</tpl>></div>'
        ],
        listeners: {
            render: function(w) {
                Ext.Ajax.request({
                    url: 'dragdropzones.js',
                    success: function(r) {
                        w.body.dom.value = r.responseText;
                    }
                });
            }
        }
    });

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
    	region: 'west',
        width: 300,
        margins: '0 0 5 5',
        layout: 'accordion',
        layoutConfig: {animate: true},
        items: [userMenu, permsetMenu, profileMenu]
    });

    // -- Make drag / drop columns and header --
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

    // -- Views to fill compare panesl --
    id1 = 'blank';
    id2 = 'blank';
    id3 = 'blank';
    id4 = 'blank';
    
    var uPermStore1 = makePermStore('permset1_Unique');
    var uPermStore2 = makePermStore('permset2_Unique');
    var uPermStore3 = makePermStore('permset3_Unique');
    var uPermStore4 = makePermStore('permset4_Unique');
    
    var uPermCommonStore1 = makePermStore('permset1_Common');
    var uPermCommonStore2 = makePermStore('permset2_Common');
    var uPermCommonStore3 = makePermStore('permset3_Common');
    var uPermCommonStore4 = makePermStore('permset4_Common');
    
    // main store that will load data to specific user perm stores from 1 json
    userPermStore = new Ext.data.Store({
    	storeId: 'userPermStoreId',
    	proxy: {
    		type: 'ajax',
    		url : 'permsetDiffs/' + id1 + '/' + id2 + '/' + id3 + '/' + id4,
	        reader: {		
	    		type: 'json',
	        	fields : [ {name: 'name', type: 'string'} ]
	        }
    	},
    	fields:  [{name: 'name'} /*, {name: 'enabled'}*/],
    	listeners: {
    		load: function(store, records, successful) {
    			uPermStore1.loadRawData(store.proxy.reader.jsonData);
    			uPermStore2.loadRawData(store.proxy.reader.jsonData);
    			uPermStore3.loadRawData(store.proxy.reader.jsonData);
    			uPermStore4.loadRawData(store.proxy.reader.jsonData);
    			
    			uPermCommonStore1.loadRawData(store.proxy.reader.jsonData);
    			uPermCommonStore2.loadRawData(store.proxy.reader.jsonData);
    			uPermCommonStore3.loadRawData(store.proxy.reader.jsonData);
    			uPermCommonStore4.loadRawData(store.proxy.reader.jsonData);
    		}
    	}
    });
    // load store Id so URL can be changed when items dropped for comparison
    userPermStoreId = Ext.StoreMgr.lookup("userPermStoreId");

    // views for displaying unique user perms
    var uPermView1 = makePermView(uPermStore1);
    var uPermView2 = makePermView(uPermStore2);
    var uPermView3 = makePermView(uPermStore3);
    var uPermView4 = makePermView(uPermStore4);
 
    var uPermCommonView1 = makePermView(uPermCommonStore1);
    var uPermCommonView2 = makePermView(uPermCommonStore2);
    var uPermCommonView3 = makePermView(uPermCommonStore3);
    var uPermCommonView4 = makePermView(uPermCommonStore4);
    
    var blankView = Ext.create('Ext.view.View', {
    	html: 'Perms shown here'
    })
    // -- Make compare panels --
    var userPermDiffs = makeColumnPanel('-- User Permissions', uPermView1, uPermView2, uPermView3, uPermView4);
    var userPermSims = makeColumnPanel('-- User Permissions', uPermCommonView1, uPermCommonView2, uPermCommonView3, uPermCommonView4);
    var objPermDiffs = makeColumnPanel('-- Object Permissions', blankView, blankView, blankView, blankView);
    var objPermSims = makeColumnPanel('-- Object Permissions', blankView, blankView, blankView, blankView);
    
    var permsetDifferences = makeComparisonPanel('Differences', [userPermDiffs]);
    var permsetSimilarities = makeComparisonPanel('Similarities', [userPermSims]);
//    var permsetDifferences = makeComparisonPanel('Differences', [userPermDiffs, objPermDiffs]);
//    var permsetSimilarities = makeComparisonPanel('Similarities', [userPermSims, objPermSims]);
    var comparePanel = makeComparisonPanel('', [permsetDifferences, permsetSimilarities]);

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
    
    Ext.create('Ext.Viewport', {
        layout: 'border',
        items: [{
            cls: 'app-header',
            region: 'north',
            height: 30,
            html: '<h1>PermComparator</h1>',
            margins: '5 5 5 5'
        }, selectionMenu, 
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

// make view for displaying perms - param: store to use
function makePermView(permStore) {
	return new Ext.create('Ext.grid.Panel', {
    	store: permStore,
    	hideHeaders: true,
    	padding: '0 0 0 0',
    	border: false,
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
        sortableColumns: false,
        hideHeaders: true,
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
            rowBodyDivCls: 'hospital-target',
            getAdditionalData: function() {
                return Ext.apply(Ext.grid.feature.RowBody.prototype.getAdditionalData.apply(this, arguments), {
                    rowBody: 'Drop Here'
                });
            }
        }],
        viewConfig: {
        	listeners: {render: initializeHospitalDropZone}
        }
    });
}

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
function initializePatientDragZone(v) {
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
                    patientData: v.getRecord(sourceEl).data
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
function initializeHospitalDropZone(v) {
    var gridView = v,
        grid = gridView.up('gridpanel');

    grid.dropZone = Ext.create('Ext.dd.DropZone', v.el, {

//      If the mouse is over a target node, return that node. This is
//      provided as the "target" parameter in all "onNodeXXXX" node event handling functions
        getTargetFromEvent: function(e) {
            return e.getTarget('.hospital-target');
        },

//      On entry into a target node, highlight that node.
        onNodeEnter : function(target, dd, e, data){
            Ext.fly(target).addCls('hospital-target-hover');
        },

//      On exit from a target node, unhighlight that node.
        onNodeOut : function(target, dd, e, data){
            Ext.fly(target).removeCls('hospital-target-hover');
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

            targetEl.update(data.patientData.Name);
            Ext.fly(target).addCls('hospital-target-dropped');
            itemCompareCount++;
            
            //if (mainId == 'blank')
            //Ext.Msg.alert(h.data.cName);
            var item = h.data.cName;
            switch(item)
            {
            case 'Item 1':
            	id1 = data.patientData.Id;
            	break;
            case 'Item 2':
        		id2 = data.patientData.Id;
            	break;
            case 'Item 3':
        		id3 = data.patientData.Id;
            	break;
            case 'Item 4':
        		id4 = data.patientData.Id;
            	break;
            }
            
            userPermStoreId.getProxy().url = 'permsetDiffs/' + id1 + '/' + id2 + '/' + id3 + '/' + id4;
            userPermStoreId.load();

            return true;
        }
    });
}

