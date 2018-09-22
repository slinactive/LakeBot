/*
 * Copyright 2017-2018 (c) Alexander "ISkylake" Shevchenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.iskylake.lakebot

import com.mongodb.MongoClient

import io.iskylake.lakebot.commands.`fun`.*
import io.iskylake.lakebot.commands.animals.*
import io.iskylake.lakebot.commands.audio.*
import io.iskylake.lakebot.commands.developer.*
import io.iskylake.lakebot.commands.general.*
import io.iskylake.lakebot.commands.info.*
import io.iskylake.lakebot.commands.moderation.*
import io.iskylake.lakebot.commands.utils.*
import io.iskylake.lakebot.entities.EventWaiter
import io.iskylake.lakebot.entities.extensions.*
import io.iskylake.lakebot.entities.handlers.CommandHandler
import io.iskylake.lakebot.entities.handlers.EventHandler
import io.iskylake.lakebot.utils.ConfigUtils

import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking

import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.entities.Game.streaming
import net.dv8tion.jda.core.entities.Game.watching
import net.dv8tion.jda.core.entities.User

import kotlin.system.exitProcess

val DEFAULT_COMMANDS = listOf(
        // Animals
        AlpacaCommand(),
        BirdCommand(),
        CatCommand(),
        DogCommand(),
        // Audio
        JoinCommand(),
        JumpCommand(),
        JumpBackCommand(),
        JumpForwardCommand(),
        LeaveCommand(),
        LoopCommand(),
        LyricsCommand(),
        PauseCommand(),
        PlayCommand(),
        PlayingCommand(),
        QueueCommand(),
        RemoveCommand(),
        RestartCommand(),
        ResumeCommand(),
        SelectCommand(),
        SkipCommand(),
        StopCommand(),
        VolumeCommand(),
        YouTubePlayCommand(),
        // Developer
        EvalCommand(),
        LakeBanCommand(),
        LakeUnbanCommand(),
        ShutdownCommand(),
        // Fun
        AkinatorCommand(),
        ChooseCommand(),
        GuessGameCommand(),
        InvertCommand(),
        SayCommand(),
        TextToImageCommand(),
        // General
        AboutCommand(),
        HelpCommand(),
        InviteCommand(),
        PingCommand(),
        SupportCommand(),
        UptimeCommand(),
        // Info
        EmotesCommand(),
        RoleCommand(),
        ServerCommand(),
        UserCommand(),
        // Moderation
        MuteCommand(),
        MuteRoleCommand(),
        PrefixCommand(),
        PruneCommand(),
        UnmuteCommand(),
        // Utils
        CalculatorCommand(),
        ColorCommand(),
        EmojiCommand(),
        GoogleCommand(),
        QRCommand(),
        ShortenerCommand(),
        TimeCommand(),
        TranslateCommand(),
        UrbanCommand(),
        WeatherCommand(),
        YouTubeCommand()
)
val USERS_WITH_PROCESSES = mutableListOf<User>()
lateinit var DISCORD: JDA
private lateinit var client: MongoClient
fun main(args: Array<String>) = try {
    runBlocking {
        client = ConfigUtils.CLIENT
        delay(1000)
        Immutable.LOGGER.info("MongoDB was successfully loaded!")
        DISCORD = buildJDA {
            token { Immutable.BOT_TOKEN }
            eventListener { EventHandler }
            eventListener { EventWaiter }
            game { watching("loading..") }
            onlineStatus { OnlineStatus.DO_NOT_DISTURB }
        }
        Immutable.LOGGER.info("JDA was successfully built!")
        for (command in DEFAULT_COMMANDS) {
            CommandHandler += command
        }
        Immutable.LOGGER.info("CommandHandler successfully loaded all ${CommandHandler.registeredCommands.size} commands!")
        System.setProperty("lakebot.version", Immutable.VERSION)
        System.setProperty("kotlin.version", KotlinVersion.CURRENT.toString())
        DISCORD.presence.game = streaming("${Immutable.VERSION} | ${Immutable.DEFAULT_PREFIX}help", "https://twitch.tv/raidlier")
        DISCORD.presence.status = OnlineStatus.ONLINE
        Immutable.LOGGER.info("LakeBot is successfully loaded!")
    }
} catch (e: Exception) {
    e.printStackTrace()
    exitProcess(0)
}