#!/bin/sh

# Decrypt the file
mkdir $HOME/secrets
# --batch to prevent interactive command
# --yes to assume "yes" for questions
gpg --quiet --batch --yes --decrypt --passphrase="$LARGE_SECRET_PASSPHRASE" \
--output $HOME/secrets/braintree_demo_app-eb501d54ba5f.json \
./.github/files/braintree-demo-app-eb501d54ba5f.json.gpg
