package com.example.bluemapnamedentities;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.markers.HtmlMarker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.java.JavaPlugin;

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
                        // Strict check: ONLY entities explicitly named with a Name Tag
                        if (entity instanceof LivingEntity && entity.getCustomName() != null && !entity.getCustomName().isEmpty()) {
                            String name = entity.getCustomName();
                            String markerId = "entity-" + entity.getUniqueId();
                            
                            // Always use mob texture instead of player owner skin
                            String headTexture = getMobHeadKey(entity);

                            String htmlSnippet = String.format(
                                "<div style=\"display:flex; flex-direction:column; align-items:center; transform:translate(-50%%, -100%%); pointer-events:auto;\">" +
                                "  <div style=\"background:rgba(0,0,0,0.75); color:#fff; padding:2px 6px; border-radius:4px; font-size:11px; font-family:sans-serif; margin-bottom:2px;\">%s</div>" +
                                "  <img src=\"https://mc-heads.net/avatar/%s/24\" style=\"width:24px; height:24px; border-radius:3px;\" />" +
                                "</div>",
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

    private String getMobHeadKey(Entity entity) {
        switch (entity.getType()) {
            case WOLF: return "MHF_Wolf";
            case CAT:
            case OCELOT: return "MHF_Ocelot";
            case PIG: return "MHF_Pig";
            case COW: return "MHF_Cow";
            case SHEEP: return "MHF_Sheep";
            case VILLAGER: return "MHF_Villager";
            case CHICKEN: return "MHF_Chicken";
            case SKELETON: return "MHF_Skeleton";
            case ZOMBIE: return "MHF_Zombie";
            case SPIDER: return "MHF_Spider";
            case ENDERMAN: return "MHF_Enderman";
            case ALEX: return "Alex";
            default: return "MHF_Question"; // Fallback icon for rare/unmapped mob heads
        }
    }

    private String escapeHtml(String input) {
        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
