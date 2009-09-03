# Set Hadoop-Webdav specific environment variables here.

# The WebDAV server address. 0.0.0.0 by default.
# export HADOOP_WEBDAV_HOST=192.168.1.4

# The WebDAV server port. 9800 by default.
# export HADOOP_WEBDAV_PORT=80

# The name of the HDFS, e.g. namenode:port.
# If not specified, Hadoop-WebDAV will try determine name of the FS
# from 'fs.default.name' parameter, specified in hadoop-site.xml of
# your Hadoop installation.
# export HADOOP_WEBDAV_HDFS=hdfs://192.168.1.4:54310/

# Java CLASSPATH. Should point to hdfs_webdav.jar file.  Required.
# export HADOOP_WEBDAV_CLASSPATH=/home/user/hadoop-webdav/lib

# Webdav Java class. Do not modify.
export WEBDAV_JAVA_CLASS=org.apache.hadoop.fs.webdav.WebdavServer
