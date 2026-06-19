package com.cosmic.agentcms;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class CharacterCosmeticCatalog {
    private final Path makeCharInfoPath;
    private final Path equipStringPath;
    private final Path wzPath;
    private volatile Map<String, GenderCatalog> cache;
    private volatile Map<Integer, String> equipNamesCache;

    public CharacterCosmeticCatalog(@Value("${cosmic.wz-path:../../wz}") String wzPath) {
        this.wzPath = Path.of(wzPath).toAbsolutePath().normalize();
        this.makeCharInfoPath = this.wzPath.resolve("Etc.wz/MakeCharInfo.img.xml");
        this.equipStringPath = this.wzPath.resolve("String.wz/Eqp.img.xml");
    }

    public List<CosmeticOption> search(String kind, int gender, String query, int limit) {
        GenderCatalog catalog = catalogFor(gender);
        List<CosmeticOption> source = switch (normalizedKind(kind)) {
            case "hair" -> catalog.hairs();
            case "face" -> catalog.faces();
            case "skin" -> catalog.skins();
            case "top" -> catalog.tops();
            case "bottom" -> catalog.bottoms();
            case "shoes" -> catalog.shoes();
            case "weapon" -> catalog.weapons();
            default -> throw new IllegalArgumentException("Unknown cosmetic kind " + kind);
        };
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return source.stream()
                .filter(option -> q.isBlank()
                        || String.valueOf(option.id()).contains(q)
                        || option.name().toLowerCase(Locale.ROOT).contains(q))
                .limit(Math.max(1, Math.min(limit, 100)))
                .toList();
    }

    public CharacterCosmetics defaultsFor(int gender) {
        GenderCatalog catalog = catalogFor(gender);
        return new CharacterCosmetics(
                firstId(catalog.faces()),
                firstId(catalog.hairs()),
                firstId(catalog.skins()),
                catalog.starterTop(),
                catalog.starterBottom(),
                catalog.starterShoes(),
                catalog.starterWeapon()
        );
    }

    public void validateSelection(int gender, int face, int hair, int skin, int top, int bottom, int shoes, int weapon) {
        GenderCatalog catalog = catalogFor(gender);
        if (!contains(catalog.faces(), face)) {
            throw new IllegalArgumentException("Face " + face + " is not available for this gender in MakeCharInfo");
        }
        if (!contains(catalog.hairs(), hair)) {
            throw new IllegalArgumentException("Hair " + hair + " is not available for this gender in MakeCharInfo");
        }
        if (!contains(catalog.skins(), skin)) {
            throw new IllegalArgumentException("Skin " + skin + " is not available for this gender in MakeCharInfo");
        }
        if (!contains(catalog.tops(), top)) {
            throw new IllegalArgumentException("Top " + top + " is not available for this gender in MakeCharInfo");
        }
        if (!contains(catalog.bottoms(), bottom)) {
            throw new IllegalArgumentException("Bottom " + bottom + " is not available for this gender in MakeCharInfo");
        }
        if (!contains(catalog.shoes(), shoes)) {
            throw new IllegalArgumentException("Shoes " + shoes + " is not available for this gender in MakeCharInfo");
        }
        if (!contains(catalog.weapons(), weapon)) {
            throw new IllegalArgumentException("Weapon " + weapon + " is not available for this gender in MakeCharInfo");
        }
    }

    public List<GenderSummary> genders() {
        return List.of(
                new GenderSummary(0, "Male", defaultsFor(0)),
                new GenderSummary(1, "Female", defaultsFor(1))
        );
    }

    public StarterEquipStats starterEquipStats(int itemId) {
        Path path = starterEquipPath(itemId);
        if (path == null || !path.toFile().isFile()) {
            return StarterEquipStats.empty();
        }
        try {
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(path.toFile());
            Element info = child(document.getDocumentElement(), "info");
            return new StarterEquipStats(
                    intValue(info, "tuc"),
                    intValue(info, "incSTR"),
                    intValue(info, "incDEX"),
                    intValue(info, "incINT"),
                    intValue(info, "incLUK"),
                    intValue(info, "incMHP"),
                    intValue(info, "incMMP"),
                    intValue(info, "incPAD"),
                    intValue(info, "incMAD"),
                    intValue(info, "incPDD"),
                    intValue(info, "incMDD"),
                    intValue(info, "incACC"),
                    intValue(info, "incEVA"),
                    intValue(info, "incSpeed"),
                    intValue(info, "incJump")
            );
        } catch (Exception ignored) {
            return StarterEquipStats.empty();
        }
    }

    private GenderCatalog catalogFor(int gender) {
        Map<String, GenderCatalog> catalogs = getOrLoad();
        return gender == 1 ? catalogs.get("CharFemale") : catalogs.get("CharMale");
    }

    private Map<String, GenderCatalog> getOrLoad() {
        Map<String, GenderCatalog> loaded = cache;
        if (loaded == null) {
            synchronized (this) {
                loaded = cache;
                if (loaded == null) {
                    loaded = loadCatalog();
                    cache = loaded;
                }
            }
        }
        return loaded;
    }

    private Map<String, GenderCatalog> loadCatalog() {
        try {
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(makeCharInfoPath.toFile());
            Element root = document.getDocumentElement();
            Map<String, GenderCatalog> result = new LinkedHashMap<>();
            for (String key : List.of("CharMale", "CharFemale")) {
                Element info = child(child(root, "Info"), key);
                Element names = child(child(root, "Name"), key);
                result.put(key, new GenderCatalog(
                        readFaceOptions(info, "0"),
                        readHairOptions(info, names),
                        readSkinOptions(info, names),
                        readEquipOptions(info, "4", "Top"),
                        readEquipOptions(info, "5", "Bottom"),
                        readEquipOptions(info, "6", "Shoes"),
                        readEquipOptions(info, "7", "Weapon")
                ));
            }
            return result;
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to read " + makeCharInfoPath, exception);
        }
    }

    private List<CosmeticOption> readFaceOptions(Element info, String nodeName) {
        return readInts(child(info, nodeName)).stream()
                .map(id -> new CosmeticOption(id, "Face " + id, "MakeCharInfo.img.xml"))
                .toList();
    }

    private List<CosmeticOption> readHairOptions(Element info, Element names) {
        Map<Integer, String> baseNames = readNames(child(names, "1"));
        List<Integer> bases = readInts(child(info, "1"));
        List<Integer> colors = readInts(child(info, "2"));
        Map<Integer, String> colorNames = readNames(child(names, "2"));
        Set<Integer> seen = new LinkedHashSet<>();
        List<CosmeticOption> options = new ArrayList<>();
        for (int base : bases) {
            int normalized = base - base % 10;
            for (int color : colors) {
                int id = normalized + color;
                if (seen.add(id)) {
                    String baseName = baseNames.getOrDefault(base, "Hair " + normalized);
                    String colorName = colorNames.getOrDefault(color, "Color " + color);
                    options.add(new CosmeticOption(id, colorName + " " + baseName, "MakeCharInfo.img.xml"));
                }
            }
        }
        options.sort(Comparator.comparing(CosmeticOption::name).thenComparing(CosmeticOption::id));
        return options;
    }

    private List<CosmeticOption> readSkinOptions(Element info, Element names) {
        Map<Integer, String> skinNames = readNames(child(names, "3"));
        return readInts(child(info, "3")).stream()
                .map(id -> new CosmeticOption(id, skinNames.getOrDefault(id, "Skin " + id), "MakeCharInfo.img.xml"))
                .toList();
    }

    private List<CosmeticOption> readEquipOptions(Element info, String nodeName, String fallbackType) {
        Map<Integer, String> names = equipNames();
        return readInts(child(info, nodeName)).stream()
                .map(id -> new CosmeticOption(id, names.getOrDefault(id, fallbackType + " " + id), "MakeCharInfo.img.xml"))
                .toList();
    }

    private Map<Integer, String> equipNames() {
        Map<Integer, String> loaded = equipNamesCache;
        if (loaded == null) {
            synchronized (this) {
                loaded = equipNamesCache;
                if (loaded == null) {
                    loaded = loadEquipNames();
                    equipNamesCache = loaded;
                }
            }
        }
        return loaded;
    }

    private Map<Integer, String> loadEquipNames() {
        Map<Integer, String> names = new LinkedHashMap<>();
        try {
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(equipStringPath.toFile());
            collectEquipNames(document.getDocumentElement(), names);
        } catch (Exception ignored) {
            // MakeCharInfo still provides valid IDs; readable names are best-effort.
        }
        return names;
    }

    private void collectEquipNames(Element parent, Map<Integer, String> names) {
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node node = children.item(index);
            if (!(node instanceof Element element) || !"imgdir".equals(element.getTagName())) {
                continue;
            }
            String nodeName = element.getAttribute("name");
            if (nodeName.matches("\\d+")) {
                Element name = child(element, "name");
                if (name != null && "string".equals(name.getTagName())) {
                    names.put(Integer.parseInt(nodeName), name.getAttribute("value"));
                }
            }
            collectEquipNames(element, names);
        }
    }

    private List<Integer> readInts(Element parent) {
        List<Integer> values = new ArrayList<>();
        if (parent == null) {
            return values;
        }
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node node = children.item(index);
            if (node instanceof Element element && "int".equals(element.getTagName())) {
                values.add(Integer.parseInt(element.getAttribute("value")));
            }
        }
        return values;
    }

    private Map<Integer, String> readNames(Element parent) {
        Map<Integer, String> values = new LinkedHashMap<>();
        if (parent == null) {
            return values;
        }
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node node = children.item(index);
            if (node instanceof Element element && "string".equals(element.getTagName())) {
                values.put(Integer.parseInt(element.getAttribute("name")), element.getAttribute("value"));
            }
        }
        return values;
    }

    private int firstId(List<CosmeticOption> options) {
        return options.isEmpty() ? 0 : options.getFirst().id();
    }

    private int intValue(Element parent, String name) {
        Element child = child(parent, name);
        if (child == null || (!"int".equals(child.getTagName()) && !"short".equals(child.getTagName()))) {
            return 0;
        }
        return Integer.parseInt(child.getAttribute("value"));
    }

    private Path starterEquipPath(int itemId) {
        int category = itemId / 10000;
        String folder = switch (category) {
            case 104 -> "Coat";
            case 106 -> "Pants";
            case 107 -> "Shoes";
            case 130, 131, 132 -> "Weapon";
            default -> null;
        };
        if (folder == null) {
            return null;
        }
        return wzPath.resolve("Character.wz").resolve(folder).resolve(String.format("%08d.img.xml", itemId));
    }

    private boolean contains(List<CosmeticOption> options, int id) {
        return options.stream().anyMatch(option -> option.id() == id);
    }

    private String normalizedKind(String kind) {
        return kind == null ? "" : kind.trim().toLowerCase(Locale.ROOT);
    }

    private Element child(Element parent, String name) {
        if (parent == null) {
            return null;
        }
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node node = children.item(index);
            if (node instanceof Element element && name.equals(element.getAttribute("name"))) {
                return element;
            }
        }
        return null;
    }

    public record CosmeticOption(int id, String name, String source) {}

    public record CharacterCosmetics(int face, int hair, int skin, int top, int bottom, int shoes, int weapon) {}

    public record GenderSummary(int id, String label, CharacterCosmetics defaults) {}

    public record StarterEquipStats(int upgradeSlots, int str, int dex, int intStat, int luk, int hp, int mp,
                                    int watk, int matk, int wdef, int mdef, int acc, int avoid, int speed, int jump) {
        static StarterEquipStats empty() {
            return new StarterEquipStats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }
    }

    private record GenderCatalog(List<CosmeticOption> faces, List<CosmeticOption> hairs, List<CosmeticOption> skins,
                                 List<CosmeticOption> tops, List<CosmeticOption> bottoms,
                                 List<CosmeticOption> shoes, List<CosmeticOption> weapons) {
        int starterTop() {
            return first(tops);
        }

        int starterBottom() {
            return first(bottoms);
        }

        int starterShoes() {
            return first(shoes);
        }

        int starterWeapon() {
            return first(weapons);
        }

        private static int first(List<CosmeticOption> options) {
            return options.isEmpty() ? 0 : options.getFirst().id();
        }
    }
}
