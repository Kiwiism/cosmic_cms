package client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class CharacterHpMpResetTest {

    @Test
    void calculatesBeginnerGrowthFromStartingPools() {
        assertArrayEquals(new int[]{176, 104}, Character.calculateAverageHpMp(Job.BEGINNER, 10));
    }

    @Test
    void includesFirstWarriorAdvancementAtLevelThirty() {
        assertArrayEquals(new int[]{921, 204}, Character.calculateAverageHpMp(Job.WARRIOR, 30));
    }

    @Test
    void usesLevelEightAdvancementForMagicians() {
        assertArrayEquals(new int[]{412, 713}, Character.calculateAverageHpMp(Job.MAGICIAN, 30));
    }

    @Test
    void appliesAranNaturalAndAdvancementGrowth() {
        assertArrayEquals(new int[]{1321, 224}, Character.calculateAverageHpMp(Job.ARAN1, 30));
    }

    @Test
    void followsExistingEvanAdvancementBonuses() {
        assertArrayEquals(new int[]{148, 1632}, Character.calculateAverageHpMp(Job.EVAN4, 45));
    }
}
