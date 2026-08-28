package com.example.bluemapnamedentities;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.markers.HtmlMarker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Tameable;
import org.bukkit.plugin.java.JavaPlugin;

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

                            // Uses CSS variables for zoom-based scaling and label visibility
                            String htmlSnippet = String.format(
                                "<div style=\"display: flex; flex-direction: column; align-items: center; transform: translate(-50%%, -50%%); pointer-events: auto;\">" +
                                "  <div class=\"bm-marker-label\" style=\"background: rgba(0, 0, 0, 0.85); color: %s; padding: 2px 6px; border-radius: 4px; font-size: 11px; font-weight: bold; font-family: sans-serif; margin-bottom: 3px; border: 1px solid rgba(255, 255, 255, 0.15); white-space: nowrap; display: var(--bm-marker-label-display, block);\">%s</div>" +
                                "  <img src=\"https://mc-heads.net/avatar/%s/32\" style=\"width: var(--bm-marker-size, 32px); height: var(--bm-marker-size, 32px); border-radius: 3px; transition: width 0.2s, height 0.2s;\" />" +
                                "</div>",
                                fontColor,
                                escapeHtml(name),
                                headTexture
                            );

                            HtmlMarker marker = HtmlMarker.builder()
                                .label(name)
                                .position(entity.getLocation().getX(), entity.getLocation().getY() + 0.5, entity.getLocation().getZ())
                                .html(htmlSnippet)
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
        String typeName = entity.getType().name().toUpperCase();

        return switch (typeName) {
            case "WOLF" -> "MHF_Wolf";
            case "CAT", "OCELOT" -> "MHF_Ocelot";
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

    private String escapeHtml(String input) {
        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
