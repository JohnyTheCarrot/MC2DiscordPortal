package me.johnythecarrot.mc2discordportal

import club.minnced.discord.webhook.WebhookClient
import club.minnced.discord.webhook.WebhookClientBuilder
import club.minnced.discord.webhook.send.WebhookMessageBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin


class Main : JavaPlugin(), Listener {

    private lateinit var client: WebhookClient
    private var jda: JDA? = null
    companion object
    {
        private var instance: Main? = null
        fun getString(key: String) : String
        {
            return instance!!.config.getString(key)!!
        }
    }
    override fun onEnable()
    {
        instance = this
        config.run {
            addDefault("enable_this_when_config_is_setup_and_valid", false)
            addDefault("webhook_url", "token here")
            addDefault("bot_token", "a.real.token")
            //addDefault("message_avatar_url", "http://dootdoot.tk/picsforbot/chat.png")
            addDefault("server_join_avatar", "http://dootdoot.tk/picsforbot/login.png")
            addDefault("server_leave_avatar", "http://dootdoot.tk/picsforbot/exit.png")
            addDefault("system_message_name", "Server")
            addDefault("discord_channel_id_to_listen_to", "123456789")
            addDefault("channel_topic",
                    "{ONLINE}/{MAX_PLAYERS} players online | " +
                            "{TOTAL_UNIQUE_PLAYERS} unique players ever joined | " +
                            "Last update: {DESC_LAST_UPDATE}"
            )
            options().copyDefaults(true)
        }
        saveConfig()
        if (config.getBoolean("enable_this_when_config_is_setup_and_valid")) {
            jda = JDABuilder(config.getString("bot_token"))
                    .addEventListeners(Discord2MC())
                    .build()
            val builder = WebhookClientBuilder(config.getString("webhook_url")!!) // or id, token

            builder.setThreadFactory { job: Runnable? ->
                val thread = Thread(job)
                thread.name = "Catblush"
                thread.isDaemon = true
                thread
            }
            builder.setWait(true)
            client = builder.build()
            Bukkit.getPluginManager().registerEvents(this, this)
        }
    }

    override fun onDisable() {
        jda?.shutdownNow()
    }

    private fun String.replace(s: String, int: Int): String {
        return this.replace(s, int.toString())
    }

    private val AsyncPlayerChatEvent.sanitizedMessage
        get() = this.message.replace("@", "@\u200B")

    private val Player.sanitizedName
        get() = this.name.replace("@", "@\u200B")

    private val String.mcFormatted
        get() = run {
            this.replace("{MAX_PLAYERS}", Bukkit.getServer().maxPlayers)
                    .replace("{DESC_LAST_UPDATE}", "" + java.util.Calendar.getInstance().time)
        }

    @EventHandler
    fun onPlayerChat(e: AsyncPlayerChatEvent)
    {
        val builder = WebhookMessageBuilder()
        builder.setUsername(e.player.name)
        //builder.setAvatarUrl(config.getString("message_avatar_url"))
        builder.setAvatarUrl("https://minotar.net/helm/${e.player.name}/60.png")
        builder.setContent(e.sanitizedMessage)
        client.send(builder.build())
        e.message = Discord2MC.markdown(e.message, currentlyBold = false, currentlyItalic = false)
    }

    @EventHandler
    fun onPlayerJoin(e: PlayerJoinEvent)
    {
        val builder = WebhookMessageBuilder()
        builder.setUsername("Server")
        builder.setContent("**${e.player.sanitizedName}** has joined the server.")
        builder.setAvatarUrl(config.getString("server_join_avatar"))
        client.send(builder.build())
        var offlinePlayerCount = 0
        Bukkit.getServer().offlinePlayers.forEach { player ->
            if (!Bukkit.getOnlinePlayers().contains(player))
                offlinePlayerCount += 1
        }
        jda?.getGuildChannelById(config.getString("discord_channel_id_to_listen_to")!!)?.manager?.setTopic(
                config.getString("channel_topic")!!.mcFormatted
                        .replace("{TOTAL_UNIQUE_PLAYERS}",
                                offlinePlayerCount + Bukkit.getServer().onlinePlayers.size
                        )
                        .replace("{ONLINE}", Bukkit.getOnlinePlayers().size)
        )?.queue()
    }

    @EventHandler
    fun onPlayerLeave(e: PlayerQuitEvent)
    {
        val builder = WebhookMessageBuilder()
        builder.setUsername("Server")
        builder.setContent("**${e.player.sanitizedName}** has left the server.")
        builder.setAvatarUrl(config.getString("server_leave_avatar"))
        client.send(builder.build())
        var offlinePlayerCount = 0
        Bukkit.getServer().offlinePlayers.forEach { player ->
            if (!Bukkit.getOnlinePlayers().contains(player))
                offlinePlayerCount += 1
        }
        jda?.getGuildChannelById(config.getString("discord_channel_id_to_listen_to")!!)?.manager?.setTopic(
                config.getString("channel_topic")!!.mcFormatted
                        .replace("{TOTAL_UNIQUE_PLAYERS}",
                                offlinePlayerCount + Bukkit.getServer().onlinePlayers.size
                        )
                        .replace("{ONLINE}", Bukkit.getOnlinePlayers().size-1)
        )?.queue()
    }

}
