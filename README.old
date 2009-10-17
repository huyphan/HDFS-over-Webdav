Notice
=======

This WebDAV server is built on top of Hadoop distribution, so make sure you have
any hadoop-0.16.* unpacked in some location. It uses some libraries from Hadoop's
lib/ dir, stores logs into logs/ dir and almost does not use Hadoop's config
files, i.e. you should not worry about configuring Hadoop if it is not 
configured yet. Current WebDAV server version supports Hadoop-0.16.* as well as
recent Hadoop versions 0.17.*.


History of this WebDAV server
=============================

We used sources at https://issues.apache.org/jira/browse/HADOOP-496 and after
fixing URL-decoding bug in it corrected server code 

 - to enable authorisation dialog on client side and 
 - implemented authentication and HDFS-permissions support on server side.

Also we provide start/stop scripts for this server. 


Compilation.
============

WebDAV uses some libs from Hadoop distribution, so you have to update build.xml
file and specify value of "hadoop.dir" property to point to your unpacked
Hadoop distribution.

Download and put into ./lib dir following jar files WebDAV server depends on:

 - jackrabbit-jcr-commons-1.4-SNAPSHOT.jar
 - jackrabbit-jcr-server-1.4-SNAPSHOT.jar
 - jackrabbit-webdav-1.4-SNAPSHOT.jar
 - commons-collections-3.2.jar
 - slf4j-api-1.3.0.jar
 - slf4j-log4j12-1.3.0.jar
 - xercesImpl-2.8.1.jar
 - jcr-1.0.jar

You can use following links:

http://mirror.olnevhost.net/pub/apache/jackrabbit/binaries/
http://repo1.maven.org/maven2/commons-collections/commons-collections/3.2/commons-collections-3.2.jar
http://repo1.maven.org/maven2/org/slf4j/slf4j-api/1.3.0/slf4j-api-1.3.0.jar
http://repo1.maven.org/maven2/org/slf4j/slf4j-log4j12/1.3.0/slf4j-log4j12-1.3.0.jar
http://repo1.maven.org/maven2/xerces/xercesImpl/2.8.1/xercesImpl-2.8.1.jar
http://repo1.maven.org/maven2/javax/jcr/jcr/1.0/jcr-1.0.jar

Then use 'ant' to compile the sources, find hdfs_webdav.jar in build/ dir.


Installation.
=============

1. Unpack webdav.tar.bz2 distribution to any location
   (for example, /home/user/webdav).

2. Modify config file hadoop-webdav.sh to satisfy your configuration:

   - HADOOP_WEBDAV_HOST, HADOOP_WEBDAV_PORT - address and port WebDAV
   server will listen to.

   - HADOOP_WEBDAV_HDFS - The name of the HDFS, e.g. namenode:port
   in case if you run WebDAV server on nodes that are different from
   the master. If this parameter is not specified, WebDAV will 
   try determine name of the FS from 'fs.default.name' parameter, 
   specified in hadoop-site.xml of your Hadoop installation.

   - HADOOP_WEBDAV_CLASSPATH parameter should point to lib 
   directory from where you unpacked WebDAV distribution.
   # TODO: fix it

3. Copy necessary files to Hadoop distribution.

3.a. Put bin/start-webdav.sh and bin/stop-webdav.sh to Hadoop's bin
     directory (for example, /home/user/hadoop/bin).

3.b. Put files from conf/ dir (hadoop-webdav.sh, accounts.properties,
     jetty.xml, web.xml) to Hadoop's config dir (/home/user/hadoop/conf/).


Authentication and permissions.
===============================

We have added support of authentication into WebDAV. We used Jetty's native
mechanism to authenticate clients with HTTP Basic Authentication. User
credentials are in plain text file and could be either stored at local server
where WebDAV deployed, or could be fetched by WebDAV from a centralized
resource via HTTP protocol. Format of credentials file is simple: one identity
entry per one line. Each line begins with username followed by colon and
password. Password can be stored in clear text, obfuscated or checksummed.
In case of not clear password, checksum should contain prefix of encryption
method followed by checksum itself. You can use class org.mortbay.util.Password
to generate all varieties of passwords. Password entry followed by comma and
list of comma-separated user roles. Also this mechanism could be extended easliy
to use centralized database. More information about authentication support in
Jetty could be found at http://docs.codehaus.org/display/JETTY/JAAS .

Also we implemented support of HDFS-side permissions by WebDAV (see more 
details could be found at 
http://hadoop.apache.org/core/docs/r0.16.4/hdfs_permissions_guide.html). When 
client accesses to WebDAV resource first time, WebDAV server opens an uniq 
session for her, asks for username and password. Once client has authenticated
successfully, WebDAV server gets all clients 'roles' from credentials file.
These roles are treated as Unix groups the client belongs to. When WebDAV server
addresses to any HDFS resource, it uses client's username and client's roles to
build UnixUserGroupInformation object internally with which it will try to
access HDFS resources. Client gets back access attributes with each resource
specifying does she have access to read and/or write the resource. Loosely,
WebDAV transfers clients identity to HDFS and returns back access rights.

Currently HDFS itself has no abilities to check correctness of client's identity
provided to the Hadoop library, so WebDAV cannot guarantee consistency of your
data.


Running.
========

In order to start WebDAV server run from Hadoop's home dir
(for example, /home/user/hadoop):

    # ./bin/start-webdav.sh

To stop the WebDAV server you have to run

    # ./bin/stop-webdav.sh


Client-specific issues.
=======================

  Windows 'native' clients.

Windows 'native' clients are known to have several issues with 
WebDAV servers. 

On Windows you can mount WebDAV resources only in case when WebDAV server 
binds 80 port. So be sure to configure server properly.

Moreover, Windows clients need service WebClient to be running. On Windows 2003
server it's blocked by default, so you have to make it starting automatically --
go to 'Control Panel' > 'Administrative Tools' and run 'Computer Management'
applet, select 'Services and  Applications' > 'Services', in right section find
WebClient, open its properties, change Startup type to Automatic. Make sure to
start it finally.

Another one Windows XP's and later issue is that it's client cannot download
files more than 50Mb from the server, see 
http://support.microsoft.com/kb/900900/en-us for more details. They suggest fix
that actually can be a sort of a hole in security of your Windows workstation.
You can change the limit by editing registry manually.
