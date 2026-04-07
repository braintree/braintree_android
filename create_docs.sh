#!/usr/bin/env bash

# Function to display usage
usage() {
    cat << EOF

Usage:
  $0 [OPTIONS] <version>

Arguments:
  version  The version number for the docs directory (required)
EOF
    exit 1
}

version="$1"

if [[ -z "$version" ]]; then
    echo "Error: Version cannot be empty" >&2
    usage
fi

cd docs
cd previousVersions || mkdir previousVersions
mkdir $version
cd ..
rsync -a --exclude 'previousVersions' ./ previousVersions/$version
