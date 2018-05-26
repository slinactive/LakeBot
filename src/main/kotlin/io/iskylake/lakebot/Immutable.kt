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

import org.slf4j.LoggerFactory

import java.awt.Color

object Immutable {
    const val DEFAULT_PREFIX = "lb!"
    const val VERSION = "0.3.2"
    const val PERMISSIONS = 2146958591L
    const val GITHUB_REPOSITORY = "https://github.com/ISkylakeS/LakeBot"
    const val SUPPORT_INVITE = "https://discord.gg/QsTevwF"
    const val USER_AGENT = "Mozilla/5.0 (Windows NT 6.3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3416.0 Safari/537.36"
    val LOGGER = LoggerFactory.getLogger("LakeBot")!!
    val BOT_TOKEN = System.getenv("BOT_TOKEN")!!
    val SUCCESS = Color(232, 66, 102)
    val FAILURE = Color(239, 67, 63)
    val CONFIRMATION = Color(118, 255, 3)
    val DEVELOPERS = longArrayOf(337643430903676928)
}