# Cosmic Agent CMS

Agent CMS manages server-side agent profiles, goals, policies, scripts, memory, route state, and runtime actions.

It is intentionally separate from Server CMS. Server CMS manages server configuration; Agent CMS manages agent life and behavior.

## Ports

- Web: http://localhost:3002
- API: http://localhost:8084

## Databases

- CMS auth/audit data: `cosmic_agent_cms`
- Agent runtime data: the main `cosmic` database, because the Cosmic server reads agent tables directly.

## Start

```powershell
.\agent-cms\start-agent-cms.ps1
```

Copy `.env.example` to `.env` and fill credentials if your local MySQL password is not blank.
