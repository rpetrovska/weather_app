import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import org.json.JSONObject

data class ForecastMetrics(
    val minTemp: Double,
    val maxTemp: Double,
    val humidity: Double,
    val windSpeed: Double,
    val windDir: String
)

fun main() {
    val apiKey = "7834374a79a3406d9ac135237261808"
    val baseUrl = "http://api.weatherapi.com/v1/forecast.json"
    val cities = listOf("Chisinau", "Madrid", "Kyiv", "Amsterdam")

    val forecastData = mutableMapOf<String, MutableMap<String, ForecastMetrics>>()
    cities.forEach { forecastData[it] = mutableMapOf() }

    val dates = mutableSetOf<String>()
    val client = HttpClient.newHttpClient()

    for (city in cities) {
        try {
            val url = "$baseUrl?key=$apiKey&q=$city&days=2&aqi=no"
            val request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() != 200) {
                throw RuntimeException("HTTP Error: ${response.statusCode()}")
            }

            val json = JSONObject(response.body())
            val tomorrow = json.getJSONObject("forecast")
                .getJSONArray("forecastday")
                .getJSONObject(1)

            val date = tomorrow.getString("date")
            dates.add(date)

            val day = tomorrow.getJSONObject("day")
            val hour12 = tomorrow.getJSONArray("hour").getJSONObject(12)

            forecastData[city]!![date] = ForecastMetrics(
                minTemp = day.getDouble("mintemp_c"),
                maxTemp = day.getDouble("maxtemp_c"),
                humidity = day.getDouble("avghumidity"),
                windSpeed = day.getDouble("maxwind_kph"),
                windDir = hour12.getString("wind_dir")
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