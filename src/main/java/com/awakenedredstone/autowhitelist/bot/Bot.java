package com.awakenedredstone.autowhitelist.bot;

import com.awakenedredstone.autowhitelist.AutoWhitelist;
import com.awakenedredstone.autowhitelist.database.SQLite;
import com.awakenedredstone.autowhitelist.util.InvalidResultException;
import com.awakenedredstone.autowhitelist.util.MemberPlayer;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.login.LoginException;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Bot extends ListenerAdapter {

    private static Bot instance;

    private static ScheduledFuture<?> scheduledUpdate;

    private static JDA jda = null;
    private static String token = AutoWhitelist.config.getConfigs().get("token").getAsString();
    private static String appId = AutoWhitelist.config.getConfigs().get("application-id").getAsString();
    private static String serverId = AutoWhitelist.config.getConfigs().get("discord-server-id").getAsString();
    private static String prefix = AutoWhitelist.config.getConfigs().get("prefix").getAsString();
    private static long updateDelay = AutoWhitelist.config.getConfigs().get("whitelist-auto-update-delay-seconds").getAsLong();

    public Bot() {
        init();
    }

    private void sendFeedbackMessage(MessageChannel channel, String title, String message) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor(jda.getSelfUser().getName(), "https://discord.com", jda.getSelfUser().getAvatarUrl());
        embedBuilder.setTitle(title);
        embedBuilder.setDescription(message);
        embedBuilder.setFooter("*Minecraft PhoenixSC Edition*");
        MessageAction messageAction = channel.sendMessage(embedBuilder.build());
    }

    private void sendTempFeedbackMessage(MessageChannel channel, String title, String message, int seconds) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor(jda.getSelfUser().getName(), "https://discord.com", jda.getSelfUser().getAvatarUrl());
        embedBuilder.setTitle(title);
        embedBuilder.setDescription(message);
        embedBuilder.setFooter(String.format("This message will be deleted %s seconds after being sent.", seconds));
        MessageAction messageAction = channel.sendMessage(embedBuilder.build());
        messageAction.queue(m -> m.delete().queueAfter(seconds, TimeUnit.SECONDS));
    }

    public static void stopBot() {
        if (jda != null) {
            scheduledUpdate.cancel(true);
            jda.shutdown();
        }
    }

    public static Bot getInstance() {
        return instance;
    }

    public void reloadBot(ServerCommandSource source) {
        token = AutoWhitelist.config.getConfigs().get("token").getAsString();
        appId = AutoWhitelist.config.getConfigs().get("application-id").getAsString();
        serverId = AutoWhitelist.config.getConfigs().get("discord-server-id").getAsString();
        prefix = AutoWhitelist.config.getConfigs().get("prefix").getAsString();
        updateDelay = AutoWhitelist.config.getConfigs().get("whitelist-auto-update-delay-seconds").getAsLong();
        source.sendFeedback(new LiteralText("Restarting the bot."), true);
        scheduledUpdate.cancel(true);
        jda.shutdown();
        init();
        source.sendFeedback(new LiteralText("Discord bot starting."), true);
    }

    private void init() {
        try {
            jda = JDABuilder.createDefault(token).enableIntents(GatewayIntent.GUILD_MEMBERS).setMemberCachePolicy(MemberCachePolicy.ALL).build();
            jda.addEventListener(this);
            jda.getPresence().setActivity(Activity.playing("on the Member Server"));
            instance = this;
        } catch (LoginException e) {
            AutoWhitelist.logger.error("Failed to start bot, got an Exception", e);
        }
    }

    @Override
    public void onReady(@NotNull ReadyEvent e) {
        AutoWhitelist.logger.info("Bot started. Parsing registered users.");
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
        scheduledUpdate = scheduler.scheduleWithFixedDelay(this::updateWhitelist, 0, updateDelay, TimeUnit.SECONDS);
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent e) {
        User user = e.getUser();

        List<MemberPlayer> players = new ArrayList<>();
        new SQLite().getMembers().stream().filter(member -> user.getId().equals(member.getUserId())).findFirst().ifPresent(players::add);
        if (players.size() > 1) {
            AutoWhitelist.logger.error("Found more than one registered user with same discord id: " + user.getId());
            return;
        } else if (players.size() == 0) return;
        MemberPlayer player = players.get(0);

        if (!AutoWhitelist.server.getPlayerManager().isOperator(player.getProfile())) {
            new SQLite().removeMemberById(player.getUserId());
            AutoWhitelist.removePlayer(player.getProfile());
        }

        AutoWhitelist.updateWhitelist();
    }

    private void updateWhitelist() {
        List<String> ids = new SQLite().getIds();
        Guild guild = jda.getGuildById(serverId);
        if (guild == null) {
            AutoWhitelist.logger.error("Failed to get discord server, got null");
            return;
        }
        guild.loadMembers().onSuccess(members -> {
            List<User> users = members.stream().map(Member::getUser).filter(member -> ids.contains(member.getId())).collect(Collectors.toList());

            for (User user : users) {

                List<MemberPlayer> players = new SQLite().getMembers().stream().filter(player -> user.getId().equals(player.getUserId())).collect(Collectors.toList());
                MemberPlayer player = players.get(0);

                List<String> roles = getMemberRoles();
                List<Role> userRoles = user.getJDA().getRoles().stream().filter((role) -> roles.contains(role.getId())).collect(Collectors.toList());
                if (userRoles.size() >= 1) {
                    int higher = 0;
                    Role best = null;
                    for (Role role : userRoles) {
                        if (role.getPosition() > higher) {
                            higher = role.getPosition();
                            best = role;
                        }
                    }
                    if (best == null) {
                        AutoWhitelist.logger.error("Failed to get best tier role!");
                        return;
                    }
                    for (Map.Entry<String, JsonElement> entry : AutoWhitelist.config.getConfigs().get("whitelist").getAsJsonObject().entrySet()) {
                        JsonArray jsonArray = entry.getValue().getAsJsonArray();
                        for (JsonElement value : jsonArray) {
                            if (value.getAsString().equals(best.getId())) {
                                if (!new SQLite().getIds().contains(user.getId())) {

                                    try {
                                        new SQLite().updateData(user.getId(), getUsername(player.getProfile().getId().toString()), player.getProfile().getId().toString(), entry.getKey());
                                    } catch (IOException e) {
                                        AutoWhitelist.logger.error("Failed to get username!", e);
                                        return;
                                    }
                                }
                            }
                        }
                    }
                } else if (!AutoWhitelist.server.getPlayerManager().isOperator(player.getProfile())) {
                    new SQLite().removeMemberById(player.getUserId());
                    AutoWhitelist.removePlayer(player.getProfile());
                }
            }
            AutoWhitelist.updateWhitelist();
        });
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent e) {
        if (e.isWebhookMessage() || e.getAuthor().isBot()) return;
        if (!e.getMessage().getContentRaw().startsWith(prefix + "register")) return;
        User _author = e.getAuthor();
        JDA author = e.getAuthor().getJDA();
        Message _message = e.getMessage();
        String message = _message.getContentRaw();
        message = message.replaceFirst(prefix + "register ", "");
        String[] values = message.split(" ");

        {
            if (new SQLite().getIds().contains(_author.getId())) {
                sendFeedbackMessage(e.getChannel(), "You only can register one account", "You can't have more than one Minecraft account registered.");
                return;
            }
        }

        {
            if (values.length != 1) {
                sendFeedbackMessage(e.getChannel(), "Invalid command usage.", String.format("Please not that the command is `%sregister <minecraft nickname> <uuid>`\nExample: `%sregister Notch`", prefix, prefix));
                return;
            } else if (values[0].length() > 16 || values[0].length() < 3) {
                sendFeedbackMessage(e.getChannel(), "Invalid nickname.", "The nickname you inserted is either too big or too small.");
                return;
            }
        }

        sendTempFeedbackMessage(e.getChannel(), "Command feedback.", "Your request has been received and is being processed, if you don't get another feedback message in the next minute than please contact a moderator.", 10);

        List<String> roles = getMemberRoles();
        List<Role> userRoles = author.getRoles().stream().filter((role) -> roles.contains(role.getId())).collect(Collectors.toList());
        if (userRoles.size() >= 1) {
            int higher = 0;
            Role best = null;
            for (Role role : userRoles) {
                if (role.getPosition() > higher) {
                    higher = role.getPosition();
                    best = role;
                }
            }
            if (best == null) {
                sendFeedbackMessage(e.getChannel(), "Something really bad happened.", "I was unable to get your best member role, due to that issue I couldn't add you to the server, please inform a moderator or PhoenixSC.");
                return;
            }
            for (Map.Entry<String, JsonElement> entry : AutoWhitelist.config.getConfigs().get("whitelist").getAsJsonObject().entrySet()) {
                JsonArray jsonArray = entry.getValue().getAsJsonArray();
                for (JsonElement value : jsonArray) {
                    if (value.getAsString().equals(best.getId())) {
                        if (!new SQLite().getIds().contains(_author.getId())) {
                            try {
                                List<GameProfile> players = new SQLite().getPlayers();
                                if (players.stream().map(GameProfile::getName).anyMatch(username -> username.equals(values[0])) && players.stream().map(GameProfile::getId).anyMatch(uuid -> {
                                    try {
                                        return uuid.toString().equals(getUUID(values[0]));
                                    } catch (IOException exception) {
                                        sendFeedbackMessage(e.getChannel(), "Something really bad happened.", "I was unable to get your UUID, due to that issue I couldn't add you to the server, please inform a moderator or PhoenixSC.");
                                        AutoWhitelist.logger.error("Failed to get UUID.", exception);
                                        return false;
                                    } catch (InvalidResultException exception) {
                                        sendFeedbackMessage(e.getChannel(), "Something went wrong.", "Seams that the username you inserted is not of an original Minecraft Java Edition account or the Mojang API is down. Please check if your username is correct, if it is than try again later.");
                                        return false;
                                    }
                                })) {
                                    sendFeedbackMessage(e.getChannel(), "This account is already registered.", "The username you inserted is already registered in the database.");
                                    return;
                                }

                                new SQLite().addMember(_author.getId(), values[0], getUUID(values[0]), entry.getKey());
                                sendFeedbackMessage(e.getChannel(), "Welcome to the group!", "Your Minecraft account has been added to the database and soon you will be able to join the server.");
                            } catch (IOException exception) {
                                sendFeedbackMessage(e.getChannel(), "Something really bad happened.", "I was unable to get your UUID, due to that issue I couldn't add you to the server, please inform a moderator or PhoenixSC.");
                                AutoWhitelist.logger.error("Failed to get UUID.", exception);
                                return;
                            } catch (InvalidResultException exception) {
                                sendFeedbackMessage(e.getChannel(), "Something went wrong.", "Seams that the username you inserted is not of an original Minecraft Java Edition account or the Mojang API is down. Please check if your username is correct, if it is than try again later.");
                                return;
                            }
                        }
                    }
                }
            }
        } else {
            sendFeedbackMessage(e.getChannel(), "Sorry, but I couldn't accept your request.", "It seams that you don't have the required subscription/member level or don't have your Twitch/Youtube account linked to your discord.");
        }
        AutoWhitelist.updateWhitelist();
    }

    private List<String> getMemberRoles() {
        List<String> roles = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : AutoWhitelist.config.getConfigs().get("whitelist").getAsJsonObject().entrySet()) {
            entry.getValue().getAsJsonArray().forEach((element) -> roles.add(element.getAsString()));
        }
        return roles;
    }

    private String getUUID(String username) throws IOException, InvalidResultException {
        URL url = new URL(String.format("https://api.mojang.com/users/profiles/minecraft/%s", username));

        try (InputStream is = url.openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String jsonText = readAll(rd);
            JsonParser parser = new JsonParser();
            JsonElement json = parser.parse(jsonText);
            if (json.getAsJsonObject().get("id") == null)
                throw new InvalidResultException("Invalid username:" + username);
            String _uuid = json.getAsJsonObject().get("id").getAsString();
            if (_uuid.length() != 32) throw new InvalidResultException("Invalid UUID string:" + _uuid);
            String[] split = new String[]{_uuid.substring(0, 8), _uuid.substring(8, 12), _uuid.substring(12, 16), _uuid.substring(16, 20), _uuid.substring(20, 32)};
            return split[0] + "-" + split[1] + "-" + split[2] + "-" + split[3] + "-" + split[4];
        }
    }

    private String getUsername(String uuid) throws IOException {
        URL url = new URL(String.format("https://sessionserver.mojang.com/session/minecraft/profile/%s", uuid));

        try (InputStream is = url.openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String jsonText = readAll(rd);
            JsonParser parser = new JsonParser();
            JsonElement json = parser.parse(jsonText);
            return json.getAsJsonObject().get("name").getAsString();
        }
    }

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }
}
