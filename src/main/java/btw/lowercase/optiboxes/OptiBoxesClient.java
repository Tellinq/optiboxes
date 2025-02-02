package btw.lowercase.optiboxes;

import btw.lowercase.optiboxes.skybox.SkyboxManager;
import btw.lowercase.optiboxes.utils.api.AbstractSkybox;
import btw.lowercase.optiboxes.config.OptiBoxesConfig;
import btw.lowercase.optiboxes.skybox.OptiFineCustomSkybox;
import btw.lowercase.optiboxes.utils.OptiFineResourceManagerHelper;
import btw.lowercase.optiboxes.utils.CommonUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OptiBoxesClient implements ClientModInitializer {
    public static OptiBoxesClient INSTANCE;
    public static final String MOD_ID = "optiboxes";

    private static final String OPTIFINE_SKY_PARENT = "optifine/sky";
    private static final String SKY_PATTERN_ENDING = "(?<world>\\w+)/(?<name>\\w+).properties$";
    private static final Pattern OPTIFINE_SKY_PATTERN = Pattern.compile("optifine/sky/" + SKY_PATTERN_ENDING);
    private static final String MCPATCHER_SKY_PARENT = "mcpatcher/sky";
    private static final Pattern MCPATCHER_SKY_PATTERN = Pattern.compile("mcpatcher/sky/" + SKY_PATTERN_ENDING);

    private final Logger logger = LoggerFactory.getLogger("OptiBoxes");

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new OptiFineResourceManagerHelper());
        ClientTickEvents.END_WORLD_TICK.register(SkyboxManager.INSTANCE::tick);
    }

    public void inject(OptiFineResourceManagerHelper managerAccessor) {
        this.logger.warn("OptiBoxes is converting MCPatcher/OptiFine custom skies resource packs! Any visual bugs are likely caused by OptiBoxes. Please do not report these issues to Resource Pack creators!");
        SkyboxManager.INSTANCE.clearSkyboxes();
        this.logger.info("Looking for OptiFine/MCPatcher Skies...");
        this.convert(managerAccessor);
    }

    public void convert(OptiFineResourceManagerHelper managerAccessor) {
        if (OptiBoxesConfig.INSTANCE.processOptiFine) {
            this.convertNamespace(managerAccessor, OPTIFINE_SKY_PARENT, OPTIFINE_SKY_PATTERN);
        }

        if (OptiBoxesConfig.INSTANCE.processMCPatcher) {
            this.convertNamespace(managerAccessor, MCPATCHER_SKY_PARENT, MCPATCHER_SKY_PATTERN);
        }
    }

    private void convertNamespace(OptiFineResourceManagerHelper optiFineResourceManagerHelper, String skyParent, Pattern skyPattern) {
        final JsonArray overworldLayers = new JsonArray();
        final JsonArray endLayers = new JsonArray();
        optiFineResourceManagerHelper.searchIn(skyParent)
                .filter(id -> id.getPath().endsWith(".properties"))
                .sorted(Comparator.comparing(ResourceLocation::getPath, (id1, id2) -> {
                    Matcher matcherId1 = skyPattern.matcher(id1);
                    Matcher matcherId2 = skyPattern.matcher(id2);
                    if (matcherId1.find() && matcherId2.find()) {
                        int id1No = CommonUtils.parseInt(matcherId1.group("name").replace("sky", ""), -1);
                        int id2No = CommonUtils.parseInt(matcherId2.group("name").replace("sky", ""), -1);
                        if (id1No >= 0 && id2No >= 0) {
                            return id1No - id2No;
                        }
                    }
                    return 0;
                }))
                .forEach(id -> {
                    Matcher matcher = skyPattern.matcher(id.getPath());
                    if (matcher.find()) {
                        String world = matcher.group("world");
                        String name = matcher.group("name");
                        if (world == null || name == null) {
                            return;
                        }

                        if (name.equals("moon_phases") || name.equals("sun")) {
                            this.logger.info("Skipping {}, moon_phases/sun aren't supported!", id);
                            return;
                        }

                        InputStream inputStream = optiFineResourceManagerHelper.getInputStream(id);
                        if (inputStream == null) {
                            this.logger.error("Error trying to read namespaced identifier: {}", id);
                            return;
                        }

                        Properties properties = new Properties();
                        try {
                            properties.load(inputStream);
                        } catch (IOException e) {
                            this.logger.error("Error trying to read properties from: {}", id);
                            return;
                        } finally {
                            try {
                                inputStream.close();
                            } catch (IOException e) {
                                this.logger.error("Error trying to close input stream at namespaced identifier: {}", id);
                            }
                        }

                        JsonObject json = CommonUtils.convertOptiFineSkyProperties(optiFineResourceManagerHelper, properties, id);
                        if (json != null) {
                            if (world.equals("world0")) {
                                overworldLayers.add(json);
                            } else if (world.equals("world1")) {
                                endLayers.add(json);
                            }
                        }
                    }
                });

        if (!overworldLayers.isEmpty()) {
            JsonObject overworldJson = new JsonObject();
            overworldJson.add("layers", overworldLayers);
            overworldJson.addProperty("world", "minecraft:overworld");
            AbstractSkybox abstractSkybox = OptiFineCustomSkybox.CODEC.decode(JsonOps.INSTANCE, overworldJson).getOrThrow().getFirst();
            SkyboxManager.INSTANCE.addSkybox(ResourceLocation.fromNamespaceAndPath(MOD_ID, "native-optifine-custom-sky-overworld"), abstractSkybox);
        }

        if (!endLayers.isEmpty()) {
            JsonObject endJson = new JsonObject();
            endJson.add("layers", endLayers);
            endJson.addProperty("world", "minecraft:the_end");
            AbstractSkybox abstractSkybox = OptiFineCustomSkybox.CODEC.decode(JsonOps.INSTANCE, endJson).getOrThrow().getFirst();
            SkyboxManager.INSTANCE.addSkybox(ResourceLocation.fromNamespaceAndPath(MOD_ID, "native-optifine-custom-sky-end"), abstractSkybox);
        }
    }
}
