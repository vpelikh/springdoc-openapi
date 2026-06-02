#!/bin/bash
set -e

CUR=$(pwd)
RELEASE_VERSION="${RELEASE_VERSION:?}"

# Configure git
git config user.email "action@github.com"
git config user.name "GitHub Action"

# Update Maven versions (recursive, handles all submodules including BOM)
mvn versions:set -DnewVersion="${RELEASE_VERSION}"
mvn versions:commit

# Generate release notes (draft)
RELEASE_TITLE="springdoc-openapi ${RELEASE_VERSION} released!"
if [ "${IS_PRERELEASE}" = "true" ]; then
  if [[ "${RELEASE_VERSION}" =~ -M ]]; then
    RELEASE_TITLE="springdoc-openapi ${RELEASE_VERSION} (Milestone) released!"
  elif [[ "${RELEASE_VERSION}" =~ -RC ]]; then
    RELEASE_TITLE="springdoc-openapi ${RELEASE_VERSION} (Release Candidate) released!"
  fi
  # For milestones and RCs, base is the last release of any kind
  BASE_TAG=$(python CI/lastRelease.py)
else
  # For final release, base is the last stable release
  BASE_TAG="${LAST_STABLE_RELEASE}"
fi
python CI/releaseNotes.py "$BASE_TAG" "$RELEASE_TITLE" "v${RELEASE_VERSION}" "${IS_PRERELEASE}"

# Stage all changes and commit (detached)
git add -A
git commit -m "Release version ${RELEASE_VERSION}"

echo "RELEASE_COMMIT=$(git rev-parse HEAD)" >> $GITHUB_ENV
