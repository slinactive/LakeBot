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

package io.iskylake.lakebot.commands.utils

import io.iskylake.lakebot.commands.Command
import io.iskylake.lakebot.entities.extensions.*
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.math.BigDecimal
import kotlin.math.*

/*fun main(args: Array<String>) {
    while (true) {
        val input = readLine() ?: "2 + 2"
        try {
            val result = CalculatorCommand().eval(input)
            val toBePrinted = if (round(result.toDouble()) == result.toDouble()) {
                "${result.toInt()}"
            } else result.toString()
            println(toBePrinted)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}*/
class CalculatorCommand : Command {
    override val name = "calculator"
    override val description = "N/A"
    override val cooldown = 2L
    override val usage = fun(prefix: String) = "${super.usage(prefix)} <expression>"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        val arguments = event.argsRaw
        if (arguments !== null) {
            try {
                val evaluated = eval(arguments)
                val result = if (evaluated.toDouble() % 1 == 0.0) evaluated.toPlainString() else "$evaluated"
                event.channel.sendMessage(result).queue()
            } catch (e: Exception) {
                event.channel.sendMessage("$e").queue()
            }
        } else {
            event.sendError("You specified no content!").queue()
        }
    }
    fun eval(expression: String): BigDecimal {
        val result = object {
            var position = -1
            var char = Character.MIN_VALUE
            fun nextChar() {
                char = if (++position < expression.length) expression[position] else (-1).toChar()
            }
            fun eat(charToEat: Char): Boolean {
                while (char.isWhitespace()) {
                    nextChar()
                }
                return if (char == charToEat) {
                    nextChar()
                    true
                } else {
                    false
                }
            }
            fun parse(): Double {
                nextChar()
                val parsed = parseExpression()
                return if (position < expression.length) throw ArithmeticException("Unexpected: $char") else parsed
            }
            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    when {
                        eat('+') -> x += parseTerm()
                        eat('-') -> x -= parseTerm()
                        else -> return x
                    }
                }
            }
            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    when {
                        eat('*') || eat('×') -> x *= parseFactor()
                        eat('/') || eat('÷') -> x /= parseFactor()
                        else -> return x
                    }
                }
            }
            fun parseFactor(): Double {
                return when {
                    eat('+') -> parseFactor()
                    eat('-') -> -parseFactor()
                    else -> {
                        var x: Double
                        val startPos = position
                        if (eat('(')) {
                            x = parseExpression()
                            eat(')')
                        } else if (char in '0'..'9' || char == '.') {
                            while (char in '0'..'9' || char == '.') {
                                nextChar()
                            }
                            x = expression.substring(startPos, position).toDouble()
                        } else if (char in 'a'..'z') {
                            while (char in 'a'..'z') {
                                nextChar()
                            }
                            val func = expression.substring(startPos, position)
                            val parseFactor = parseFactor()
                            x = when (func) {
                                "sqrt" -> sqrt(parseFactor)
                                "sin" -> sin(Math.toRadians(parseFactor))
                                "cos" -> cos(Math.toRadians(parseFactor))
                                "tan" -> tan(Math.toRadians(parseFactor))
                                else -> throw ArithmeticException("Unknown function: $func")
                            }
                        } else {
                            throw ArithmeticException("Unexpected: $char")
                        }
                        if (eat('^')) {
                            x = x.pow(parseFactor())
                        }
                        x
                    }
                }
            }
        }
        return result.parse().toBigDecimal()
    }
}