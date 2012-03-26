#! /bin/bash

OWN_DIR="$(readlink -f "$(dirname $0)")"

export LD_LIBRARY_PATH="$OWN_DIR/lib"
eclipse
