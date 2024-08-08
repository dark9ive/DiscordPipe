package com.dark9ive.DiscordPipe;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class PlayerChatListener implements Listener {

    private final JDA jda;
    private final String channelId;

    public PlayerChatListener(JDA jda, String channelId) {
        this.jda = jda;
        this.channelId = channelId;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String playerName = escapeDiscordMarkdown(event.getPlayer().getName());
        String message = parseEmoji(event.getMessage());

        if (jda != null) {
            TextChannel channel = jda.getTextChannelById(channelId);
            if (channel != null) {
                channel.sendMessage("**<" + playerName + "@mcserver>**").queue();
                channel.sendMessage(message).queue();
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
    private String parseEmoji(String text) {
        Guild guild = jda.getTextChannelById(channelId).getGuild();
        if (guild != null) {
            for (CustomEmoji emoji : guild.retrieveEmojis().complete()) {
                if (emoji.isAnimated()){
                    text = text.replace(":" + emoji.getName() + ":", "<a:" + emoji.getAsReactionCode() + ">");
                }
                else{
                    text = text.replace(":" + emoji.getName() + ":", "<:" + emoji.getAsReactionCode() + ">");
                }
            }
        }
        return text;
    }
}