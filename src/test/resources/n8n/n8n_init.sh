#!/bin/bash

set -e

n8n import:workflow --input="/home/node/.n8n/workflows/n8n_ci_workflows.json"
n8n import:credentials --input="/home/node/.n8n/workflows/mock_ci_credentials.json"
n8n update:workflow --all --active=true
n8n start
