package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

data class DailyForecast(
    val dateStr: String,
    val dayOfWeek: String,
    val tempMax: Double,
    val tempMin: Double,
    val precipitationSum: Double,
    val precipitationProbability: Int,
    val weatherCode: Int,
    val conditionDescription: String
)

data class AgronomicWeatherCondition(
    val temperature: Double = 21.0,
    val apparentTemperature: Double = 21.0,
    val humidity: Int = 60,
    val windSpeedKmH: Double = 8.0,
    val precipitationMm: Double = 0.0,
    val weatherCode: Int = 0,
    val conditionTitle: String = "Sereno",
    val locationName: String = "Uliveto",
    val dailyForecasts: List<DailyForecast> = emptyList(),
    val adviceList: List<AgronomicAdvice> = emptyList(),
    val lastUpdated: String = ""
)

data class AgronomicAdvice(
    val type: AdviceType,
    val title: String,
    val description: String,
    val isAlert: Boolean = false
)

enum class AdviceType {
    TRATTAMENTI,
    IRRIGAZIONE,
    VENTO,
    MALATTIE,
    LAVORAZIONI
}

object WeatherService {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val _weatherState = MutableStateFlow(getDefaultCondition())
    val weatherState: StateFlow<AgronomicWeatherCondition> = _weatherState.asStateFlow()

    private var lastFetchTimeMs = 0L

    suspend fun refreshWeather(latitude: Double? = null, longitude: Double? = null, force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && (now - lastFetchTimeMs < 15 * 60 * 1000) && _weatherState.value.dailyForecasts.isNotEmpty()) {
            return
        }

        // Determine coordinates: provided, or from property, or typical Italian Mediterranean olive location
        val (lat, lon) = when {
            latitude != null && longitude != null -> Pair(latitude, longitude)
            FarmRepository.property.value?.polygon?.isNotEmpty() == true -> {
                val poly = FarmRepository.property.value!!.polygon
                Pair(poly.map { it.latitude }.average(), poly.map { it.longitude }.average())
            }
            FarmRepository.fields.value.isNotEmpty() && FarmRepository.fields.value.first().polygon.isNotEmpty() -> {
                val poly = FarmRepository.fields.value.first().polygon
                Pair(poly.map { it.latitude }.average(), poly.map { it.longitude }.average())
            }
            else -> Pair(39.2238, 9.1217) // Default Mediterranean farm location (Sardegna/Calabria/Puglia)
        }

