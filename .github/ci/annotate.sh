#!/usr/bin/env bash
# Usage: annotate.sh <error|notice> <title> <file>
# Publishes the file's content as a workflow annotation, which is readable on the
# run page and through the public checks API (job logs need a login).
level=$1; title=$2; file=$3
msg=$(head -c 60000 "$file" 2>/dev/null)
msg="${msg//'%'/'%25'}"; msg="${msg//$'\r'/'%0D'}"; msg="${msg//$'\n'/'%0A'}"
echo "::${level} title=${title}::${msg}"
