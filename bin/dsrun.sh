#!/bin/bash
#
# Calls DSPACE_HOME/bin/dspace dsrun

if [ -z "$DSPACE_HOME" ]; then
	echo "Environment variable DSPACE_HOME not set."
	exit 1
fi

dsrun_dir=`dirname $0`
CLASSPATH=`echo ${dsrun_dir}/*.jar | sed 's/ /\:/g'`
export CLASSPATH

bash $DSPACE_HOME/bin/dspace dsrun "$@"

export CLASSPATH=
