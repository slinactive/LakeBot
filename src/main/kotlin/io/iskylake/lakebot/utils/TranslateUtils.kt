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

package io.iskylake.lakebot.utils

import khttp.get

import org.json.JSONArray

import java.net.URLEncoder

object TranslateUtils {
    val LANGUAGE_PAIRS = setOf(
            "af" to "Afrikaans",
            "sq" to "Albanian",
            "am" to "Amharic",
            "ar" to "Arabic",
            "hy" to "Armenian",
            "az" to "Azerbaijani",
            "eu" to "Basque",
            "be" to "Belarusian",
            "bn" to "Bengali",
            "bs" to "Bosnian",
            "bg" to "Bulgarian",
            "ca" to "Catalan",
            "ceb" to "Cebuano",
            "ny" to "Chichewa",
            "zh-cn" to "Chinese",
            "co" to "Corsican",
            "hr" to "Croatian",
            "cs" to "Czech",
            "da" to "Danish",
            "nl" to "Dutch",
            "en" to "English",
            "eo" to "Esperanto",
            "et" to "Estonian",
            "tl" to "Filipino",
            "fi" to "Finnish",
            "fr" to "French",
            "fy" to "Frisian",
            "gl" to "Galician",
            "ka" to "Georgian",
            "de" to "German",
            "el" to "Greek",
            "gu" to "Gujarati",
            "ht" to "Haitian",
            "ha" to "Hausa",
            "haw" to "Hawaiian",
            "iw" to "Hebrew",
            "hi" to "Hindi",
            "hmn" to "Hmong",
            "hu" to "Hungarian",
            "is" to "Icelandic",
            "ig" to "Igbo",
            "id" to "Indonesian",
            "ga" to "Irish",
            "it" to "Italian",
            "ja" to "Japanese",
            "jw" to "Javanese",
            "kn" to "Kannada",
            "kk" to "Kazakh",
            "km" to "Khmer",
            "ko" to "Korean",
            "ku" to "Kurdish",
            "ky" to "Kyrgyz",
            "lo" to "Lao",
            "la" to "Latin",
            "lv" to "Latvian",
            "lt" to "Lithuanian",
            "lb" to "Luxembourgish",
            "mk" to "Macedonian",
            "mg" to "Malagasy",
            "ms" to "Malay",
            "ml" to "Malayalam",
            "mt" to "Maltese",
            "mi" to "Maori",
            "mr" to "Marathi",
            "mn" to "Mongolian",
            "my" to "Myanmar",
            "ne" to "Nepali",
            "no" to "Norwegian",
            "ps" to "Pashto",
            "fa" to "Persian",
            "pl" to "Polish",
            "pt" to "Portuguese",
            "ma" to "Punjabi",
            "ro" to "Romanian",
            "ru" to "Russian",
            "sm" to "Samoan",
            "gd" to "Gaelic",
            "sr" to "Serbian",
            "st" to "Sesotho",
            "sn" to "Shona",
            "sd" to "Sindhi",
            "si" to "Sinhala",
            "sk" to "Slovak",
            "sl" to "Slovenian",
            "so" to "Somali",
            "es" to "Spanish",
            "su" to "Sundanese",
            "sw" to "Swahili",
            "sv" to "Swedish",
            "tg" to "Tajik",
            "ta" to "Tamil",
            "te" to "Telugu",
            "th" to "Thai",
            "tr" to "Turkish",
            "uk" to "Ukrainian",
            "ur" to "Urdu",
            "uz" to "Uzbek",
            "vi" to "Vietnamese",
            "cy" to "Welsh",
            "xh" to "Xhosa",
            "yi" to "Yiddish",
            "yo" to "Yoruba",
            "zu" to "Zulu"
    )
    fun getLanguageCode(name: String) = LANGUAGE_PAIRS.firstOrNull { it.second.equals(name, true) }?.first ?: throw IllegalArgumentException("Provided language is invalid!")
    fun getLanguageName(code: String) = LANGUAGE_PAIRS.firstOrNull { it.first.equals(code, true) }?.second ?: throw IllegalArgumentException("Provided code is invalid!")
    fun rawRequest(code: String, text: String): JSONArray {
        val url = "https://translate.googleapis.com/translate_a/single"
        val params = "client=gtx&sl=auto&t=$code&hl=$code&dt=t&dt=at&dt=bd&dt=ex&dt=ld&dt=md&dt=qca&dt=rw&dt=rm&dt=ss&ie=UTF-8&oe=UTF-8&otf=1&ssel=0&tsel=0&kc=7&q=${URLEncoder.encode(text, "UTF-8")}"
        val response = get("$url?$params", headers = mapOf())
        return response.jsonArray
    }
    fun translate(to: String, text: String): String? = try {
        val code = getLanguageCode(to)
        val jsonArray = rawRequest(code, text)
        val first = jsonArray.getJSONArray(0)
        first.remove(first.count() - 1)
        buildString {
            first.forEach {
                val array = it as JSONArray
                append(array.getString(0))
            }
        }
    } catch (e: Exception) {
        null
    }
    fun detect(text: String): String {
        val jsonArray = rawRequest("", text)
        return jsonArray.getString(2)
    }
}