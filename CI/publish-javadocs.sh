#!/bin/bash

CUR=$(pwd)
TMPDIR="$(dirname -- "${0}")"

SC_RELEASE_TAG="v${RELEASE_VERSION}"

#####################
### publish javadocs
#####################

# Publish versioned javadocs
mkdir -p $CUR/springdoc-openapi/${SC_RELEASE_TAG}
cp -aR $TMPDIR/apidocs $CUR/springdoc-openapi/${SC_RELEASE_TAG}

# Publish to "latest" (always points to the most recent release)
rm -rf $CUR/springdoc-openapi/latest
mkdir -p $CUR/springdoc-openapi/latest
cp -aR $TMPDIR/apidocs $CUR/springdoc-openapi/latest

git add -A
git commit -m "apidocs for release ${SC_RELEASE_TAG}"
git push -u origin gh-pages
