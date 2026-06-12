package client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CharacterSkillPointResetTest {

    @Test
    void calculatesNormalJobSpFromLevelInsteadOfExistingSkillLevels() {
        int[] sp = Character.calculateLegitimateSkillPoints(Job.HERO, 120, 10);

        assertEquals(336, sp[0]);
        assertEquals(336, sum(sp));
    }

    @Test
    void capsNormalJobSpAtTheClassProgressionMaximum() {
        int[] sp = Character.calculateLegitimateSkillPoints(Job.HERO, 255, 10);

        assertEquals(576, sp[0]);
        assertEquals(576, sum(sp));
    }

    @Test
    void rebuildsEvanSpInSeparateGrowthStageBooks() {
        int[] sp = Character.calculateLegitimateSkillPoints(Job.EVAN4, 45, 10);

        assertArrayEquals(new int[]{30, 30, 30, 18, 0, 0, 0, 0, 0, 0}, sp);
    }

    private static int sum(int[] values) {
        int total = 0;
        for (int value : values) {
            total += value;
        }
        return total;
    }
}
