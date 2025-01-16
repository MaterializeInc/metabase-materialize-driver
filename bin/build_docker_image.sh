#!/usr/bin/env bash

set -euo pipefail

# Global variables
METABASE_VERSION=${1:-}
MATERIALIZE_JAR_PATH=${2:-}
DOCKER_IMAGE_TAG=${3:-}
BUILD_DIR=".build"

cleanup() {
    echo "Cleaning up..."
    rm -f "${BUILD_DIR}/materialize-driver.jar"
}

trap cleanup EXIT

usage() {
    echo
    echo "Usage: $0 METABASE_VERSION PATH_TO_MATERIALIZE_JAR DOCKER_IMAGE_TAG"
    echo
    echo "This script builds and tags a Metabase Docker image with Materialize driver built-in."
    echo
    echo "Example:"
    echo
    echo "$0 v0.52.6 /some/path/to/materialize.metabase-driver.jar my-metabase-with-materialize:v0.0.1"
    exit 1
}

# Validate input arguments
if [ -z "$METABASE_VERSION" ] || [ -z "$MATERIALIZE_JAR_PATH" ] || [ -z "$DOCKER_IMAGE_TAG" ]; then
    usage
fi

# Validate the JAR file's existence
if [ ! -f "$MATERIALIZE_JAR_PATH" ]; then
    echo "Error: JAR file '$MATERIALIZE_JAR_PATH' not found!"
    exit 2
fi

# Create build directory
echo "Preparing build environment..."
mkdir -p "$BUILD_DIR"

# Copy JAR to build directory
cp "$MATERIALIZE_JAR_PATH" "${BUILD_DIR}/materialize-driver.jar"

# Build the Docker image
echo "Building Docker image with Metabase version '$METABASE_VERSION' and Materialize driver..."
docker build --build-arg METABASE_VERSION="$METABASE_VERSION" --tag "$DOCKER_IMAGE_TAG" .

# Completion message
echo "Build complete. Image tagged as '$DOCKER_IMAGE_TAG'."
echo "To run the image, use 'docker run -p 3000:3000 $DOCKER_IMAGE_TAG'"
