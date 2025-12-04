#!/bin/sh
set -e

echo "[n8n_init] Init starting..."

export N8N_USER_FOLDER="/home/node/.n8n"
export HOME="/home/node"

WF_DIR="$N8N_USER_FOLDER/workflows"
WF_FILE="$WF_DIR/n8n_ci_workflows.json"
CRED_FILE="$WF_DIR/mock_ci_credentials.json"

mkdir -p "$N8N_USER_FOLDER"
mkdir -p "$WF_DIR"

echo "[n8n_init] Using N8N_USER_FOLDER=$N8N_USER_FOLDER"
echo "[n8n_init] Using HOME=$HOME"

# Import workflows/credentials
echo "[n8n_init] Importing workflows from: $WF_FILE"
n8n import:workflow --input="$WF_FILE"

echo "[n8n_init] Importing credentials from: $CRED_FILE"
n8n import:credentials --input="$CRED_FILE"

echo "[n8n_init] Backfilling workflow history entries..."
node <<'NODE'
const sqlite3 = require('/usr/local/lib/node_modules/n8n/node_modules/.pnpm/sqlite3@5.1.7/node_modules/sqlite3');
const path = `${process.env.N8N_USER_FOLDER}/.n8n/database.sqlite`;

const db = new sqlite3.Database(path);
const all = (sql, params = []) => new Promise((resolve, reject) => db.all(sql, params, (err, rows) => err ? reject(err) : resolve(rows)));
const run = (sql, params = []) => new Promise((resolve, reject) => db.run(sql, params, function (err) { err ? reject(err) : resolve(this); }));

(async () => {
  const workflows = await all('SELECT id, versionId, nodes, connections, name FROM workflow_entity');
  const existing = new Set((await all('SELECT versionId FROM workflow_history')).map((row) => row.versionId));

  for (const wf of workflows) {
    if (!existing.has(wf.versionId)) {
      await run(
        'INSERT INTO workflow_history(versionId, workflowId, authors, nodes, connections, name) VALUES (?, ?, ?, ?, ?, ?)',
        [wf.versionId, wf.id, 'ci', wf.nodes, wf.connections, wf.name],
      );
    }
  }

  await run('UPDATE workflow_entity SET activeVersionId = versionId WHERE activeVersionId IS NULL;');
  db.close();
})().catch((error) => {
  console.error('[n8n_init] Failed to backfill workflow history:', error);
  process.exit(1);
});
NODE

# Activate all workflows
echo "[n8n_init] Activating all workflows..."
n8n update:workflow --all --active=true

# Log what is actually in the DB
echo "[n8n_init] Listing workflows after activation:"
n8n list:workflow || true

echo "[n8n_init] Starting n8n..."
exec n8n start
