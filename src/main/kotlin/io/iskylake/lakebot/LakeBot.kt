/*
 *  Copyright 2017-2018 (c) Alexander "ISkylake" Shevchenko
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

@file:JvmName("LakeBot")
package io.iskylake.lakebot

import io.iskylake.lakebot.commands.Command
import io.iskylake.lakebot.commands.general.UptimeCommand
import io.iskylake.lakebot.entities.extensions.*
import io.iskylake.lakebot.entities.handlers.CommandHandler
import io.iskylake.lakebot.entities.handlers.EventHandler

import kotlinx.coroutines.experimental.runBlocking

import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Game.streaming

import kotlin.system.exitProcess

val DEFAULT_COMMANDS = listOf<Command>(
    UptimeCommand()
)
lateinit var discord: JDA
fun main(args: Array<String>) {
    try {
        runBlocking<Unit> {
            for (command in DEFAULT_COMMANDS) {
                CommandHandler += command
            }
            discord = buildJDA {
                token {
                    Immutable.BOT_TOKEN
                }
                game {
                    streaming("${Immutable.VERSION} | ${Immutable.DEFAULT_PREFIX}help", "https://twitch.tv/raidlier")
                }
                eventListener {
                    EventHandler
                }
            }
            System.setProperty("lakebot.version", Immutable.VERSION)
            System.setProperty("kotlin.version", KotlinVersion.CURRENT.toString())
        }
    } catch (e: Exception) {
        e.printStackTrace()
        exitProcess(0)
    }
}