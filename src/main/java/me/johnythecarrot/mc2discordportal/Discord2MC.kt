package me.johnythecarrot.mc2discordportal

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Bukkit

class Discord2MC : ListenerAdapter() {

    companion object
    {
        fun markdown(str: String, currentlyBold: Boolean, currentlyItalic: Boolean): String
        {
            var bold = currentlyBold
            var italic = currentlyItalic

            val boldReg = Regex("\\*\\*(\\*?(?:(?!\\*\\*).)+\\*?)\\*\\*")
            boldReg.findAll(str).forEach { _ -> bold = true; return@forEach }
            var endString: String = boldReg.replace(str) {
                m -> "§l" + markdown(m.groupValues[1], bold, currentlyItalic) + "§r" + if (currentlyItalic) "§o" else ""
            }
            val italicReg = Regex("\\*((?:(?!\\*).)+)\\*")
            italicReg.findAll(str).forEach { _ -> italic = true; return@forEach }
            endString = italicReg.replace(endString) {
                m -> "§o" + markdown(m.groupValues[1], currentlyBold, italic) + "§r" + if (currentlyBold) "§l" else ""
            }
            return endString
        }
    }

    private fun interpretContent(content: String, event: MessageReceivedEvent): String {
        var toReturn: String
        val emoteRegex = Regex("<a?:([0-9a-zA-Z]+):[0-9]+>")
        toReturn = emoteRegex.replace(content)
        {
            m -> "§b:${emoteRegex.find(m.value)?.groupValues?.get(1)!!}:§r"
        }
        val pingRegex = Regex("<@!?([0-9]+)>")
        toReturn = pingRegex.replace(toReturn) {
            m -> "§9@" + (
                event.guild.getMemberById(
                        pingRegex.find(m.value)?.groupValues?.get(1) ?: "123465789123456789"
                )?.user?.name ?: "unknown-user") + "§r"
        }
        val channelRegex = Regex("<#([0-9]+)>")
        toReturn = channelRegex.replace(toReturn) {
            m -> "§9#" + (
                event.guild.getGuildChannelById(
                channelRegex.find(m.value)?.groupValues?.get(1) ?: "123465789123456789"
                )?.name ?: "unknown-channel") + "§r"
        }
        toReturn = markdown(toReturn, currentlyBold = false, currentlyItalic = false)
        return toReturn
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot) return
        if (event.channel.id == Main.getString("discord_channel_id_to_listen_to"))
        {
            with (event.message.contentRaw)
            {
                when {
                    startsWith("/qt") -> {
                        val target = event.message.contentRaw.split("/qt ")[1]
                        Bukkit.getServer().onlinePlayers.forEach { player ->
                            player.sendMessage("$target has made the advancement §a[Be a Massive QT]")
                        }
                        return
                    }
                    startsWith("/online") -> {
                        val playerList = Bukkit.getServer().onlinePlayers
                                .joinToString(", ") {
                                    player -> player.name.replace("@", "@\u200B")
                                }
                        event.channel.sendMessage(
                                "Currently online player list (${Bukkit.getServer().onlinePlayers.size}):\n$playerList"
                        ).queue()
                    }
                    else -> {
                        val content = interpretContent(event.message.contentRaw, event)
                        val messageLinkRegex = Regex("https://(?:(canary|ptb)\\.)?discordapp\\.com/channels/([0-9]+)/([0-9]+)/([0-9]+)")
                        val textComponent = TextComponent(
                                "§9[Discord] ${event.author.name}#${event.author.discriminator} »§r "
                        )
                        var index = 0
                        var matches = 0
                        messageLinkRegex.findAll(content).forEachIndexed { i, _ -> matches = i+1 }
                        if (matches == 0)
                        {
                            Bukkit.getServer().onlinePlayers.forEach { player ->
                                player.sendMessage("§9[Discord] ${event.author.name}#${event.author.discriminator} »§r $content")
                            }
                            return
                        }
                        val componentList = arrayListOf<TextComponent>().toMutableList()
                        fun handleMessageLink(m: MatchResult)
                        {
                            event.guild.getTextChannelById(m.groupValues[2])
                                    ?.retrieveMessageById(m.groupValues[3])
                                    ?.queue { msg ->
                                        val component = TextComponent("§amessage (hover)§r")
                                        component.hoverEvent = HoverEvent(
                                                HoverEvent.Action.SHOW_TEXT,
                                                ComponentBuilder(
                                                        "${msg.author.name}#${msg.author.discriminator} » ${interpretContent(msg.contentRaw, event)}"
                                                ).create()
                                        )
                                        componentList.add(component)
                                        index++
                                        if (index == matches)
                                        {
                                            messageLinkRegex.replace(content, "msg-link")
                                                    .split("msg-link").forEachIndexed { i, text ->
                                                        textComponent.addExtra(text)
                                                        if (i < componentList.size)
                                                            textComponent.addExtra(componentList[i])
                                                    }
                                            Bukkit.getServer().onlinePlayers.forEach { player ->
                                                player.spigot().sendMessage(textComponent)
                                            }
                                        }
                                    }
                        }
                        messageLinkRegex.replace(content) {
                            m -> handleMessageLink(m); "msg-link"
                        }
                    }
                }
            }
        }
    }

}