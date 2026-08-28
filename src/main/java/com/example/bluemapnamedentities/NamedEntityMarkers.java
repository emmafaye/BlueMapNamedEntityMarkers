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

public class NamedEntityMarkers extends JavaPlugin {

    private static final String MARKER_SET_ID = "bm-named-entities";
    private static final String MARKER_SET_LABEL = "Pets & Named Entities";

    @Override
    public void onEnable() {
        // Register hook into BlueMap API
        BlueMapAPI.onEnable(api -> {
            // Run entity update task every 5 seconds (100 ticks)
            Bukkit.getScheduler().runTaskTimer(this, () -> updateEntityMarkers(api), 20L, 100L);
        });
    }

    private void updateEntityMarkers(BlueMapAPI api) {
        for (World world : Bukkit.getWorlds()) {
            api.getWorld(world).ifPresent(blueWorld -> {
                // Loop through all maps registered to this world
                blueWorld.getMaps().forEach(map -> {
                    // Get or create the toggleable MarkerSet on the map
                    MarkerSet markerSet = map.getMarkerSets().computeIfAbsent(MARKER_SET_ID, id -> 
                        MarkerSet.builder()
                            .label(MARKER_SET_LABEL)
                            .toggleable(true)
                            .defaultHidden(false)
                            .build()
                    );

                    // Clear previous iteration markers
                    markerSet.getMarkers().clear();

                    // Scan entities currently loaded in this world
                    for (Entity entity : world.getEntities()) {
                        if (isNamedOrPet(entity) && entity instanceof LivingEntity) {
                            String name = entity.getCustomName() != null 
                                    ? entity.getCustomName() 
                                    : entity.getType().name();

                            String markerId = "entity-" + entity.getUniqueId();
                            String headTexture = getHeadTextureKey(entity);

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

    private boolean isNamedOrPet(Entity entity) {
        if (entity.getCustomName() != null && !entity.getCustomName().isEmpty()) {
            return true;
        }
        return entity instanceof Tameable && ((Tameable) entity).isTamed();
    }

    private String getHeadTextureKey(Entity entity) {
        if (entity instanceof Tameable) {
            Tameable pet = (Tameable) entity;
            if (pet.isTamed() && pet.getOwner() != null && pet.getOwner().getUniqueId() != null) {
                return pet.getOwner().getUniqueId().toString();
            }
        }
        
        switch (entity.getType()) {
            case WOLF: return "MHF_Wolf";
            case OCELOT:
            case CAT: return "MHF_Ocelot";
            case PIG: return "MHF_Pig";
            case COW: return "MHF_Cow";
            case SHEEP: return "MHF_Sheep";
            case VILLAGER: return "MHF_Villager";
            default: return "MHF_Question";
        }
    }

    private String escapeHtml(String input) {
        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
