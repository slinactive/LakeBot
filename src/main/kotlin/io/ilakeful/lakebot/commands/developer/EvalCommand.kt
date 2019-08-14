/*
 * Copyright 2017-2019 (c) Alexander "ILakeful" Shevchenko
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

package io.ilakeful.lakebot.commands.developer

import io.ilakeful.lakebot.Immutable
import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.entities.extensions.*

import io.iskylake.weather.LakeWeather
import io.iskylake.weather.entities.Forecast

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.requests.RestAction

import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.script.jsr223.KotlinJsr223JvmLocalScriptEngine
import org.json.JSONObject

import javax.script.ScriptContext
import javax.script.ScriptEngineManager

class EvalCommand : Command {
    companion object {
        val PACKAGES = mutableListOf(
                "java.io",
                "java.lang.annotation",
                "java.lang.management",
                "java.text",
                "java.time",
                "java.time.temporal",
                "java.time.format",
                "java.util.function",
                "java.util.concurrent",
                "java.util.concurrent.atomic",
                "java.util.stream",
                "java.util.regex",
                "java.net",
                "javax.script",
                "net.dv8tion.jda.api",
                "net.dv8tion.jda.api.audit",
                "net.dv8tion.jda.api.entities",
                "net.dv8tion.jda.api.managers",
                "net.dv8tion.jda.api.utils",
                "net.dv8tion.jda.api.utils.cache",
                "net.dv8tion.jda.api.requests",
                "net.dv8tion.jda.api.requests.restaction",
                "net.dv8tion.jda.api.requests.restaction.order",
                "net.dv8tion.jda.api.requests.restaction.pagination",
                "net.dv8tion.jda.api.exceptions",
                "net.dv8tion.jda.api.hooks",
                "net.dv8tion.jda.api.events",
                "net.dv8tion.jda.api.events.message",
                "net.dv8tion.jda.api.events.message.react",
                "net.dv8tion.jda.api.entities",
                "net.dv8tion.jda.api.utils.cache",
                "io.ilakeful.lakebot",
                "io.ilakeful.lakebot.audio",
                "io.ilakeful.lakebot.commands",
                "io.ilakeful.lakebot.commands.music",
                "io.ilakeful.lakebot.commands.developer",
                "io.ilakeful.lakebot.commands.`fun`",
                "io.ilakeful.lakebot.commands.general",
                "io.ilakeful.lakebot.commands.info",
                "io.ilakeful.lakebot.commands.moderation",
                "io.ilakeful.lakebot.commands.utils",
                "io.ilakeful.lakebot.entities",
                "io.ilakeful.lakebot.entities.annotations",
                "io.ilakeful.lakebot.entities.applemusic",
                "io.ilakeful.lakebot.entities.extensions",
                "io.ilakeful.lakebot.entities.handlers",
                "io.ilakeful.lakebot.entities.pagination",
                "io.ilakeful.lakebot.utils",
                "io.iskylake.weather",
                "io.iskylake.weather.entities",
                "kotlinx.coroutines",
                "kotlin.reflect",
                "kotlin.reflect.jvm",
                "kotlin.reflect.full",
                "kotlin.system",
                "kotlin.io",
                "kotlin.random",
                "kotlin.concurrent",
                "kotlin.streams",
                "kotlin.properties",
                "org.slf4j",
                "org.json",
                "org.reflections"
        )
        val IMPORT_REGEX = "(\")?(import\\s+(\\w+|\\d+|_|\\*|\\.)+(?:\\s+(as)\\s+(\\w+|\\d+|_))?;?)(\")?".toRegex()
    }
    init {
        val os = System.getProperty("os.name")?.toLowerCase()
        if (os?.startsWith("win") == true) {
            setIdeaIoUseFallback()
        }
    }
    override val name = "eval"
    override val usage = fun(prefix: String) = "${super.usage(prefix)} <code>"
    override val aliases = listOf("evaluate", "exec", "execute", "script")
    override val description = "The command executing attached Kotlin script"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        val arguments = event.argsRaw
        if (arguments !== null) {
            val engine = ScriptEngineManager().getEngineByExtension("kts") as KotlinJsr223JvmLocalScriptEngine
            if (arguments.startsWith("```")) {
                val content = arguments.removeSurrounding("```")
                if (content.startsWith("kotlin", true)) {
                    kotlinEval(event, content.substring(6), engine)
                } else {
                    event.channel.sendFailure("That is an invalid language!").queue()
                }
            } else {
                kotlinEval(event, arguments, engine)
            }
        } else {
            event.channel.sendFailure("You haven't specified code!").queue()
        }
    }
    private fun kotlinEval(event: MessageReceivedEvent, content: String, engine: KotlinJsr223JvmLocalScriptEngine) {
        var script = content
        val commandImports = mutableListOf<String>()
        val khttpImports = mutableListOf<String>().apply {
            val packages = listOf("get", "post", "patch", "put", "delete")
            for (`package` in packages) {
                this += "import khttp.$`package` as http${`package`.capitalize()}"
            }
        }.toList()
        for (result in IMPORT_REGEX.findAll(content).filter { !it.value.contains("\"") }) {
            commandImports += result.value
            script = script.replace(result.value, "")
        }
        engine.put("event", event)
        engine.put("message", event.message)
        engine.put("textChannel", event.textChannel)
        engine.put("channel", event.channel)
        engine.put("author", event.author)
        engine.put("jda", event.jda)
        engine.put("guild", event.guild)
        engine.put("member", event.member)
        engine.put("selfUser", event.jda.selfUser)
        engine.put("selfMember", event.guild.selfMember)
        val scriptPrefix = buildString {
            for (import in PACKAGES) {
                appendln("import $import.*")
            }
            for (import in commandImports) {
                appendln(import)
            }
            for (import in khttpImports) {
                appendln(import)
            }
            for ((key, value) in engine.getBindings(ScriptContext.ENGINE_SCOPE)) {
                if ("." !in key) {
                    val name: String = value.javaClass.name
                    val bind = """val $key = bindings["$key"] as $name"""
                    appendln(bind)
                }
            }
        }
        try {
            val evaluated: Any? = engine.compileAndEval("$scriptPrefix\n$script", engine.context)
            if (evaluated !== null) {
                when (evaluated) {
                    is EmbedBuilder -> event.channel.sendMessage(evaluated.build()).queue()
                    is MessageEmbed -> event.channel.sendMessage(evaluated).queue()
                    is MessageBuilder -> event.channel.sendMessage(evaluated.build()).queue()
                    is Message -> event.channel.sendMessage(evaluated).queue()
                    is RestAction<*> -> evaluated.queue()
                    is Command -> event.channel.sendMessage("Command[${evaluated.fullName}, ${evaluated.id}]").queue()
                    is Array<*> -> event.channel.sendMessage(evaluated.contentToString()).queue()
                    is JSONObject -> event.channel.sendMessage(evaluated.toString(2)).queue()
                    is Forecast -> event.channel.sendMessage(evaluated.toString().removeContent(evaluated.api.key)).queue()
                    is LakeWeather -> event.channel.sendMessage(evaluated.toString().removeContent(evaluated.key)).queue()
                    else -> event.channel.sendMessage("$evaluated").queue()
                }
            }
        } catch (t: Throwable) {
            event.channel.sendEmbed {
                color { Immutable.FAILURE }
                author { "An error has occurred:" }
                description {
                    """```kotlin
                        |${t::class.qualifiedName ?: t.javaClass.name ?: "Unknown"}
                        |
                        |${t.message?.safeSubstring(0, 1536) ?: "None"}```""".trimMargin()
                }
                timestamp()
            }.queue()
        }
    }
}