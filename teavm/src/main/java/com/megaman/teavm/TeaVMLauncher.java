package com.megaman.teavm;

import com.github.xpenatan.gdx.backends.teavm.TeaApplication;
import com.github.xpenatan.gdx.backends.teavm.TeaApplicationConfiguration;
import com.github.xpenatan.gdx.backends.teavm.config.AssetFileHandle;
import com.github.xpenatan.gdx.backends.teavm.config.TeaBuildConfiguration;
import com.github.xpenatan.gdx.backends.teavm.config.TeaBuilder;
import com.megaman.CoreGame;
import org.teavm.tooling.TeaVMTool;

import java.io.File;
import java.io.IOException;

/** 网页端（TeaVM / WebGL）启动器：浏览器加载后由此进入游戏。 */
public class TeaVMLauncher {
    public static void main(String[] args) {
        TeaApplicationConfiguration config = new TeaApplicationConfiguration("canvas");
        config.width = 0;   // 0 = 跟随画布自适应
        config.height = 0;
        new TeaApplication(new CoreGame(), config);
    }
}

/** 构建器：把游戏编译为 HTML5/JS，输出到 teavm/build/dist。 */
class TeaVMBuilder {
    public static void main(String[] args) throws IOException {
        TeaBuildConfiguration cfg = new TeaBuildConfiguration();
        cfg.assetsPath.add(new AssetFileHandle("../assets"));
        cfg.webappPath = new File("build/dist").getCanonicalPath();
        cfg.mainClass = "com.megaman.teavm.TeaVMLauncher";
        cfg.mainApplicationClass = "com.megaman.teavm.TeaVMLauncher";
        cfg.htmlTitle = "Mega Platformer";
        cfg.htmlWidth = 960;
        cfg.htmlHeight = 540;
        cfg.useDefaultHtmlIndex = true;
        cfg.showLoadingLogo = false;

        TeaVMTool tool = TeaBuilder.config(cfg);
        TeaBuilder.build(tool);
    }
}
