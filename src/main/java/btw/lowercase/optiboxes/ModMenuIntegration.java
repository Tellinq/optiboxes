package btw.lowercase.optiboxes;

import btw.lowercase.optiboxes.config.OptiBoxesConfig;
import btw.lowercase.optiboxes.config.OptiBoxesConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new OptiBoxesConfigScreen(parent, OptiBoxesConfig.INSTANCE);
    }
}