#! /bin/sh

# This script will start Eclipse after appropriately modifying the
# LD_LIBRARY_PATH.

PROJECT_ROOT="`dirname $0`"

export LD_LIBRARY_PATH="$LD_LIBRARY_PATH:$PROJECT_ROOT/lib"
eclipse
