package server.agent.actions;

import client.Character;
import client.Character.SkillEntry;
import client.Skill;
import server.StatEffect;
import server.agent.AgentIntentCapability;
import server.agent.AgentIntentType;

import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class AgentSkillActionAdapter implements AgentActionAdapter {
    @Override
    public AgentIntentCapability capability() {
        return AgentIntentCapability.SKILL;
    }

    @Override
    public AgentActionResult execute(AgentActionContext context) {
        if (context.intent().type() != AgentIntentType.SKILL) {
            return AgentActionResult.blockedByRuntime(capability(), context.intent().type() + " reached the skill adapter unexpectedly");
        }

        Character character = context.managed().character();
        if (character == null) {
            return AgentActionResult.blockedByRuntime(capability(), "Cannot inspect skills without an attached character");
        }

        Optional<Map.Entry<Skill, SkillEntry>> selected = selectSkill(character, context.intent().argument());
        if (selected.isEmpty()) {
            return AgentActionResult.blockedByRuntime(
                    capability(),
                    "No matching learned skill is available for " + displayTarget(context.intent().argument()),
                    skillDetailsJson(context, null, null, "NO_SKILL", "No matching learned skill found", false, null)
            );
        }

        Map.Entry<Skill, SkillEntry> entry = selected.get();
        Skill skill = entry.getKey();
        SkillEntry skillEntry = entry.getValue();
        StatEffect effect = effect(skill, skillEntry);
        if (canApplySelfBuff(character, skill, skillEntry, effect)) {
            int beforeHp = character.getHp();
            int beforeMp = character.getMp();
            boolean applied = effect.applyTo(character);
            int afterHp = character.getHp();
            int afterMp = character.getMp();
            String applicationJson = applicationJson(beforeHp, beforeMp, afterHp, afterMp, applied);
            if (applied) {
                return AgentActionResult.ok(
                        capability(),
                        "Applied self-buff skill " + skill.getId() + " at level " + skillEntry.skillevel,
                        true,
                        skillDetailsJson(context, skill, skillEntry, "BUFF_APPLIED", "Safe self-buff applied through StatEffect", true, applicationJson)
                );
            }

            return AgentActionResult.blockedByRuntime(
                    capability(),
                    "Self-buff skill " + skill.getId() + " was rejected by StatEffect",
                    skillDetailsJson(context, skill, skillEntry, "BUFF_REJECTED", "StatEffect.applyTo returned false", true, applicationJson)
            );
        }

        return AgentActionResult.ok(
                capability(),
                "Skill readiness found skill " + entry.getKey().getId() + " at level " + entry.getValue().skillevel,
                false,
                skillDetailsJson(context, skill, skillEntry, "SKILL_READY", readinessNote(character, skill, skillEntry, effect), false, null)
        );
    }

    private Optional<Map.Entry<Skill, SkillEntry>> selectSkill(Character character, String argument) {
        String target = argument == null ? "" : argument.trim();
        return character.getSkills().entrySet().stream()
                .filter(entry -> entry.getValue().skillevel > 0)
                .filter(entry -> matchesSkill(entry.getKey(), entry.getValue(), target))
                .sorted(skillOrdering(target))
                .findFirst();
    }

    private Comparator<Map.Entry<Skill, SkillEntry>> skillOrdering(String target) {
        Comparator<Map.Entry<Skill, SkillEntry>> levelOrder = Comparator
                .comparingInt((Map.Entry<Skill, SkillEntry> entry) -> entry.getValue().skillevel)
                .reversed()
                .thenComparingInt(entry -> entry.getKey().getId());
        if (target == null || target.isBlank()) {
            return levelOrder;
        }
        return Comparator
                .comparingInt((Map.Entry<Skill, SkillEntry> entry) -> String.valueOf(entry.getKey().getId()).equals(target.trim()) ? 0 : 1)
                .thenComparing(levelOrder);
    }

    private boolean matchesSkill(Skill skill, SkillEntry entry, String target) {
        if (target == null || target.isBlank()) {
            return isLikelyActive(skill, entry);
        }
        if (String.valueOf(skill.getId()).equals(target)) {
            return true;
        }

        String normalized = target.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "attack", "active", "damage", "offense" -> isLikelyAttack(skill, entry);
            case "buff", "support" -> isLikelyBuff(skill, entry);
            case "passive" -> !isLikelyActive(skill, entry);
            default -> false;
        };
    }

    private boolean isLikelyActive(Skill skill, SkillEntry entry) {
        StatEffect effect = effect(skill, entry);
        return skill.getAction()
                || effect != null && (effect.getMpCon() > 0
                || effect.getCooldown() > 0
                || effect.getDamage() > 0
                || effect.getMobCount() > 0
                || effect.getStatups() != null && !effect.getStatups().isEmpty());
    }

    private boolean isLikelyAttack(Skill skill, SkillEntry entry) {
        StatEffect effect = effect(skill, entry);
        return skill.getAction() || effect != null && (effect.getDamage() > 0 || effect.getMobCount() > 0);
    }

    private boolean isLikelyBuff(Skill skill, SkillEntry entry) {
        StatEffect effect = effect(skill, entry);
        return effect != null && effect.getStatups() != null && !effect.getStatups().isEmpty();
    }

    private StatEffect effect(Skill skill, SkillEntry entry) {
        try {
            return entry.skillevel <= 0 || entry.skillevel > skill.getMaxLevel() ? null : skill.getEffect(entry.skillevel);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private boolean canApplySelfBuff(Character character, Skill skill, SkillEntry entry, StatEffect effect) {
        return skill != null
                && entry != null
                && effect != null
                && isLikelyBuff(skill, entry)
                && effect.getDamage() <= 0
                && effect.getMobCount() <= 0
                && effect.getHpCon() <= 0
                && effect.getCooldown() <= 0
                && effect.getMpCon() <= character.getMp()
                && !character.skillIsCooling(skill.getId());
    }

    private String readinessNote(Character character, Skill skill, SkillEntry entry, StatEffect effect) {
        if (effect == null) {
            return "Readiness only; no usable effect data was found";
        }
        if (!isLikelyBuff(skill, entry)) {
            return "Readiness only; direct skill casting is limited to safe self-buffs";
        }
        if (effect.getDamage() > 0 || effect.getMobCount() > 0) {
            return "Readiness only; attack skills are handled by the combat adapter";
        }
        if (effect.getHpCon() > 0) {
            return "Readiness only; HP-consuming skills are blocked for agent safety";
        }
        if (effect.getCooldown() > 0 || character.skillIsCooling(skill.getId())) {
            return "Readiness only; cooldown skills are not cast directly by V1 agents";
        }
        if (effect.getMpCon() > character.getMp()) {
            return "Readiness only; agent does not have enough MP to cast safely";
        }
        return "Readiness only; skill is not in the V1 direct-cast allowlist";
    }

    private String skillDetailsJson(
            AgentActionContext context,
            Skill skill,
            SkillEntry entry,
            String state,
            String note,
            boolean mutationEnabled,
            String applicationJson
    ) {
        return "{"
                + "\"skillState\":\"" + escapeJson(state) + "\","
                + "\"intent\":\"" + context.intent().type().name() + "\","
                + "\"argument\":\"" + escapeJson(context.intent().argument()) + "\","
                + "\"world\":" + world(context) + ","
                + "\"channel\":" + channel(context) + ","
                + "\"mapId\":" + mapId(context) + ","
                + "\"selectedSkill\":" + skillJson(skill, entry) + ","
                + "\"mutationEnabled\":" + mutationEnabled + ","
                + "\"application\":" + (applicationJson == null ? "null" : applicationJson) + ","
                + "\"note\":\"" + escapeJson(note) + "\""
                + "}";
    }

    private String skillJson(Skill skill, SkillEntry entry) {
        if (skill == null || entry == null) {
            return "null";
        }
        StatEffect effect = effect(skill, entry);
        return "{"
                + "\"skillId\":" + skill.getId() + ","
                + "\"skillJob\":" + (skill.getId() / 10000) + ","
                + "\"level\":" + entry.skillevel + ","
                + "\"masterLevel\":" + entry.masterlevel + ","
                + "\"expiration\":" + entry.expiration + ","
                + "\"maxLevel\":" + skill.getMaxLevel() + ","
                + "\"action\":" + skill.getAction() + ","
                + "\"beginnerSkill\":" + skill.isBeginnerSkill() + ","
                + "\"fourthJobSkill\":" + skill.isFourthJob() + ","
                + "\"animationTime\":" + skill.getAnimationTime() + ","
                + "\"effect\":" + effectJson(effect)
                + "}";
    }

    private String effectJson(StatEffect effect) {
        if (effect == null) {
            return "null";
        }
        return "{"
                + "\"mpCon\":" + effect.getMpCon() + ","
                + "\"hpCon\":" + effect.getHpCon() + ","
                + "\"duration\":" + effect.getDuration() + ","
                + "\"cooldown\":" + effect.getCooldown() + ","
                + "\"damage\":" + effect.getDamage() + ","
                + "\"attackCount\":" + effect.getAttackCount() + ","
                + "\"mobCount\":" + effect.getMobCount() + ","
                + "\"watk\":" + effect.getWatk() + ","
                + "\"matk\":" + effect.getMatk() + ","
                + "\"statupCount\":" + (effect.getStatups() == null ? 0 : effect.getStatups().size())
                + "}";
    }

    private String applicationJson(int beforeHp, int beforeMp, int afterHp, int afterMp, boolean applied) {
        return "{"
                + "\"applied\":" + applied + ","
                + "\"beforeHp\":" + beforeHp + ","
                + "\"afterHp\":" + afterHp + ","
                + "\"beforeMp\":" + beforeMp + ","
                + "\"afterMp\":" + afterMp + ","
                + "\"hpDelta\":" + (afterHp - beforeHp) + ","
                + "\"mpDelta\":" + (afterMp - beforeMp)
                + "}";
    }

    private int world(AgentActionContext context) {
        return context.perception() == null ? context.managed().client().getWorld() : context.perception().world();
    }

    private int channel(AgentActionContext context) {
        return context.perception() == null ? context.managed().client().getChannel() : context.perception().channel();
    }

    private int mapId(AgentActionContext context) {
        if (context.perception() != null) {
            return context.perception().mapId();
        }
        Character character = context.managed().character();
        return character == null ? -1 : character.getMapId();
    }

    private String displayTarget(String target) {
        return target == null || target.isBlank() ? "default active skill" : target;
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
