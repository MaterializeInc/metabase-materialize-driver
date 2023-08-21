#!/usr/bin/env bash

set -euo pipefail

cleanup() {
    echo "Cleaning up..."
    rm -f .build/materialize-driver.jar
}

trap cleanup EXIT

METABASE_VERSION=${1:-}
MATERIALIZE_JAR_PATH=${2:-}
DOCKER_IMAGE_TAG=${3:-}

if [ -z "$METABASE_VERSION" ] || [ -z "$MATERIALIZE_JAR_PATH" ] || [ -z "$DOCKER_IMAGE_TAG" ]; then
    echo
    echo "Usage: $0 METABASE_VERSION PATH_TO_MATERIALIZE_JAR DOCKER_IMAGE_TAG"
    echo
    echo "This script builds and tags a Metabase Docker image with Materialize driver built-in."
    echo
    echo "Example:"
    echo
    echo "$0 v0.46.7 /some/path/to/materialize.metabase-driver.jar my-metabase-with-materialize:v0.0.1"
    exit 1
fi

if [ ! -f "$MATERIALIZE_JAR_PATH" ]; then
    echo "Error: JAR file '$MATERIALIZE_JAR_PATH' not found!"
    exit 2
fi

mkdir -p .build

cp "$MATERIALIZE_JAR_PATH" .build/materialize-driver.jar

echo "Building Docker image with Metabase version '$METABASE_VERSION' and Materialize driver..."
docker build --build-arg METABASE_VERSION="$METABASE_VERSION" --tag "$DOCKER_IMAGE_TAG" .

echo "Build complete. Image tagged as '$DOCKER_IMAGE_TAG'."
echo "To run the image, use 'docker run -p 3000:3000 $DOCKER_IMAGE_TAG'"
