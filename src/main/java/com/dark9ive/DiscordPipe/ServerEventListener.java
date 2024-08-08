package com.dark9ive.DiscordPipe;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

public class ServerEventListener implements Listener {
    private final JDA jda;
    private final String channelId;

    public ServerEventListener(JDA jda, String channelId) {
        this.jda = jda;
        this.channelId = channelId;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        String deathMessage = escapeDiscordMarkdown(event.getDeathMessage());

        if (jda != null) {
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel != null) {
                channel.sendMessage("**[server]** " + deathMessage).queue();
            }
        }
    }

    @EventHandler
    public void onPlayerAchievement(PlayerAdvancementDoneEvent event) {
        String achievementMessage = escapeDiscordMarkdown(event.getPlayer().getName()) + " has just earned the achievement [" + event.getAdvancement().getDisplay().getTitle() + "]";

        if (jda != null) {
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel != null) {
                channel.sendMessage("**[server]** " + achievementMessage).queue();
            }
        }
    }

    private String escapeDiscordMarkdown(String text) {
        return text.replace("_", "\\_")
                   .replace("*", "\\*")
                   .replace("~", "\\~")
                   .replace("`", "\\`")
                   .replace("|", "\\|");
    }
}