        withContext(Dispatchers.IO) {
            try {
                val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                        "&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m" +
                        "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,precipitation_probability_max" +
                        "&timezone=auto&forecast_days=6"

                val request = Request.Builder().url(url).build()
                val response = httpClient.newCall(request).execute()

                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrBlank()) {
                        val parsed = parseOpenMeteoResponse(body)
                        _weatherState.value = parsed
                        lastFetchTimeMs = now
                        return@withContext
                    }
                }
            } catch (e: Exception) {
                // If offline or network error, generate intelligent local seasonal condition
                if (_weatherState.value.dailyForecasts.isEmpty()) {
                    _weatherState.value = getDefaultCondition()
                }
            }
        }
    }

    private fun parseOpenMeteoResponse(jsonStr: String): AgronomicWeatherCondition {
        val root = JSONObject(jsonStr)
        val currentObj = root.optJSONObject("current")
        val dailyObj = root.optJSONObject("daily")

        val temp = currentObj?.optDouble("temperature_2m", 21.0) ?: 21.0
        val apparentTemp = currentObj?.optDouble("apparent_temperature", temp) ?: temp
        val humidity = currentObj?.optInt("relative_humidity_2m", 60) ?: 60
        val precip = currentObj?.optDouble("precipitation", 0.0) ?: 0.0
        val weatherCode = currentObj?.optInt("weather_code", 0) ?: 0
        val windSpeed = currentObj?.optDouble("wind_speed_10m", 8.0) ?: 8.0

        val forecasts = mutableListOf<DailyForecast>()
        if (dailyObj != null) {
            val times = dailyObj.optJSONArray("time")
            val maxTemps = dailyObj.optJSONArray("temperature_2m_max")
            val minTemps = dailyObj.optJSONArray("temperature_2m_min")
            val rainSums = dailyObj.optJSONArray("precipitation_sum")
            val rainProbs = dailyObj.optJSONArray("precipitation_probability_max")
            val codes = dailyObj.optJSONArray("weather_code")

            val count = times?.length() ?: 0
            val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val sdfDayName = SimpleDateFormat("EEEE", Locale.ITALIAN)
            val sdfDisplay = SimpleDateFormat("dd/MM", Locale.ITALIAN)

            for (i in 0 until count) {
                val dateRaw = times?.optString(i, "") ?: ""
                val tMax = maxTemps?.optDouble(i, temp + 2.0) ?: (temp + 2.0)
                val tMin = minTemps?.optDouble(i, temp - 5.0) ?: (temp - 5.0)
                val rSum = rainSums?.optDouble(i, 0.0) ?: 0.0
                val rProb = rainProbs?.optInt(i, 0) ?: 0
                val code = codes?.optInt(i, 0) ?: 0

                var dayName = "Giorno"
                var displayDate = dateRaw
                try {
                    val d = sdfInput.parse(dateRaw)
                    if (d != null) {
                        dayName = if (i == 0) "Oggi" else if (i == 1) "Domani" else sdfDayName.format(d).replaceFirstChar { it.uppercase() }
                        displayDate = sdfDisplay.format(d)
                    }
                } catch (_: Exception) {}

                forecasts.add(
                    DailyForecast(
                        dateStr = displayDate,
                        dayOfWeek = dayName,
                        tempMax = tMax,
                        tempMin = tMin,
                        precipitationSum = rSum,
                        precipitationProbability = rProb,
                        weatherCode = code,
                        conditionDescription = getWeatherCodeDescription(code)
                    )
                )
            }
        }

        val advice = generateAgronomicAdvice(temp, humidity, precip, windSpeed, forecasts)
        val nowFormatted = SimpleDateFormat("HH:mm", Locale.ITALIAN).format(Date())

        return AgronomicWeatherCondition(
            temperature = temp,
            apparentTemperature = apparentTemp,
            humidity = humidity,
            windSpeedKmH = windSpeed,
            precipitationMm = precip,
            weatherCode = weatherCode,
            conditionTitle = getWeatherCodeDescription(weatherCode),
            locationName = FarmRepository.property.value?.name ?: "Uliveto",
            dailyForecasts = forecasts,
            adviceList = advice,
            lastUpdated = nowFormatted
        )
    }

    fun generateAgronomicAdvice(
        temp: Double,
        humidity: Int,
        precip: Double,
        wind: Double,
        forecasts: List<DailyForecast>
    ): List<AgronomicAdvice> {
        val list = mutableListOf<AgronomicAdvice>()

        // 1. Rain & Treatments Analysis (Next 48h)
        val rainTomorrow = forecasts.getOrNull(1)?.precipitationSum ?: 0.0
        val rainProbTomorrow = forecasts.getOrNull(1)?.precipitationProbability ?: 0
        val rainToday = precip > 0.0 || (forecasts.getOrNull(0)?.precipitationSum ?: 0.0) > 1.0

        if (rainToday || rainTomorrow > 1.5 || rainProbTomorrow >= 60) {
            list.add(
                AgronomicAdvice(
                    type = AdviceType.TRATTAMENTI,
                    title = "⚠️ Rischio pioggia: evitare trattamenti",
                    description = "Pioggia prevista (${if (rainToday) "in corso/oggi" else "domani $rainTomorrow mm"}). I fitofarmaci e rameici verrebbero dilavati: rimandare a cielo sereno.",
                    isAlert = true
                )
            )
        } else {
            list.add(
                AgronomicAdvice(
                    type = AdviceType.TRATTAMENTI,
                    title = "✓ Finestra favorevole per trattamenti",
                    description = "Nessuna pioggia significativa prevista nelle prossime 48 ore. Ottime condizioni per trattamenti nutrizionali o fogliari.",
                    isAlert = false
                )
            )
        }

        // 2. Wind & Atomizer Drift Analysis
        if (wind >= 16.0) {
            list.add(
                AgronomicAdvice(
                    type = AdviceType.VENTO,
                    title = "💨 Vento sostenuto (${wind.toInt()} km/h)",
                    description = "Forte rischio deriva: sconsigliato l'uso dell'atomizzatore e diserbi. Rischio di dispersione del prodotto.",
                    isAlert = true
                )
            )
        } else {
            list.add(
                AgronomicAdvice(
                    type = AdviceType.VENTO,
                    title = "🍃 Vento debole (${wind.toInt()} km/h)",
                    description = "Condizioni di calma ideali per la micronizzazione e distribuzione omogenea dei prodotti.",
                    isAlert = false
                )
            )
        }

        // 3. Fungal Disease / Olive Fly Risk (Humidity & Temp)
        if (humidity >= 75 && temp in 14.0..24.0) {
            list.add(
                AgronomicAdvice(
                    type = AdviceType.MALATTIE,
                    title = "🍄 Rischio Occhio di Pavone e Lebbra",
                    description = "Umidità alta ($humidity%) e temperatura mite (${temp.toInt()}°C) favoriscono la germinazione delle spore fungine. Ispezionare la chioma.",
                    isAlert = true
                )
            )
        }

        // 4. Irrigation and Water Stress
        if (temp >= 28.0 && !rainToday && rainTomorrow < 0.5) {
            list.add(
                AgronomicAdvice(
                    type = AdviceType.IRRIGAZIONE,
                    title = "💧 Caldo intenso: monitorare idratazione",
                    description = "Elevata evapotraspirazione. Se le piante sono in fase critica (allegagione/ingrossamento drupa), valutare irrigazione di soccorso notturna.",
                    isAlert = false
                )
            )
        }

        // 5. Soil Work & Pruning
        if (!rainToday && rainTomorrow < 1.0 && wind < 25.0) {
            list.add(
                AgronomicAdvice(
                    type = AdviceType.LAVORAZIONI,
                    title = "🚜 Ottime condizioni per lavorazioni suolo",
                    description = "Terreno in tempera favorevole per trinciatura erba, ripassi leggeri e gestione dell'inerbimento nell'oliveto.",
                    isAlert = false
                )
            )
        }

        return list
    }

    private fun getWeatherCodeDescription(code: Int): String {
        return when (code) {
            0 -> "Sereno"
            1 -> "Prevalentemente sereno"
            2 -> "Parzialmente nuvoloso"
            3 -> "Coperto"
            45, 48 -> "Nebbia"
            51, 53, 55 -> "Pioggerella"
            61, 63 -> "Pioggia moderata"
            65 -> "Pioggia forte"
            71, 73, 75 -> "Neve"
            80, 81, 82 -> "Rovesci di pioggia"
            95, 96, 99 -> "Temporale"
            else -> "Variabile"
        }
    }

    private fun getDefaultCondition(): AgronomicWeatherCondition {
        val cal = Calendar.getInstance()
        val month = cal.get(Calendar.MONTH)
        val temp = when (month) {
            in 2..4 -> 18.0
            in 5..7 -> 27.0
            in 8..9 -> 22.0
            else -> 13.0
        }

        val dummyForecasts = listOf(
            DailyForecast("Oggi", "Oggi", temp + 2, temp - 4, 0.0, 10, 0, "Sereno"),
            DailyForecast("Domani", "Domani", temp + 1, temp - 5, 0.0, 15, 1, "Poco nuvoloso"),
            DailyForecast("+2 gg", "Dopodomani", temp + 3, temp - 3, 2.5, 65, 61, "Possibile pioggia"),
            DailyForecast("+3 gg", "Prossimo", temp + 0, temp - 6, 0.0, 20, 2, "Variabile")
        )

        val advice = generateAgronomicAdvice(temp, 58, 0.0, 9.0, dummyForecasts)

        return AgronomicWeatherCondition(
            temperature = temp,
            apparentTemperature = temp,
            humidity = 58,
            windSpeedKmH = 9.0,
            precipitationMm = 0.0,
            weatherCode = 0,
            conditionTitle = "Sereno",
            locationName = "La mia proprietà",
            dailyForecasts = dummyForecasts,
            adviceList = advice,
            lastUpdated = "Oggi"
        )
    }
}
