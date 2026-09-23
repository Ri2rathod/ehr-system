# EHR System Monorepo

This repository contains the EHR backend and frontend in one Git repository.

- `backend/` — Java Spring Boot service
- `frontend/` — Next.js application

The original histories were imported with paths rewritten into their respective
directories. The merge commit that created this monorepo preserves both
independent commit graphs, including their authors, dates, and messages.

## Development

Install frontend dependencies once, then start both development servers from the
repository root:

```bash
npm --prefix frontend ci
npm run dev
```

The launcher runs Spring Boot at `http://localhost:8080` and Next.js using its
normal development server. Press `Ctrl+C` to stop both. Run backend commands
from `backend/` and frontend commands from `frontend/` when working on one
component only. The existing component-specific README files and ignore rules
remain in place.
