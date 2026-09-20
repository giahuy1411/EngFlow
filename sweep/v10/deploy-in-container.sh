#!/bin/sh
# audit-v10 — deploy schema study (Phase 1.3)
# Chay trong container sqlserver. Backup da verify: engflow_2026-09-19-audit-v10.bak
set -e
cd /opt/mssql-tools18/bin
./sqlcmd -S localhost -U sa -P "$SA_PASSWORD" -C -d english_learning -b \
  -v EffectiveFrom="2026-09-20" \
  BackupFile="/var/opt/mssql/backup/engflow_2026-09-19-audit-v10.bak" \
  -i /tmp/deploy.sql
