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

import groovy.lang.GroovyShell

import io.iskylake.lakebot.Immutable
import io.iskylake.lakebot.commands.Command
import io.iskylake.lakebot.entities.extensions.*

import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.MessageBuilder
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageEmbed

import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.requests.RestAction

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
                "java.util",
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
                "net.dv8tion.jda.bot",
                "net.dv8tion.jda.bot.entities",
                "net.dv8tion.jda.bot.utils.cache",
                "net.dv8tion.jda.webhook",
                "io.iskylake.lakebot",
                "io.iskylake.lakebot.commands",
                "io.iskylake.lakebot.commands.developer",
                "io.iskylake.lakebot.commands.general",
                "io.iskylake.lakebot.entities",
                "io.iskylake.lakebot.entities.extensions",
                "io.iskylake.lakebot.entities.handlers",
                "io.iskylake.lakebot.utils",
                "org.slf4j",
                "org.json"
        )
        val KOTLIN_PACKAGES = PACKAGES - "java.util" + mutableListOf(
                "kotlinx.coroutines.experimental",
                "kotlin.reflect",
                "kotlin.reflect.jvm",
                "kotlin.reflect.full",
                "kotlin.system",
                "kotlin.io",
                "kotlin.concurrent",
                "kotlin.coroutines.experimental",
                "kotlin.streams",
                "kotlin.properties"
        )
        val IMPORT_REGEX = "(\")?(import\\s+(\\w+|\\d+|_|\\*|\\.)+(?:\\s+(as)\\s+(\\w+|\\d+|_))?;?)(\")?".toRegex()
    }
    override val name = "eval"
    override val usage = fun(prefix: String) = "${super.usage(prefix)} <code>"
    override val aliases = listOf("evaluate", "exec", "execute")
    override val description = "The command that evaluates Groovy/Kotlin script"
    override fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        val arguments = event.argsRaw
        if (arguments !== null) {
            if (arguments.startsWith("```")) {
                val content = arguments.removeSurrounding("```")
                when {
                    content.startsWith("kotlin", true) -> {
                        var scr = content.substring(6)
                        val commandImports = mutableListOf<String>()
                        for (result in IMPORT_REGEX.findAll(content).filter { !it.value.contains("\"") }) {
                            commandImports += result.value
                            scr = scr.replace(result.value, "")
                        }
                        val engine = ScriptEngineManager().getEngineByExtension("kts")
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
                            for (import in KOTLIN_PACKAGES) {
                                appendln("import $import.*")
                            }
                            for (import in commandImports) {
                                appendln(import)
                            }
                            for ((key, value) in engine.getBindings(ScriptContext.ENGINE_SCOPE)) {
                                val name: String = value::class.qualifiedName!!
                                val bind = """val $key = bindings["$key"] as $name"""
                                appendln(bind)
                            }
                        }
                        val script = "$scriptPrefix\n$scr"
                        Immutable.EVAL_THREAD_POOL.submit {
                            try {
                                val evaluated: Any? = engine.eval(script)
                                if (evaluated !== null) {
                                    when (evaluated) {
                                        is EmbedBuilder -> event.channel.sendMessage(evaluated.build()).queue()
                                        is MessageEmbed -> event.channel.sendMessage(evaluated).queue()
                                        is MessageBuilder -> event.channel.sendMessage(evaluated.build()).queue()
                                        is Message -> event.channel.sendMessage(evaluated).queue()
                                        is RestAction<*> -> evaluated.queue()
                                        is Array<*> -> event.channel.sendMessage(evaluated.contentToString()).queue()
                                        else -> event.channel.sendMessage("$evaluated").queue()
                                    }
                                }
                            } catch (t: Throwable) {
                                event.sendMessage(buildEmbed {
                                    color {
                                        Immutable.FAILURE
                                    }
                                    author {
                                        "An error has occured:"
                                    }
                                    description {
                                        """```kotlin
                                            |${t::class.qualifiedName ?: t.javaClass.name}
                                            |
                                            |${t.message?.safeSubstring(0, 1536) ?: "None"}```""".trimMargin()
                                    }
                                    timestamp()
                                }).queue()
                            }
                        }.get()
                    }
                    content.startsWith("groovy", true) -> {
                        val script = buildString {
                            for (import in PACKAGES) {
                                appendln("import $import.*")
                            }
                            appendln(content.substring(6))
                        }
                        val engine = GroovyShell()
                        engine.setVariable("event", event)
                        engine.setVariable("message", event.message)
                        engine.setVariable("textChannel", event.textChannel)
                        engine.setVariable("channel", event.channel)
                        engine.setVariable("author", event.author)
                        engine.setVariable("jda", event.jda)
                        engine.setVariable("guild", event.guild)
                        engine.setVariable("member", event.member)
                        engine.setVariable("selfUser", event.jda.selfUser)
                        engine.setVariable("selfMember", event.guild.selfMember)
                        Immutable.EVAL_THREAD_POOL.submit {
                            try {
                                val evaluated: Any? = engine.evaluate(script)
                                if (evaluated !== null) {
                                    when (evaluated) {
                                        is EmbedBuilder -> event.channel.sendMessage(evaluated.build()).queue()
                                        is MessageEmbed -> event.channel.sendMessage(evaluated).queue()
                                        is MessageBuilder -> event.channel.sendMessage(evaluated.build()).queue()
                                        is Message -> event.channel.sendMessage(evaluated).queue()
                                        is RestAction<*> -> evaluated.queue()
                                        is Array<*> -> event.channel.sendMessage(evaluated.contentToString()).queue()
                                        else -> event.channel.sendMessage("$evaluated").queue()
                                    }
                                }
                            } catch (t: Throwable) {
                                event.sendMessage(buildEmbed {
                                    color {
                                        Immutable.FAILURE
                                    }
                                    author {
                                        "An error has occured:"
                                    }
                                    description {
                                        """```groovy
                                            |${t::class.qualifiedName ?: t.javaClass.name}
                                            |
                                            |${t.message?.safeSubstring(0, 1536) ?: "None"}```""".trimMargin()
                                    }
                                    timestamp()
                                }).queue()
                            }
                        }.get()
                    }
                    else -> event.sendError("That's not a valid language!").queue()
                }
            } else {
                event.sendError("You didn't surround your script with code block!").queue()
            }
        } else {
            event.sendError("You specified no code!").queue()
        }
    }
}