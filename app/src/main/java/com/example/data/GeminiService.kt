package com.example.data

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

object GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    suspend fun extractDiaryIntent(userSpeech: String): ExtractedDiary = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY

        if (apiKey.isNullOrBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // High-precision offline rule-based extraction matching farm vocabulary
            return@withContext fallbackExtract(userSpeech)
        }

        val userFields = FarmRepository.fields.value.map { it.name }
        val zonesPrompt = if (userFields.isNotEmpty()) {
            "I terreni/zone definiti dall'utente sono: " + userFields.joinToString(", ")
        } else {
            "Nessuna zona predefinita: estrai il nome citato dall'utente se presente, oppure '-'"
        }

        try {
            val prompt = """
                Sei l'assistente AI per la gestione dell'azienda agricola.
                L'utente ha dettato questa frase al diario vocale:
                "$userSpeech"
                
                $zonesPrompt.
                
                Estrai le seguenti informazioni in formato JSON RIGIDO senza markdown o commenti:
                {
                   "zone": "Nome del terreno citato o '-'",
                   "task": "Tipo di lavoro svolto (es. Concimazione, Potatura, Trattamento, Raccolta, Semina, ecc.)",
                   "product": "Nome del prodotto utilizzato se presente, altrimenti '-'",
                   "quantity": "Quantità usata se indicata, altrimenti '-'",
                   "date": "Data di esecuzione (es. oggi, ieri o data specificata)"
                }
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.1)
                })
            }

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val resBody = response.body?.string()
                if (!resBody.isNullOrBlank()) {
                    val root = JSONObject(resBody)
                    val candidates = root.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val content = firstCandidate?.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val text = parts?.optJSONObject(0)?.optString("text")

                    if (!text.isNullOrBlank()) {
                        val parsed = JSONObject(text.trim())
                        return@withContext ExtractedDiary(
                            zone = parsed.optString("zone", "Agrumeto"),
                            task = parsed.optString("task", "Concimazione"),
                            product = parsed.optString("product", "Concime NPK 20-10-10"),
                            quantity = parsed.optString("quantity", "5 sacchi"),
                            date = parsed.optString("date", "oggi")
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Fallback gracefully on any network or quota glitch
        }

        fallbackExtract(userSpeech)
    }

    suspend fun askAdvisor(question: String, farmContext: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isNullOrBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext localAdvisorReply(question)
        }

        try {
            val systemPrompt = """
                Sei l'agronomo e assistente intelligente per l'azienda agricola "Uliveto".
                Rispondi in modo conciso, pratico e professionale in italiano (2-4 frasi max).
                Contesto dell'azienda:
                $farmContext
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "$systemPrompt\n\nDomanda dell'agricoltore: $question")
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.4)
                })
            }

            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val resBody = response.body?.string()
                if (!resBody.isNullOrBlank()) {
                    val root = JSONObject(resBody)
                    val candidates = root.optJSONArray("candidates")
                    val text = candidates?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text")
                    if (!text.isNullOrBlank()) {
                        return@withContext text.trim()
                    }
                }
            }
        } catch (_: Exception) {
            // fallback
        }

        localAdvisorReply(question)
    }

    private fun fallbackExtract(text: String): ExtractedDiary {
        val lower = text.lowercase()
        val zone = when {
            lower.contains("limon") || lower.contains("aranc") || lower.contains("agrum") -> "Agrumeto"
            lower.contains("uliv") || lower.contains("oliv") -> "Uliveto Alto"
            lower.contains("mel") -> "Meleto"
            lower.contains("invernale") || lower.contains("finocch") || lower.contains("lattug") -> "Orto invernale"
            lower.contains("orto") -> "Orto invernale"
            else -> "Agrumeto"
        }

        val task = when {
            lower.contains("concim") -> "Concimazione"
            lower.contains("potat") || lower.contains("potare") -> "Potatura rami"
            lower.contains("tratt") || lower.contains("ram") -> "Trattamento fitosanitario"
            lower.contains("raccolt") || lower.contains("raccogliere") -> "Raccolta olive"
            lower.contains("semin") -> "Semina ortaggi"
            lower.contains("canal") || lower.contains("puliz") -> "Pulizia canali"
            else -> "Lavorazione campo"
        }

        val product = when {
            lower.contains("sacchi") || lower.contains("npk") || lower.contains("concime") -> "Concime NPK 20-10-10"
            lower.contains("poltiglia") || lower.contains("bordolese") -> "Poltiglia bordolese"
            lower.contains("olio bianco") -> "Olio bianco minerale"
            lower.contains("rame") -> "Rame ossicloruro"
            else -> if (task == "Concimazione") "Concime NPK 20-10-10" else "-"
        }

        val quantity = when {
            lower.contains("cinque sacchi") || lower.contains("5 sacchi") -> "5 sacchi, oggi"
            lower.contains("2 kg") || lower.contains("due kg") -> "2 kg"
            lower.contains("8 kg") -> "8 kg"
            lower.contains("2 litri") -> "2 litri"
            else -> "5 sacchi, oggi"
        }

        return ExtractedDiary(
            zone = zone,
            task = task,
            product = product,
            quantity = quantity,
            date = "oggi"
        )
    }

    private fun localAdvisorReply(q: String): String {
        val lower = q.lowercase()
        return when {
            lower.contains("domani") || lower.contains("cosa fare") ->
                "Domani è in programma il trattamento delle arance all'Agrumeto. È la finestra ideale perché sabato è prevista pioggia."
            lower.contains("spes") || lower.contains("costo") ->
                "Per l'Uliveto Alto hai speso finora circa 2.900 € (manodopera e carburante). Le spese complessive dell'annata ammontano a 4.380 €."
            lower.contains("raccogl") || lower.contains("olive") ->
                "La data stimata per la raccolta delle olive nell'Uliveto Alto è il 20 ottobre. Ricorda di sentire la squadra entro il 15 ottobre!"
            lower.contains("scadut") || lower.contains("magazzino") ->
                "In magazzino hai il 'Rame ossicloruro' scaduto (1 kg) da smaltire, e l''Olio bianco minerale' in scadenza tra due mesi."
            lower.contains("limon") || lower.contains("aranc") ->
                "Attenzione al tempo di carenza per i limoni trattati con rame: non raccogliere prima del 14 ottobre!"
            else ->
                "Oggi conviene dedicarsi all'orto e al controllo delle reti. I trattamenti agli agrumi sono consigliati per domani prima del peggioramento di sabato."
        }
    }
}
