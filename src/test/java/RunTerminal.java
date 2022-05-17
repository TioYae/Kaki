import java.io.File;

import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import net.mamoe.mirai.Bot;
import net.mamoe.mirai.BotFactory;
import net.mamoe.mirai.console.MiraiConsole;
import net.mamoe.mirai.console.extension.PluginComponentStorage;
import net.mamoe.mirai.console.terminal.MiraiConsoleImplementationTerminal;
import net.mamoe.mirai.console.terminal.MiraiConsoleTerminalLoader;
import net.mamoe.mirai.utils.BotConfiguration;
import org.TioYae.mirai.Kaki;
import org.jetbrains.annotations.NotNull;

class RunTerminal {
    static void setupWorkingDir() {
        // see: net.mamoe.mirai.console.terminal.MiraiConsoleImplementationTerminal
        System.setProperty("user.dir", new File("debug-sandbox").getAbsolutePath());
    }

    public static void main(String[] args) {
        setupWorkingDir();

        MiraiConsoleTerminalLoader.INSTANCE.startAsDaemon(new MiraiConsoleImplementationTerminal());

        Kaki pluginInstance = Kaki.INSTANCE;

        pluginInstance.onLoad(new PluginComponentStorage(new Kaki())); // 主动加载插件, Console 会调用 Kaki.onLoad
        pluginInstance.onEnable(); // 主动启用插件, Console 会调用 Kaki.onEnable

        Bot bot = BotFactory.INSTANCE.newBot(2418349874L, "sakura33daisuki", new BotConfiguration() {{
            setHeartbeatStrategy(BotConfiguration.HeartbeatStrategy.REGISTER);
            setProtocol(MiraiProtocol.ANDROID_PAD);
        }});
        bot.login();

        MiraiConsole.INSTANCE.getJob().join(new Continuation<Unit>() {
            @NotNull
            @Override
            public CoroutineContext getContext() {
                return null;
            }

            @Override
            public void resumeWith(@NotNull Object o) {

            }
        });
    }
}