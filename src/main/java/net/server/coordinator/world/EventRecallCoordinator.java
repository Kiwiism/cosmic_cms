/*
    This file is part of the HeavenMS MapleStory Server
    Copyleft (L) 2016 - 2019 RonanLana

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package net.server.coordinator.world;

import config.YamlConfig;
import scripting.event.EventInstanceManager;

import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.concurrent.TimeUnit.HOURS;

/**
 * @author Ronan
 */
public class EventRecallCoordinator {

    private final static EventRecallCoordinator instance = new EventRecallCoordinator();

    public static EventRecallCoordinator getInstance() {
        return instance;
    }

    private static final long RECALL_EXPIRY_MILLIS = HOURS.toMillis(2);

    private record RecallEntry(EventInstanceManager event, long expiresAt) {
    }

    private final ConcurrentHashMap<Integer, RecallEntry> eventHistory = new ConcurrentHashMap<>();

    private static boolean isRecallableEvent(EventInstanceManager eim) {
        return eim != null && !eim.isEventDisposed() && !eim.isEventCleared();
    }

    public EventInstanceManager recallEventInstance(int characterId) {
        RecallEntry entry = eventHistory.remove(characterId);
        return entry != null && entry.expiresAt() >= System.currentTimeMillis()
                && isRecallableEvent(entry.event()) ? entry.event() : null;
    }

    public void storeEventInstance(int characterId, EventInstanceManager eim) {
        if (YamlConfig.config.server.USE_ENABLE_RECALL_EVENT && isRecallableEvent(eim)) {
            eventHistory.put(characterId,
                    new RecallEntry(eim, System.currentTimeMillis() + RECALL_EXPIRY_MILLIS));
        }
    }

    public void manageEventInstances() {
        if (!eventHistory.isEmpty()) {
            List<Integer> toRemove = new LinkedList<>();

            long now = System.currentTimeMillis();
            for (Entry<Integer, RecallEntry> eh : eventHistory.entrySet()) {
                RecallEntry entry = eh.getValue();
                if (entry.expiresAt() < now || !isRecallableEvent(entry.event())) {
                    toRemove.add(eh.getKey());
                }
            }

            for (Integer r : toRemove) {
                eventHistory.remove(r);
            }
        }
    }

    public void clear() {
        eventHistory.clear();
    }
}
