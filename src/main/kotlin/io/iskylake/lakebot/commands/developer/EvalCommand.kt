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

package io.iskylake.lakebot.commands.developer

import io.iskylake.lakebot.Immutable
import io.iskylake.lakebot.commands.Command
import io.iskylake.lakebot.entities.extensions.*
import io.iskylake.weather.LakeWeather
import io.iskylake.weather.entities.Forecast

import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.MessageBuilder
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.requests.RestAction

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
                "net.dv8tion.jda.core",
                "net.dv8tion.jda.core.audit",
                "net.dv8tion.jda.core.entities",
                "net.dv8tion.jda.core.managers",
                "net.dv8tion.jda.core.utils",
                "net.dv8tion.jda.core.utils.cache",
                "net.dv8tion.jda.core.requests",
                "net.dv8tion.jda.core.requests.restaction",
                "net.dv8tion.jda.core.requests.restaction.order",
                "net.dv8tion.jda.core.requests.restaction.pagination",
                "net.dv8tion.jda.core.exceptions",
                "net.dv8tion.jda.core.hooks",
                "net.dv8tion.jda.core.events",
                "net.dv8tion.jda.core.events.message",
                "net.dv8tion.jda.core.events.message.react",
                "net.dv8tion.jda.bot",
                "net.dv8tion.jda.bot.entities",
                "net.dv8tion.jda.bot.utils.cache",
                "net.dv8tion.jda.webhook",
                "io.iskylake.lakebot",
                "io.iskylake.lakebot.audio",
                "io.iskylake.lakebot.commands",
                "io.iskylake.lakebot.commands.animals",
                "io.iskylake.lakebot.commands.audio",
                "io.iskylake.lakebot.commands.developer",
                "io.iskylake.lakebot.commands.`fun`",
                "io.iskylake.lakebot.commands.general",
                "io.iskylake.lakebot.commands.info",
                "io.iskylake.lakebot.commands.moderation",
                "io.iskylake.lakebot.commands.utils",
                "io.iskylake.lakebot.entities",
                "io.iskylake.lakebot.entities.annotations",
                "io.iskylake.lakebot.entities.extensions",
                "io.iskylake.lakebot.entities.handlers",
                "io.iskylake.lakebot.entities.pagination",
                "io.iskylake.lakebot.utils",
                "io.iskylake.weather",
                "io.iskylake.weather.entities",
                "kotlinx.coroutines.experimental",
                "kotlin.reflect",
                "kotlin.reflect.jvm",
                "kotlin.reflect.full",
                "kotlin.system",
                "kotlin.io",
                "kotlin.concurrent",
                "kotlin.coroutines.experimental",
                "kotlin.streams",
                "kotlin.properties",
                "org.slf4j",
                "org.json"
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
    override val aliases = listOf("evaluate", "exec", "execute")
    override val description = "The command that evaluates Kotlin script"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        val arguments = event.argsRaw
        if (arguments !== null) {
            val engine = ScriptEngineManager().getEngineByExtension("kts") as KotlinJsr223JvmLocalScriptEngine
            if (arguments.startsWith("```")) {
                val content = arguments.removeSurrounding("```")
                if (content.startsWith("kotlin", true)) {
                    kotlinEval(event, content.substring(6), engine)
                } else {
                    event.sendError("That's not a valid language!").queue()
                }
            } else {
                kotlinEval(event, arguments, engine)
            }
        } else {
            event.sendError("You specified no code!").queue()
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
            event.sendMessage(buildEmbed {
                color { Immutable.FAILURE }
                author { "An error has occured:" }
                description {
                    """```kotlin
                        |${t::class.qualifiedName ?: t.javaClass.name ?: "Unknown"}
                        |
                        |${t.message?.safeSubstring(0, 1536) ?: "None"}```""".trimMargin()
                }
                timestamp()
            }).queue()
        }
    }
}