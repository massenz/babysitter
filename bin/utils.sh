#!/bin/bash

# Utility functions to build and run the server.

function git_sha {
    git log --abbrev-commit --pretty=oneline -1 | cut -f 1 -d ' '
}

function get_version {
    echo "0.24.0"
}

