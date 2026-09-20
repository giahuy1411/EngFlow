# audit-v9-full clarification ledger

| Decision | Resolution |
|---|---|
| Environment | Local development and verification, not production rollout. |
| Build integrity | Fixes must be proven in regression tests, the packaged JAR, and the running container. |
| Data mutation | Backup before DML; delete only manifest-verified fixtures; preserve legacy and content data. |
| Empty answers | Preserve data; classify rather than synthesize missing answer keys. |
| Missing LISTENING audio | Backfill only after proving the exact TTS/Cloudinary path; otherwise record a per-row reason. |
| Legacy audit data | Keep until an owner reviews an ID manifest. |
| Index and cache decisions | No mutation without representative workload evidence and a measured benefit. |
| Vite stability | A temporary workaround is not a closure claim; record F114 as BLOCKED unless reproduced or excluded by soak evidence. |
