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
 *  limitations under the License.
 */

package io.ilakeful.lakebot.utils

import io.ilakeful.lakebot.Immutable

import khttp.post

import org.json.JSONObject

object PerspectiveApiUtils {
    private val KEY = Immutable.PERSPECTIVE_API_KEY
    private const val BASE_URL = "https://commentanalyzer.googleapis.com/v1alpha1"
    private const val ENDPOINT_URL = "/comments:analyze"
    fun requestBody(textToCheck: String, mode: String = "SEXUALLY_EXPLICIT") = JSONObject().apply {
        put("comment", JSONObject().put("text", textToCheck))
        put("requestedAttributes", JSONObject().put(mode, JSONObject()))
    }
    fun request(data: JSONObject) = post(
            url = "$BASE_URL$ENDPOINT_URL?key=$KEY",
            headers = emptyMap(),
            json = data
    )
    @Throws(IllegalArgumentException::class)
    fun probability(textToCheck: String, mode: String = "SEXUALLY_EXPLICIT") = request(requestBody(textToCheck, mode))
            .let {
                if (it.statusCode != 200) {
                    val json = it.jsonObject
                    throw IllegalArgumentException(if (json.has("error")) {
                        json.getJSONObject("error").optString("message", "Unknown API error!")
                    } else "Unknown API error!")
                } else {
                    it.jsonObject
                            .getJSONObject("attributeScores")
                            .getJSONObject(mode)
                            .getJSONObject("summaryScore")
                            .getFloat("value")
                }
            }
}