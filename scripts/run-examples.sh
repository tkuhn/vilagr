#!/bin/bash

echo "Running example 1..."
scripts/Vilagr.sh src/main/resources/demo/graph1.properties

echo

echo "Running example 2..."
scripts/Vilagr.sh src/main/resources/demo/graph2.properties

echo

echo "Re-running example 2 with different colors..."
scripts/Vilagr.sh src/main/resources/demo/graph2b.properties
