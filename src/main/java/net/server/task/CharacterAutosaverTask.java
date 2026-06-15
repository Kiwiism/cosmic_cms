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
package net.server.task;

import client.Character;
import config.YamlConfig;
import net.server.PlayerStorage;
import net.server.world.World;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Ronan
 */
public class CharacterAutosaverTask extends BaseTask implements Runnable {  // thanks Alex09 (Alex-0000) for noticing these runnable classes are tasks, "workers" runs them
    private int cursor;
    private final Map<Integer, Long> lastQueuedAt = new HashMap<>();

    @Override
    public void run() {
        if (!YamlConfig.config.server.USE_AUTOSAVE) {
            return;
        }

        PlayerStorage ps = wserv.getPlayerStorage();
        List<Character> characters = new ArrayList<>(ps.getAllCharacters());
        characters.sort(Comparator.comparingInt(Character::getId));
        if (characters.isEmpty()) {
            cursor = 0;
            lastQueuedAt.clear();
            return;
        }

        Set<Integer> onlineIds = characters.stream().map(Character::getId).collect(Collectors.toSet());
        lastQueuedAt.keySet().retainAll(onlineIds);

        int batchSize = Math.min(Math.max(1, YamlConfig.config.server.AUTOSAVE_BATCH_SIZE), characters.size());
        long now = System.currentTimeMillis();
        long characterInterval = Math.max(
                YamlConfig.config.server.AUTOSAVE_BATCH_INTERVAL_MS,
                YamlConfig.config.server.AUTOSAVE_CHARACTER_INTERVAL_MS);
        int submitted = 0;
        int scanned = 0;
        while (submitted < batchSize && scanned < characters.size()) {
            Character chr = characters.get(cursor);
            cursor = (cursor + 1) % characters.size();
            scanned++;

            long lastQueued = lastQueuedAt.getOrDefault(chr.getId(), 0L);
            if (chr.isLoggedin() && now - lastQueued >= characterInterval) {
                chr.queueAutosave();
                lastQueuedAt.put(chr.getId(), now);
                submitted++;
            }
        }
    }

    public CharacterAutosaverTask(World world) {
        super(world);
    }
}
