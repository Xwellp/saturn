package xwellp.saturn;

import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.renderer.packer.GuiTexture;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xwellp.saturn.modules.*;
import xwellp.saturn.settings.StringPairSetting;

import java.util.Random;

public class Saturn extends MeteorAddon {
    public static final Logger LOG = LoggerFactory.getLogger("Saturn");
    public static final Category CATEGORY = new Category("Saturn", Items.DIAMOND.getDefaultStack());
    public static final HudGroup HUD_GROUP = new HudGroup("Saturn");
    private static final String[] MESSAGES = {
            "Saturn is coming",
    };
    public static String MOD_ID = "saturn";
    public static GuiTexture ARROW_UP;
    public static GuiTexture ARROW_DOWN;
    public static GuiTexture COPY;
    public static GuiTexture EYE;

    public static Identifier identifier(String path) {
        return Identifier.of(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        Random random = new Random();
        LOG.info(MESSAGES[random.nextInt(MESSAGES.length)]);

        LOG.info("Registering custom factories...");
        StringPairSetting.register();

        LOG.info("Registering modules...");
        Modules.get().add(new FightHelper());
        Modules.get().add(new RotienderBox());
        Modules.get().add(new SwingAnimation());

        ARROW_UP = GuiRenderer.addTexture(identifier("textures/icons/gui/arrow_up.png"));
        ARROW_DOWN = GuiRenderer.addTexture(identifier("textures/icons/gui/arrow_down.png"));
        COPY = GuiRenderer.addTexture(identifier("textures/icons/gui/copy.png"));
        EYE = GuiRenderer.addTexture(identifier("textures/icons/gui/eye.png"));
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(CATEGORY);
    }

    @Override
    public String getPackage() {
        return getRepo().owner() + "." + getRepo().name();
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("xwellp", "saturn");
    }
}