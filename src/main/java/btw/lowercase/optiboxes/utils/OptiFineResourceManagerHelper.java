package btw.lowercase.optiboxes.utils;

import btw.lowercase.optiboxes.OptiBoxesClient;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

public class OptiFineResourceManagerHelper implements IdentifiableResourceReloadListener {
    private ResourceManager resourceManager;

    @Override
    public @NotNull CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, ResourceManager resourceManager, Executor executor, Executor executor2) {
        this.resourceManager = resourceManager;
        return CompletableFuture.runAsync(() -> OptiBoxesClient.INSTANCE.inject(this)).thenCompose(preparationBarrier::wait);
    }

    @Override
    public ResourceLocation getFabricId() {
        return ResourceLocation.tryBuild(OptiBoxesClient.MOD_ID, "skybox_reader");
    }

    public Stream<ResourceLocation> searchIn(String parent) {
        return this.resourceManager.listResources(parent, path -> true).keySet().stream();
    }

    public InputStream getInputStream(ResourceLocation resourceLocation) {
        try {
            Resource resource = this.resourceManager.getResource(resourceLocation).orElse(null);
            return resource == null ? null : resource.open();
        } catch (IOException e) {
            return null;
        }
    }
}