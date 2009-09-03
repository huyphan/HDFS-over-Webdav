#!/usr/bin/env bash

# Stop hadoop webdav daemon.  Run this on master node.

bin=`dirname "$0"`
bin=`cd "$bin"; pwd`

. "$bin"/hadoop-config.sh

webdav_conf="hadoop-webdav.sh"

if [ -f "${HADOOP_CONF_DIR}/${webdav_conf}" ]; then
    . "${HADOOP_CONF_DIR}/${webdav_conf}"
else
    echo "Couldn't find ${webdav_conf} in config dir (${HADOOP_CONF_DIR})."
    exit 53
fi

# stop webdav daemon
"$bin"/hadoop-daemon.sh --config $HADOOP_CONF_DIR stop $WEBDAV_JAVA_CLASS
