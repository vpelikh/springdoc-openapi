#!/bin/bash
set -e

# This script bumps the version on main to the next SNAPSHOT after a final release.
# It assumes we are on a detached HEAD after the release tag.

RELEASE_VERSION="${RELEASE_VERSION:?}"
CURRENT_BRANCH=$(git branch --show-current)
if [ "$CURRENT_BRANCH" != "main" ]; then
  git checkout main
  git pull origin main
fi

# Compute next snapshot version: increment patch, e.g., 3.0.0 -> 3.0.1-SNAPSHOT
if [[ $RELEASE_VERSION =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
  MAJOR=${BASH_REMATCH[1]}
  MINOR=${BASH_REMATCH[2]}
  PATCH=${BASH_REMATCH[3]}
  NEXT_PATCH=$((PATCH + 1))
  NEXT_SNAPSHOT="${MAJOR}.${MINOR}.${NEXT_PATCH}-SNAPSHOT"
else
  echo "ERROR: Release version '$RELEASE_VERSION' does not match semantic versioning (X.Y.Z)"
  exit 1
fi

echo "Bumping version on main to $NEXT_SNAPSHOT"

# Update all POMs in one go
./mvnw versions:set -DnewVersion="${NEXT_SNAPSHOT}" -DgenerateBackupPoms=false

# Commit and push
git config user.email "action@github.com"
git config user.name "GitHub Action"
git add -A
git commit -m "Bump version to ${NEXT_SNAPSHOT}"
git push origin main