/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

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
package net.server.channel.handlers;

import client.Client;
import config.YamlConfig;
import net.packet.InPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.runtime.RuntimeMetrics;
import tools.PacketCreator;
import tools.exceptions.EmptyMovementException;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

public final class MovePlayerHandler extends AbstractMovementPacketHandler {
    private static final Logger log = LoggerFactory.getLogger(MovePlayerHandler.class);

    @Override
    public final void handlePacket(InPacket p, Client c) {
        long handlerStart = System.nanoTime();
        RuntimeMetrics metrics = RuntimeMetrics.getInstance();
        boolean diagnostics = YamlConfig.config.server.MOVEMENT_DIAGNOSTICS;
        if (diagnostics) {
            long gapMillis = c.markMovementPacketAndGetGapMillis(System.currentTimeMillis());
            metrics.recordMovementGap(gapMillis, YamlConfig.config.server.MOVEMENT_GAP_WARNING_MS);
            if (gapMillis >= YamlConfig.config.server.MOVEMENT_GAP_WARNING_MS) {
                log.warn("Movement packet gap {} ms for {} on map {}", gapMillis,
                        c.getPlayer() == null ? "?" : c.getPlayer().getName(),
                        c.getPlayer() == null ? "?" : c.getPlayer().getMapId());
            }
        }

        p.skip(9);
        try {   // thanks Sa for noticing empty movement sequences crashing players
            int movementDataStart = p.getPosition();
            updatePosition(p, c.getPlayer(), 0);
            long movementDataLength = p.getPosition() - movementDataStart; //how many bytes were read by updatePosition
            p.seek(movementDataStart);

            c.getPlayer().getMap().movePlayer(c.getPlayer(), c.getPlayer().getPosition());
            if (c.getPlayer().isHidden()) {
                c.getPlayer().getMap().broadcastGMMessage(c.getPlayer(), PacketCreator.movePlayer(c.getPlayer().getId(), p, movementDataLength), false);
            } else {
                c.getPlayer().getMap().broadcastMessage(c.getPlayer(), PacketCreator.movePlayer(c.getPlayer().getId(), p, movementDataLength), false);
            }
        } catch (EmptyMovementException e) {
        } finally {
            long elapsedMillis = NANOSECONDS.toMillis(System.nanoTime() - handlerStart);
            metrics.recordMovementPacket(elapsedMillis, YamlConfig.config.server.MOVEMENT_HANDLER_WARNING_MS);
            if (diagnostics && elapsedMillis >= YamlConfig.config.server.MOVEMENT_HANDLER_WARNING_MS) {
                log.warn("Movement handler took {} ms for {} on map {}", elapsedMillis,
                        c.getPlayer() == null ? "?" : c.getPlayer().getName(),
                        c.getPlayer() == null ? "?" : c.getPlayer().getMapId());
            }
        }
    }
}
