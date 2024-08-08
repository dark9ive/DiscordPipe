package com.dark9ive.DiscordPipe;

import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import net.md_5.bungee.api.ChatColor;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class DiscordPipe extends JavaPlugin implements Listener {

    private String discordToken;
    private String channelId;
    private JDA jda;

    @Override
    public void onEnable() {
        if (!loadConfig() || !initializeBot()) {
            getLogger().warning("DiscordLogger initialization failed. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(new PlayerChatListener(jda, channelId), this);
        getServer().getPluginManager().registerEvents(new ServerEventListener(jda, channelId), this);
        getLogger().info("DiscordLogger has been enabled!");
    }

    @Override
    public void onDisable() {
        if (jda != null) {
            try {
                jda.shutdown();
                // Allow at most 10 seconds for remaining requests to finish
                if (!jda.awaitShutdown(10, TimeUnit.SECONDS)) {
                    jda.shutdownNow(); // Cancel all remaining requests
                    jda.awaitShutdown(); // Wait until shutdown is complete (indefinitely)
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        getLogger().info("DiscordLogger has been disabled!");
    }

    private boolean loadConfig() {
        Properties properties = new Properties();
        File configFile = new File(getServer().getWorldContainer(), "server.properties");

        try (FileInputStream in = new FileInputStream(configFile)) {
            properties.load(in);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        discordToken = properties.getProperty("discord.bot.token", "").trim();
        channelId = properties.getProperty("discord.channel.id", "").trim();

        if (discordToken.isEmpty() || channelId.isEmpty()) {
            try (FileOutputStream out = new FileOutputStream(configFile)) {
                if (discordToken.isEmpty()) properties.setProperty("discord.bot.token", "");
                if (channelId.isEmpty()) properties.setProperty("discord.channel.id", "");
                properties.store(out, null);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return false;
        }

        return true;
    }

    private boolean initializeBot() {
        try {
            jda = JDABuilder.createDefault(discordToken)
                    .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                    .disableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE)
                    .addEventListeners(new DiscordListener())
                    .build();
            jda.awaitReady();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            getLogger().warning("Invalid Discord channel ID. Discord integration will be disabled.");
            return false;
        }

        getLogger().info("Successfully connected to Discord channel: " + channel.getName());
        return true;
    }

    private class DiscordListener extends ListenerAdapter {
        @Override
        public void onMessageReceived(MessageReceivedEvent event) {
            if (event.getChannel().getId().equals(channelId) && !event.getAuthor().isBot()) {
                String nickname = event.getMember().getEffectiveName();
                String messageContent = event.getMessage().getContentDisplay();
    
                // Get the color of the highest role
                ChatColor nickcolor = ChatColor.WHITE; // Default color
                if (!event.getMember().getRoles().isEmpty()) {
                    Role highestRole = event.getMember().getRoles().get(0);
                    for (Role role : event.getMember().getRoles()) {
                        if (role.getPosition() > highestRole.getPosition()) {
                            highestRole = role;
                        }
                    }
                    if (highestRole.getColor() != null) {
                        nickcolor = ChatColor.of(highestRole.getColor());
                    }
                }
                
                if (!messageContent.isEmpty()) {
                    String coloredMessage = "<" + nickcolor + nickname + ChatColor.RESET + "@" + jda.getTextChannelById(channelId).getName() + "> " + messageContent;
    
                    Bukkit.getScheduler().runTask(DiscordPipe.this, () -> 
                        Bukkit.broadcastMessage(coloredMessage)
                    );
                }
                for (Attachment attachment : event.getMessage().getAttachments()) {
                    String attachmentMessage = nickcolor + nickname + ChatColor.RESET + " sent an attachment: " + attachment.getFileName();
                    if (attachment.isImage()) {
                        attachmentMessage = nickcolor + nickname + ChatColor.RESET + " sent an image: " + attachment.getFileName();
                    } else if (attachment.isVideo()) {
                        attachmentMessage = nickcolor + nickname + ChatColor.RESET + " sent a video: " + attachment.getFileName();
                    }
                    String finalAttachmentMessage = attachmentMessage;
                    Bukkit.getScheduler().runTask(DiscordPipe.this, () -> 
                        Bukkit.broadcastMessage(finalAttachmentMessage)
                    );
                }
            }
        }
    }
}

