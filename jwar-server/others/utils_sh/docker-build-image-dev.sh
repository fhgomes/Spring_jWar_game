#!/bin/bash

# Check if the VERSAO variable is passed
if [ -z "$1" ]; then
  echo "No version provided. Usage: ./build.sh <versao>"
  exit 1
fi

VERSAO=$1

echo "Initial directory: $INITIAL_DIR"
# Set the correct working directory where the Dockerfile and sources are located
BUILD_CONTEXT_DIR="$(dirname "$0")/../../" # Adjust according to your project structure

echo "Build context directory: $BUILD_CONTEXT_DIR"

# Change directory to the build context
cd "$BUILD_CONTEXT_DIR" || { echo "Failed to change directory"; exit 1; }

# Print the current directory after changing to the build context
echo "Current directory after cd: $(pwd)"

ls -l ./jwarsv-sboot/build/libs/

docker build --no-cache --build-arg VERSAO=$VERSAO -t jwar-server:${VERSAO} -t jwar-server:latest-dev .