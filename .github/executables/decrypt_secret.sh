#!/bin/sh

# Get arguments
while getopts p:i:o: flag
do
    case "${flag}" in
        p) passphrase=${OPTARG};;
        i) input=${OPTARG};;
        o) output=${OPTARG};;
    esac
done

# --batch to prevent interactive command
# --yes to assume "yes" for questions
gpg --quiet --batch --yes --decrypt --passphrase="$passphrase" --output "$output" "$input"