# Static Memory and Resource Leak Audit

This audit covers high-confidence retention and resource lifecycle problems found by static inspection. It does not prove that the server is leak-free; heap and load profiling are still required for that.

## Fixed

- Server restarts now reuse one JVM shutdown hook instead of retaining every previous `Server` instance.
- Session, login bypass, event recall, HWID, and login-attempt registries are cleared during in-process restart.
- Login bypass removal now uses the same key type used during insertion.
- Event recall entries expire after two hours instead of retaining event instances indefinitely.
- NPC and quest script managers release disconnected clients through central client cleanup.
- Script manager maps are safe under concurrent channel access.
- Map reloads dispose the removed map, including its recurring monitor tasks.
- Channel map-manager disposal now clears its map cache after taking ownership of the maps to dispose.
- The CMS bridge closes its request executor when stopped.
- EXP logging uses a bounded queue, avoids empty database work, and restores failed batches without unbounded growth.
- Recurring coupon and ranking statements use explicit try-with-resources.

## Residual Runtime Checks

- Run repeated login, channel-change, map-change, event, shop, and disconnect cycles while comparing heap histograms.
- Inspect retained `Character`, `Client`, `MapleMap`, `EventInstanceManager`, script engine, and Netty channel counts after full GC.
- Exercise ignored match confirmations; they currently rely on gameplay dismissal rather than a universal timeout.
- Monitor intentionally long-lived WZ, item, quest, skill, reactor, shop, and monster caches for unexpected cardinality growth.
- Verify EXP log backpressure behavior during extended database outages.
