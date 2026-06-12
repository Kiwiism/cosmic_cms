/*
    This file is part of the HeavenMS MapleStory Server, commands OdinMS-based
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

/*
   @Author: Arthur L - Refactored command content into modules
*/
package client.command.commands.gm3;

import client.BuffStat;
import client.Character;
import client.Client;
import client.command.Command;

public class CheckDmgCommand extends Command {
    {
        setDescription("Show your own stats and base damage.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();
        int maxBase = player.calculateMaxBaseDamage(player.getTotalWatk());
        Integer watkBuff = player.getBuffedValue(BuffStat.WATK);
        Integer matkBuff = player.getBuffedValue(BuffStat.MATK);
        int blessing = player.getSkillLevel(10000000 * player.getJobType() + 12);
        if (watkBuff == null) {
            watkBuff = 0;
        }
        if (matkBuff == null) {
            matkBuff = 0;
        }

        player.dropMessage(5, "Cur Str: " + player.getTotalStr() + " Cur Dex: " + player.getTotalDex() + " Cur Int: " + player.getTotalInt() + " Cur Luk: " + player.getTotalLuk());
        player.dropMessage(5, "Cur WATK: " + player.getTotalWatk() + " Cur MATK: " + player.getTotalMagic());
        player.dropMessage(5, "Cur WATK Buff: " + watkBuff + " Cur MATK Buff: " + matkBuff + " Cur Blessing Level: " + blessing);
        player.dropMessage(5, "Your maximum base damage (before skills) is " + maxBase);
    }
}
