#!/bin/sh

# Get arguments
while getopts passphrase:input:output: flag
do
    case "${flag}" in
        passphrase) passphrase=${OPTARG};;
        input) input=${OPTARG};;
        output) output=${OPTARG};;
    esac
done
echo "Passphrase: $passphrase";
echo "Input: $input";
echo "Output: $output";

# Decrypt the file
mkdir $HOME/secrets
# --batch to prevent interactive command
# --yes to assume "yes" for questions
gpg --quiet --batch --yes --decrypt --passphrase="$passphrase" \
--output "$output" "$input" \
&& cat "$output"
