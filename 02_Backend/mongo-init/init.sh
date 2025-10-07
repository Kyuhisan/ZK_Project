#!/bin/bash

until mongo --host mongo --eval "db.adminCommand('ping')" &>/dev/null; do
  echo "Waiting for MongoDB..."
  sleep 2
done

echo "Importing to Listings-Filtered..."
mongoimport \
  --host mongo \
  --db Listings \
  --collection "Listings-Filtered" \
  --file /mongo-init/InitialData.json \
  --jsonArray

echo "Import complete."
