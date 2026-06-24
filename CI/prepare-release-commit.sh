#!/bin/bash
set -e

RELEASE_VERSION="${RELEASE_VERSION:?}"

# Configure git
git config user.email "action@github.com"
git config user.name "GitHub Action"

# Update all Maven POMs in one go
./mvnw versions:set -DnewVersion="${RELEASE_VERSION}" -DgenerateBackupPoms=false

# Stage all changes and commit (detached)
git add -A
git commit -m "Release version ${RELEASE_VERSION}"

echo "RELEASE_COMMIT=$(git rev-parse HEAD)" >> $GITHUB_ENV