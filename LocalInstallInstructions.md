##   
Perm-Comparator Install

Instructions to install and run Perm-Comparator locally. Ideal when your Salesforce org enforces IP restrictions — the @heroku servers are outside your IP ranges, thus you won't be able to connect using [https://perm-comparator.herokuapp.com](https://perm-comparator.herokuapp.com/).

**Install instructions: **  
Note: these instructions are for a Mac and commands to be run in terminal are prefixed with “command”

1. Get source code 
    1. command: git clone [https://github.com/john-brock/perm-comparator.git](https://github.com/john-brock/perm-comparator.git)

2. Check version of Play! that is installed — Play! is a java framework, read more here: [http://www.playframework.com/documentation/1.3.4/home](http://www.playframework.com/documentation/1.2.6/home) 
    1. command: which play
    2. If Play! isn't installed, download Play 1.3.4 jar.
        1. [http://downloads.typesafe.com/play/1.3.4/play-1.3.4.zip](http://downloads.typesafe.com/play/1.3.4/play-1.3.4.zip)
        2. extract to folder of your choice
        3. create an alias for Play! in /usr/local/bin
            1. command: ln -s /Users/jbrock/Downloads/play-1.3.4/play /usr/local/bin

        4. check: which play and play version
            1. play version should start Play! and version 1.3.4 should be displayed

3. Check app dependencies from directory where you cloned perm-comparator
    1. command: play dependencies
        1. you may need to run command: play dependencies —sync (read the output, Play! will tell you)

4. Set environment variables
    1. perm-comparator requires two environment variables, clientKey and clientSecret which are used to authenticate with the Salesforce connected app allowing for OAuth login / connections
        1. you will need to create a new Connected App in any Salesforce org (DE or Sandbox will work)
            1. Create &gt; Apps
                1. New Connected App
                    1. Fill required fields
                    2. Enable OAuth Settings
                        1. Callback URL: [https://127.0.0.1:9000/forcedotcomoauth2/callback](https://127.0.0.1:9000/forcedotcomoauth2/callback)
                        2. Select: Full Access and Perform Requests (refresh token)

            2. When app is created, keep the Consumer Key and the Consumer Secret 

        2. edit ~/.bash_profile   
export clientKey= Consumer Key from Connected App in previous step  
export clientSecret=Consumer Secret from Connected App in previous step
            1. NOTE: I use a different connected app (different keys) in production on Heroku

        3. command: source ~/.bash_profile
        4. close and restart terminal
            1. ensure configs are set correctly
                1. command: echo $clientKey
                2. command: echo $clientSecret

    2. uncomment X509 certificate settings (key and file) in conf/application.conf
        1. you may have to revisit this step if you have an issue connecting to [https://127.0.0.1:9000](https://127.0.0.1:9000/) once perm-comparator starts
            1. not likely but may need to generate personal client and key — see this website, but also google search for X509 certificates: [http://www.mobilefish.com/services/ssl_certificates/ssl_certificates.php](http://www.mobilefish.com/services/ssl_certificates/ssl_certificates.php)

    3. uncomment https.port = 9000 in conf/application.conf
        1. if you don't do this or don't uncomment the X509 certs, you will get an SSL error when connecting

5. Start Perm-Comparator
    1. from directory where you cloned repository 
        1. command: play run

6. Connect and Login
    1. navigate to: [https://127.0.0.1:9000/](https://127.0.0.1:9000/) (cannot use localhost because of uri redirect mismatch for OAuth)
    2. login using either Production or Sandbox — uses OAuth for authentication with Salesforce -- can log into any other org, not just where connected app was created

7. Compare Users, Profiles, and Permission Sets 
    1. drag an item from the left-side menus to a “Drop Here” box
        1. drop items in multiple drop-boxes for comparisons

    2. comparisons will be performed on User Permissions, Object Permissions, and Setup Entity Access (apps, connected apps, apex classes, and visualforce pages)
