package btw.lowercase.optiboxes.skybox;

import btw.lowercase.optiboxes.utils.api.AbstractSkybox;
import btw.lowercase.optiboxes.utils.api.AbstractSkyboxManager;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public class SkyboxManager implements AbstractSkyboxManager {
    public static final SkyboxManager INSTANCE = new SkyboxManager();

    private final List<ResourceLocation> preloadedTextures = new ArrayList<>();
    private final Map<ResourceLocation, AbstractSkybox> skyboxMap = new Object2ObjectLinkedOpenHashMap<>();
    private final Map<ResourceLocation, AbstractSkybox> permanentSkyboxMap = new Object2ObjectLinkedOpenHashMap<>();
    private final List<AbstractSkybox> activeAbstractSkyboxes = new LinkedList<>();

    @Override
    public void addSkybox(ResourceLocation resourceLocation, AbstractSkybox abstractSkybox) {
        Preconditions.checkNotNull(resourceLocation, "Identifier was null");
        Preconditions.checkNotNull(abstractSkybox, "Skybox was null");
        this.skyboxMap.put(resourceLocation, abstractSkybox);
    }

    @Override
    public void clearSkyboxes() {
        this.skyboxMap.clear();
        this.activeAbstractSkyboxes.clear();
        this.preloadedTextures.forEach(Minecraft.getInstance().getTextureManager()::release);
        this.preloadedTextures.clear();
    }

    @Override
    public void renderSkyboxes(SkyRenderer skyRenderer, PoseStack poseStack, float tickDelta, Camera camera, MultiBufferSource.BufferSource bufferSource, FogParameters fogParameters) {
        for (AbstractSkybox abstractSkybox : this.activeAbstractSkyboxes) {
            abstractSkybox.render(skyRenderer, poseStack, tickDelta, camera, bufferSource, fogParameters);
        }
    }

    @Override
    public void tick(ClientLevel level) {
        for (AbstractSkybox abstractSkybox : Iterables.concat(this.skyboxMap.values(), this.permanentSkyboxMap.values())) {
            abstractSkybox.tick(level);
        }

        this.activeAbstractSkyboxes.removeIf(abstractSkybox -> !abstractSkybox.isActive());
        for (AbstractSkybox abstractSkybox : Iterables.concat(this.skyboxMap.values(), this.permanentSkyboxMap.values())) {
            if (!this.activeAbstractSkyboxes.contains(abstractSkybox) && abstractSkybox.isActive()) {
                this.activeAbstractSkyboxes.add(abstractSkybox);
            }
        }

        this.activeAbstractSkyboxes.sort(Comparator.comparingInt(AbstractSkybox::getLayer));
    }

    @Override
    public List<AbstractSkybox> getActiveSkyboxes() {
        return this.activeAbstractSkyboxes;
    }
}