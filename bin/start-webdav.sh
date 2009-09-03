#!/usr/bin/env bash

# Start hadoop webdav daemon. Run this on master node.

usage="Usage: start-webdav.sh"

bin=`dirname "$0"`
bin=`cd "$bin"; pwd`

. "$bin"/hadoop-config.sh

webdav_conf="hadoop-webdav.sh"

if [ -f "${HADOOP_CONF_DIR}/${webdav_conf}" ]; then
    . "${HADOOP_CONF_DIR}/${webdav_conf}"
else
    echo "Couldn't find ${webdav_conf} in config dir (${HADOOP_CONF_DIR})."
    exit 52
fi

optAddr=""
if [ "${HADOOP_WEBDAV_HOST}" != "" ]; then
    optAddr="-l ${HADOOP_WEBDAV_HOST}"
fi

optPort=""
if [ "${HADOOP_WEBDAV_PORT}" != "" ]; then
    optPort="-p ${HADOOP_WEBDAV_PORT}"
fi

optHDFS=""
if [ "${HADOOP_WEBDAV_HDFS}" != "" ]; then
    optHDFS="-n ${HADOOP_WEBDAV_HDFS}"
fi

# add libs to CLASSPATH
classpath=""
for f in $HADOOP_WEBDAV_CLASSPATH/*.jar; do
    classpath=${classpath}:$f;
done

export HADOOP_CLASSPATH="${HADOOP_CLASSPATH}:${classpath}"

# start webdav daemon
"$bin"/hadoop-daemon.sh --config $HADOOP_CONF_DIR start $WEBDAV_JAVA_CLASS $optAddr $optPort $optHDFS
