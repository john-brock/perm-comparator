Ext.require(['*']);

Ext.onReady(function() {
	
	var header = Ext.create('Ext.Panel', {
    	cls: 'app-header',
    	frame: true,
        width: 500,
        //height: 30,
        region: 'north',
        style: 'margin:0 auto;margin-top:5px;',
        html: '<h1>Login - PermComparator</h1>',
	});
	
    var loginButton = Ext.create('Ext.button.Button', {
  	   text: 'Login',
  	   cls: 'login-button',
 	   tooltip: 'Login',
 	   height: 40,
 	   handler: function() {
 		   window.location.href='/login';
 	   }
     });
    
	var contentPanel = Ext.create('Ext.Panel', {
		height: 60,
    	layout: 'column',
        border: false,
        cls: 'login-button',
        items: [
        {
        	items: [loginButton],
        	border: false,
        }
        ]
	});
	
	var body = Ext.create('Ext.Panel', {
    	cls: 'app-body',
        width: 500,
        height: 450,
        frame: true,
        border: false,
        style: 'margin:0 auto;margin-top:5px;',
        html: '<h2>Quickly compare Users, Profiles, and Permission Sets<h2>' +
        	  '<h3>1) Login to Force.com using OAuth2<br>2) Drag \'n drop up-to 4 items' +
        	  '<br>3) Find differences and similarities between effective user permissions<h3>' +
        	  '<br><h2>Technology<h2>' +
        	  '<h3> - Frontend: ExtJS4<br> - Backend: Java with Play!<br> - Hosted on Heroku' +
        	  '<br> - Force.com REST API<h3>' + 
        	  '<br><h2>v0.2 Updates<h2>' +
        	  '<h3>1) Fix for user permissions not being retrieved<br>2) Ability to compare object permissions' +
        	  '<br>3) UI fixes and updates<h3>' +
        	  '<br><h2>Open-Source<h2>' +
        	  '<a href="https://github.com/john-brock/perm-comparator"><h3>https://github/john-brock/perm-comparator</h3></a>',
        dockedItems: [{
        	dock: 'bottom',
        	items: [contentPanel]
        }]
	});
	
	var footer = Ext.create('Ext.Panel', {
        cls: 'app-footer',
        border: false,
		width: 500,
        style: 'margin:0 auto;margin-top:2px;',
        html: '<h2>Questions: @_johnbrock or _johnbrock@outlook.com</h2>'
	});
	
    Ext.create('Ext.Viewport', {
        renderTo: 'body',
        items: [ header, body, footer ] 
    });
});


