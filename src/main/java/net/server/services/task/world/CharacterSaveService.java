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
package net.server.services.task.world;

import net.server.services.BaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.runtime.ServerExecutors;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;

/**
 * @author Ronan
 */
public class CharacterSaveService extends BaseService {
    private static final Logger log = LoggerFactory.getLogger(CharacterSaveService.class);

    private final Set<Integer> queuedCharacters = ConcurrentHashMap.newKeySet();
    private volatile boolean disposed;

    @Override
    public void dispose() {
        disposed = true;
        queuedCharacters.clear();
    }

    public void registerSaveCharacter(int characterId, Runnable runAction) {
        if (disposed || !queuedCharacters.add(characterId)) {
            return;
        }

        try {
            ServerExecutors.getInstance().executePersistence(() -> {
                try {
                    runAction.run();
                } finally {
                    queuedCharacters.remove(characterId);
                }
            });
        } catch (RejectedExecutionException e) {
            queuedCharacters.remove(characterId);
            log.error("Persistence queue is full; could not queue character {}", characterId, e);
        }
    }

}
