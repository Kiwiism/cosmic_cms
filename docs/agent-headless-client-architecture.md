# Agent Headless Client Architecture

## Decision

The broken in-server agent runtime has been removed from Cosmic. Future agents should run as an external headless client process that connects to Cosmic through the normal login and channel protocol.

This keeps the game server maintainable:

- Cosmic receives normal client packets.
- Movement, mob control, NPC dialogs, shop actions, item use, and chat flow through existing packet handlers.
- Other players see agents through the same spawn and movement broadcasts used by real clients.
- Agent CMS owns configuration, cards, schedules, and observability, but does not inject fake characters into maps.

## Components

### Cosmic Server

Cosmic should stay unaware of agent internals. Allowed integration points are narrow operations endpoints such as:

- server health
- cache reloads
- optional diagnostics

No agent tick loop, fake `Client`, fake channel session, or direct map movement simulation should live inside Cosmic.

### Agent CMS

Agent CMS owns persistent agent data in `cosmic_agent_cms`:

- agent profiles
- card loadouts
- task queues and schedules
- personality and behavior cards
- runtime audit/history
- operator controls

Agent CMS should call an external runtime service for deploy, undeploy, pause, and status. The current Server CMS bridge should not be used for agent actions.

### Headless Client Runtime

The runtime should be a separate process or service with these systems:

- **Protocol System**: login, world select, channel select, character select, encryption/session handling, opcode encode/decode.
- **Physics System**: v83-like foothold gravity, jump, fall, ladder/rope, walk speed, stance, and movement-fragment generation.
- **Navigation System**: WZ foothold graph for local movement plus portal graph for map travel.
- **Action System**: packet-level actions for movement, NPC click/dialog, quest choice, item use, equip, shop buy/sell/recharge, chat, party, trade, and combat.
- **Card Execution System**: converts task/behavior/personality cards into prioritized runtime intents.
- **Observation System**: reads server packets to maintain map objects, mobs, drops, NPCs, chat, HP/MP, inventory, quest state, and current position.
- **Safety System**: stuck detection, route retry, fallback task, resource sustain policy, and operator alerts.

## Reference Notes

SoloMapling proves that bots can look alive, but it does so by adding a server-side artificial player layer. That approach works for demos but risks long-term server coupling.

nutnnut's movement tooling is more useful for this direction: packet logging plus physics calibration can help make the headless runtime produce client-like movement packets.

## Migration Rule

Reusable code/data from the previous agent work should move into Agent CMS or the external runtime only when it does not depend on `server.agent` or fake server-side clients.

Server code should not regain:

- `server.agent.*`
- headless `Client` constructors
- `/internal/admin/agents/*` bridge endpoints
- agent runtime scheduler modules
- agent-specific map movement helpers

