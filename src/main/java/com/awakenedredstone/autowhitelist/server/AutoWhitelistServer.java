package com.awakenedredstone.autowhitelist.server;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.commands.AutoWhitelistCommand;
import com.awakenedredstone.autowhitelist.discord.Bot;
import com.awakenedredstone.autowhitelist.lang.CustomLanguage;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import java.io.*;
import java.nio.file.Files;

import static com.awakenedredstone.autowhitelist.AutoWhitelist.config;
import static com.awakenedredstone.autowhitelist.lang.CustomLanguage.translations;

@Environment(EnvType.SERVER)
public class AutoWhitelistServer implements DedicatedServerModInitializer {

    @Override
    public void onInitializeServer() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> AutoWhitelistCommand.register(server.getCommandManager().getDispatcher()));
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, serverResourceManager, success) -> AutoWhitelistCommand.register(server.getCommandManager().getDispatcher()));
        ServerLifecycleEvents.SERVER_STOPPING.register((server -> Bot.stopBot(false)));
        ServerLifecycleEvents.SERVER_STARTED.register((server -> {
            AutoWhitelist.server = server;
            try {
                {
                    InputStream inputStream = AutoWhitelistServer.class.getResource("/messages.json").openStream();
                    CustomLanguage.load(inputStream, translations::put);
                }
                File file = new File(config.getConfigDirectory(), "messages.json");
                if (!file.exists()) {
                    Files.copy(AutoWhitelistServer.class.getResource("/messages.json").openStream(), file.toPath());
                }

                InputStream inputStream = Files.newInputStream(file.toPath());
                CustomLanguage.load(inputStream, translations::put);
            } catch (IOException ignored) {}

            new Bot().start();
        }));

    }
}
