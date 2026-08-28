package com.example.bluemapnamedentities;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.markers.HtmlMarker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class NamedEntityMarkers extends JavaPlugin {

    private static final String MARKER_SET_ID = "bm-named-entities";
    private static final String MARKER_SET_LABEL = "Named Entities";

    @Override
    public void onEnable() {
        BlueMapAPI.onEnable(api -> {
            Bukkit.getScheduler().runTaskTimer(this, () -> updateEntityMarkers(api), 20L, 100L);
        });
    }

    private void updateEntityMarkers(BlueMapAPI api) {
        for (World world : Bukkit.getWorlds()) {
            api.getWorld(world).ifPresent(blueWorld -> {
                blueWorld.getMaps().forEach(map -> {
                    MarkerSet markerSet = map.getMarkerSets().computeIfAbsent(MARKER_SET_ID, id -> 
                        MarkerSet.builder()
                            .label(MARKER_SET_LABEL)
                            .toggleable(true)
                            .defaultHidden(false)
                            .build()
                    );

                    markerSet.getMarkers().clear();

                    for (Entity entity : world.getEntities()) {
                        if (entity instanceof LivingEntity && entity.getCustomName() != null && !entity.getCustomName().isEmpty()) {
                            String name = entity.getCustomName();
                            String markerId = "entity-" + entity.getUniqueId();
                            
                            String fontColor = getOwnerColor(entity);
                            String headTexture = getMobHeadKey(entity);

                            String htmlSnippet = String.format(
                                "<div class=\"bm-player-marker\" style=\"display: flex; flex-direction: column; align-items: center; pointer-events: auto;\">" +
                                "  <div class=\"bm-player-label\" style=\"background: rgba(0, 0, 0, 0.85); color: %s; padding: 2px 6px; border-radius: 4px; font-size: 11px; font-weight: bold; font-family: sans-serif; margin-bottom: 2px; border: 1px solid rgba(255, 255, 255, 0.15); white-space: nowrap;\">%s</div>" +
                                "  <img class=\"bm-player-head\" src=\"https://mc-heads.net/avatar/%s/32\" style=\"width: 24px; height: 24px; border-radius: 3px; border: 1px solid rgba(0, 0, 0, 0.5);\" />" +
                                "</div>",
                                fontColor,
                                escapeHtml(name),
                                headTexture
                            );

                            HtmlMarker marker = HtmlMarker.builder()
                                .label(name)
                                .position(entity.getLocation().getX(), entity.getLocation().getY() + 0.5, entity.getLocation().getZ())
                                .html(htmlSnippet)
                                .styleClasses("bm-player-marker-container")
                                .build();

                            markerSet.getMarkers().put(markerId, marker);
                        }
                    }
                });
            });
        }
    }

    private String getOwnerColor(Entity entity) {
        if (entity instanceof Tameable pet && pet.isTamed() && pet.getOwner() != null) {
            String ownerName = pet.getOwner().getName();
            if (ownerName != null && !ownerName.isEmpty()) {
                return generateColorFromSeed(ownerName);
            }
        }
        return "#FFFFFF";
    }

    private String generateColorFromSeed(String seed) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(seed.getBytes(StandardCharsets.UTF_8));
            int hue = Math.abs((hash[0] << 8 | (hash[1] & 0xFF))) % 360;
            return String.format("hsl(%d, 85%%, 65%%)", hue);
        } catch (Exception e) {
            return "#FFD700";
        }
    }

    private String getMobHeadKey(Entity entity) {
        // 1. Dynamic Reflection check for Wolf Variants (1.20.5+)
        if (entity instanceof Wolf wolf) {
            String variantName = getWolfVariantKey(wolf);
            if (variantName != null) {
                return switch (variantName) {
                    case "ashen" -> "MHF_Wolf_Ashen";
                    case "black" -> "MHF_Wolf_Black";
                    case "chestnut" -> "MHF_Wolf_Chestnut";
                    case "rusty" -> "MHF_Wolf_Rusty";
                    case "snowy" -> "MHF_Wolf_Snowy";
                    case "spotted" -> "MHF_Wolf_Spotted";
                    case "striped" -> "MHF_Wolf_Striped";
                    case "woods" -> "MHF_Wolf_Woods";
                    default -> "MHF_Wolf";
                };
            }
            return "MHF_Wolf";
        }

        // 2. Cat Type check
        if (entity instanceof Cat cat) {
            try {
                String catType = cat.getCatType().getKey().getKey().toLowerCase();
                return switch (catType) {
                    case "black" -> "MHF_Cat_Black";
                    case "siamese" -> "MHF_Cat_Siamese";
                    case "tabby" -> "MHF_Cat_Tabby";
                    case "calico" -> "MHF_Cat_Calico";
                    case "persian" -> "MHF_Cat_Persian";
                    case "ragdoll" -> "MHF_Cat_Ragdoll";
                    default -> "MHF_Ocelot";
                };
            } catch (Exception ignored) {
                return "MHF_Ocelot";
            }
        }

        // 3. Fallback for basic entity types
        String typeName = entity.getType().name().toUpperCase();
        return switch (typeName) {
            case "PIG" -> "MHF_Pig";
            case "COW" -> "MHF_Cow";
            case "SHEEP" -> "MHF_Sheep";
            case "VILLAGER" -> "MHF_Villager";
            case "CHICKEN" -> "MHF_Chicken";
            case "SKELETON" -> "MHF_Skeleton";
            case "ZOMBIE" -> "MHF_Zombie";
            case "SPIDER" -> "MHF_Spider";
            case "ENDERMAN" -> "MHF_Enderman";
            case "GOAT" -> "MHF_Goat";
            default -> "MHF_Question";
        };
    }

    private String getWolfVariantKey(Wolf wolf) {
        try {
            Method getVariantMethod = wolf.getClass().getMethod("getVariant");
            Object variantObj = getVariantMethod.invoke(wolf);
            if (variantObj != null) {
                Method getKeyMethod = variantObj.getClass().getMethod("getKey");
                Object namespacedKey = getKeyMethod.invoke(variantObj);
                Method getKeyStringMethod = namespacedKey.getClass().getMethod("getKey");
                return ((String) getKeyStringMethod.invoke(namespacedKey)).toLowerCase();
            }
        } catch (Exception ignored) {
            // Method doesn't exist in the current API version target
        }
        return null;
    }

    private String escapeHtml(String input) {
        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
