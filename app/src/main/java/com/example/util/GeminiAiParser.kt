package com.example.util

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class AiParsedResult(
    val title: String,
    val jalaliDate: JalaliDate,
    val hour: Int,
    val minute: Int,
    val prio: String, // "urgent", "high", "normal", "low"
    val tags: List<String>,
    val repeat: String // "none", "daily", "weekly"
)

object GeminiAiParser {
    private const val TAG = "GeminiAiParser"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun parsePrompt(
        rawText: String,
        today: JalaliDate = JalaliCalendar.today(),
        customApiKey: String? = null
    ): AiParsedResult = withContext(Dispatchers.IO) {
        val apiKey = customApiKey?.trim()?.ifBlank { null }
            ?: (try { BuildConfig.GEMINI_API_KEY } catch (e: Throwable) { "" }).trim()

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val aiResult = callGeminiRestApi(rawText, today, apiKey)
                if (aiResult != null) return@withContext aiResult
            } catch (e: Exception) {
                Log.w(TAG, "Gemini API call failed, falling back to local heuristic parser: ${e.message}")
            }
        }

        // Fallback to sophisticated local Persian text parser
        return@withContext parseLocally(rawText, today)
    }

    private fun callGeminiRestApi(
        rawText: String,
        today: JalaliDate,
        apiKey: String
    ): AiParsedResult? {
        val systemPrompt = """
            تو یک دستیار هوشمند، برنامه‌ریز و تحلیل‌گر تقویم شمسی برای اپ یادآور هستی.
            امروز ${today.year}/${today.month}/${today.day} (شمسی) است.
            متن یا صوت پیاده‌شده کاربر را تحلیل کن و یک شیء JSON با فیلدهای زیر تولید کن:
            1. title: عنوان خلاصه، واضح و معنادار بدون کلمات زمان و هشتگ.
            2. jy: سال شمسی (عدد)
            3. jm: ماه شمسی 1 تا 12 (عدد)
            4. jd: روز شمسی 1 تا 31 (عدد)
            5. time: ساعت به فرمت "HH:MM" مثلا "09:30" یا "18:00". اگر نگفت، ساعت کاری مناسب پیشنهاد بده مثلا "09:00" یا "17:00".
            6. prio: اولویت کار: "urgent" (فوری)، "high" (مهم)، "normal" (عادی)، "low" (کم).
            7. tags: آرایه‌ای از 1 تا 3 برچسب فارسی مناسب مرتبط با موضوع (مثلاً ["کار"، "پروژه"] یا ["خرید"، "خونه"] یا ["شخصی"] یا ["سلامت"]).
            8. repeat: تکرار کار: "none" یا "daily" یا "weekly".
            
            فقط و فقط یک شیء JSON استاندارد معتبر برگردان.
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemPrompt) })
                })
            })
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", rawText) })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.2)
            })
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash-lite:generateContent?key=$apiKey")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            val errBody = response.body?.string() ?: ""
            Log.e(TAG, "Gemini API error ${response.code}: $errBody")
            return null
        }

        val respText = response.body?.string() ?: return null
        val respJson = JSONObject(respText)
        val candidates = respJson.optJSONArray("candidates") ?: return null
        val firstCandidate = candidates.optJSONObject(0) ?: return null
        val content = firstCandidate.optJSONObject("content") ?: return null
        val parts = content.optJSONArray("parts") ?: return null
        val outputText = parts.optJSONObject(0)?.optString("text") ?: return null

        val cleanJson = outputText.replace("```json", "").replace("```", "").trim()
        val parsed = JSONObject(cleanJson)

        val title = parsed.optString("title", rawText).trim().ifBlank { rawText }
        val jy = parsed.optInt("jy", today.year)
        val jm = parsed.optInt("jm", today.month)
        val jd = parsed.optInt("jd", today.day)
        val timeStr = parsed.optString("time", "09:00")
        val prio = parsed.optString("prio", "normal").lowercase()
        val repeat = parsed.optString("repeat", "none").lowercase()

        val tagsList = mutableListOf<String>()
        val tagsArr = parsed.optJSONArray("tags")
        if (tagsArr != null) {
            for (i in 0 until tagsArr.length()) {
                val t = tagsArr.optString(i).trim().replace("#", "")
                if (t.isNotBlank()) tagsList.add(t)
            }
        }
        if (tagsList.isEmpty()) tagsList.add("شخصی")

        val (h, m) = parseHourMinute(timeStr)

        return AiParsedResult(
            title = title,
            jalaliDate = JalaliDate(jy, jm, jd),
            hour = h,
            minute = m,
            prio = if (prio in listOf("urgent", "high", "normal", "low")) prio else "normal",
            tags = tagsList,
            repeat = if (repeat in listOf("daily", "weekly")) repeat else "none"
        )
    }

    /** Detect an explicit Persian weekday. Index: Saturday=0 ... Friday=6. */
    fun detectWeekday(rawText: String): Int? {
        val normalized = rawText.replace("‌", "").replace("ي", "ی").replace("ك", "ک")
        return when {
            normalized.contains("یکشنبه") || normalized.contains("یکشنب") -> 1
            normalized.contains("دوشنبه") -> 2
            normalized.contains("سهشنبه") -> 3
            normalized.contains("چهارشنبه") -> 4
            normalized.contains("پنجشنبه") -> 5
            normalized.contains("جمعه") -> 6
            normalized.contains("شنبه") -> 0
            else -> null
        }
    }

    /** Natural-language phrases meaning the user wants a matrix reminder without a phone alarm. */
    fun requestsNoAlarm(rawText: String): Boolean {
        val normalized = rawText.replace("‌", " ").lowercase()
        return listOf(
            "بدون آلارم",
            "بدون زنگ",
            "لازم نیست زنگ",
            "لازم نیست آلارم",
            "زنگ نمی‌خوام",
            "آلارم نمی‌خوام",
            "زنگ نزن",
            "آلارم نذار",
            "آلارم نگذار",
            "فقط در ماتریس",
            "فقط توی ماتریس",
            "فقط در آیزنهاور",
            "فقط توی آیزنهاور",
            "در ماتریس بیاد",
            "تو ماتریس بیاد",
            "در ماتریس باشد",
            "توی ماتریس باشه"
        ).any { normalized.contains(it) }
    }

    /**
     * Local heuristic Persian parser (runs instantly even offline or without API key)
     */
    fun parseLocally(raw: String, today: JalaliDate = JalaliCalendar.today()): AiParsedResult {
        var text = " $raw "
        val tags = mutableListOf<String>()

        // Extract #hashtags
        val hashMatcher = Pattern.compile("#([^\\s#]+)").matcher(text)
        while (hashMatcher.find()) {
            hashMatcher.group(1)?.let { tags.add(it) }
        }
        text = text.replace(Regex("#[^\\s#]+"), " ")

        // Priority extraction
        var prio = "normal"
        when {
            text.contains("!!!") || text.contains("فوری") || text.contains("اضطراری") -> {
                prio = "urgent"
                text = text.replace("!!!", " ").replace("فوری", " ").replace("اضطراری", " ")
            }
            text.contains("!!") || text.contains("مهم") -> {
                prio = "high"
                text = text.replace("!!", " ").replace("مهم", " ")
            }
            text.contains("!") -> {
                prio = "normal"
                text = text.replace("!", " ")
            }
            text.contains("کم اهمیت") || text.contains("بعدا") || text.contains("بعداً") -> {
                prio = "low"
                text = text.replace("کم اهمیت", " ").replace("بعدا", " ").replace("بعداً", " ")
            }
        }

        // Weekday extraction. An explicit weekday means a weekly reminder even if
        // the user did not literally say "هر هفته".
        val explicitDow = detectWeekday(text)

        // Repeat extraction
        var repeat = "none"
        if (text.contains("هر روز") || text.contains("روزانه")) {
            repeat = "daily"
            text = text.replace("هر روز", " ").replace("روزانه", " ")
        } else if (text.contains("هر هفته") || text.contains("هفتگی")) {
            repeat = "weekly"
            text = text.replace("هر هفته", " ").replace("هفتگی", " ")
        }

        // Date extraction
        var targetDate = today
        when {
            text.contains("پس‌فردا") || text.contains("پس فردا") -> {
                targetDate = JalaliCalendar.addDays(today, 2)
                text = text.replace("پس‌فردا", " ").replace("پس فردا", " ")
            }
            text.contains("فردا") -> {
                targetDate = JalaliCalendar.addDays(today, 1)
                text = text.replace("فردا", " ")
            }
            text.contains("امروز") -> {
                targetDate = today
                text = text.replace("امروز", " ")
            }
            text.contains("هفته بعد") || text.contains("هفته آینده") -> {
                targetDate = JalaliCalendar.addDays(today, 7)
                text = text.replace("هفته بعد", " ").replace("هفته آینده", " ")
            }
        }

        // Time extraction (e.g. ساعت ۹:۳۰ or ۹:۳۰ or ساعت ۵ عصر)
        var hour = 9
        var minute = 0

        val timeRegex = Pattern.compile("(\\d{1,2})[:：](\\d{2})").matcher(text)
        if (timeRegex.find()) {
            hour = timeRegex.group(1)?.toIntOrNull() ?: 9
            minute = timeRegex.group(2)?.toIntOrNull() ?: 0
            text = text.replace(timeRegex.group(0) ?: "", " ")
        } else {
            val hourOnlyRegex = Pattern.compile("(?:ساعت)\\s*(\\d{1,2})").matcher(text)
            if (hourOnlyRegex.find()) {
                hour = hourOnlyRegex.group(1)?.toIntOrNull() ?: 9
                text = text.replace(hourOnlyRegex.group(0) ?: "", " ")
            }
        }

        if ((text.contains("عصر") || text.contains("بعدازظهر") || text.contains("شب")) && hour < 12) {
            hour += 12
        }

        // Clean leftover words
        val cleanTitle = text.replace(Regex("\\s+"), " ")
            .replace("ساعت", "")
            .replace("عصر", "")
            .replace("صبح", "")
            .replace("شب", "")
            .replace("شنبه", "")
            .replace("یک‌شنبه", "").replace("یکشنبه", "")
            .replace("دوشنبه", "")
            .replace("سه‌شنبه", "").replace("سهشنبه", "")
            .replace("چهارشنبه", "")
            .replace("پنج‌شنبه", "").replace("پنجشنبه", "")
            .replace("جمعه", "")
            .replace("بدون آلارم", "")
            .replace("بدون زنگ", "")
            .replace("فقط در ماتریس", "")
            .replace("فقط توی ماتریس", "")
            .replace("فقط در آیزنهاور", "")
            .replace("فقط توی آیزنهاور", "")
            .replace("در ماتریس بیاد", "")
            .replace("توی ماتریس باشه", "")
            .trim()
            .ifBlank { raw.trim() }

        if (tags.isEmpty()) {
            tags.add("شخصی")
        }

        if (explicitDow != null) {
            targetDate = JalaliCalendar.nextOrSameDayOfWeek(today, explicitDow)
            repeat = "weekly"
        }

        return AiParsedResult(
            title = cleanTitle,
            jalaliDate = targetDate,
            hour = hour % 24,
            minute = minute % 60,
            prio = prio,
            tags = tags,
            repeat = repeat
        )
    }

    private fun parseHourMinute(timeStr: String): Pair<Int, Int> {
        val parts = timeStr.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return Pair(h % 24, m % 60)
    }
}
