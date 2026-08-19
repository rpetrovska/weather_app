import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// --- 1. Retrofit Data Models ---
data class WeatherApiResponse(
    val forecast: Forecast
)

data class Forecast(
    @SerializedName("forecastday")
    val forecastDay: List<ForecastDay>
)

data class ForecastDay(
    val date: String,
    val day: Day,
    val hour: List<Hour>
)

data class Day(
    @SerializedName("mintemp_c") val minTempC: Double,
    @SerializedName("maxtemp_c") val maxTempC: Double,
    @SerializedName("avghumidity") val avgHumidity: Double,
    @SerializedName("maxwind_kph") val maxWindKph: Double
)

data class Hour(
    @SerializedName("wind_dir") val windDir: String
)

// --- 2. Local Application Data Model ---
data class ForecastMetrics(
    val minTemp: Double,
    val maxTemp: Double,
    val humidity: Double,
    val windSpeed: Double,
    val windDir: String
)

// --- 3. Retrofit API Interface ---
interface WeatherApiService {
    @GET("v1/forecast.json")
    fun getForecast(
        @Query("key") apiKey: String,
        @Query("q") city: String,
        @Query("days") days: Int = 2,
        @Query("aqi") aqi: String = "no"
    ): Call<WeatherApiResponse>
}

// --- 4. Main Function ---
fun main() {
    val apiKey = "7834374a79a3406d9ac135237261808"
    val baseUrl = "http://api.weatherapi.com/"
    val cities = listOf("Chisinau", "Madrid", "Kyiv", "Amsterdam")

    // Initialize Retrofit
    val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService = retrofit.create(WeatherApiService::class.java)

    val forecastData = mutableMapOf<String, MutableMap<String, ForecastMetrics>>()
    cities.forEach { forecastData[it] = mutableMapOf() }

    val dates = mutableSetOf<String>()

    for (city in cities) {
        try {
            val response = apiService.getForecast(apiKey = apiKey, city = city).execute()

            if (!response.isSuccessful || response.body() == null) {
                throw RuntimeException("HTTP Error: ${response.code()}")
            }

            val weatherData = response.body()!!
            val tomorrow = weatherData.forecast.forecastDay[1]

            val date = tomorrow.date
            dates.add(date)

            val day = tomorrow.day
            val hour12 = tomorrow.hour[12]

            forecastData[city]!![date] = ForecastMetrics(
                minTemp = day.minTempC,
                maxTemp = day.maxTempC,
                humidity = day.avgHumidity,
                windSpeed = day.maxWindKph,
                windDir = hour12.windDir
            )
        } catch (e: Exception) {
            println("Error fetching data for $city: ${e.message}")
        }
    }

    val sortedDates = dates.sorted()
    val header = "%-15s".format("City") + sortedDates.joinToString("") { "%-64s".format(it) }

    println(header)
    println("=".repeat(header.length))

    for (city in cities) {
        var row = "%-15s".format(city)
        for (date in sortedDates) {
            val cellData = forecastData[city]?.get(date)
            val cell = if (cellData != null) {
                val formatted = "%-8s: %-6.1f | %-8s: %-6.1f | %-8s: %-6.0f | %-8s: %-6.1f | %-8s: %-6s".format(
                    "Min temp (°C)", cellData.minTemp,
                    "Max temp (°C)", cellData.maxTemp,
                    "Humidity (%)", cellData.humidity,
                    "Wind speed (kph)", cellData.windSpeed,
                    "Wind direction", cellData.windDir
                )
                "%-64s".format(formatted)
            } else {
                "%-64s".format("Data unavailable")
            }
            row += cell
        }
        println(row)
    }
}