package app
import com.beust.klaxon.*
import java.net.URL
import java.util.*
import java.net.URLEncoder.encode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

class Geo {
    private val key:String = "&key=0826219a14f94bfbb49a56b9902f31e8"
    private val url:String = "https://api.opencagedata.com/geocode/v1/json"

    fun makeUrl(place: String):String {
        return "$url?q=$place&limit=1$key"
    }

    fun makeRequest(url:String):String {
        return URL(url).readText()
    }
}

class Weather(private var lat:String = "0", private var lng:String = "0") {
    private val key:String = "&appid=c08b3f72d1bbfdc53e34c571a5cc5c86"
    private var url:String = "https://api.openweathermap.org/data/2.5/onecall"

    private var metricUnit:String = "metric"
    var temperatureUnit:String = "°C"
    var speedUnit:String = "m/s"

    var sunrise:Int = 0
    var sunset:Int = 0

    fun makeUrl():String {
        return "$url?lat=$lat&lon=$lng&exclude=minutely&units=$metricUnit$key"
    }

    fun makeRequest(url:String):String {
        return URL(url).readText()
    }

    fun metricSetter() {
        println("Results will be shown in metric system")
        println("Do you prefer results in imperial units?")
        print("Please enter 'Y' for imperial or 'N' for metric:")

        val input:String? = readLine()!!

        if (input!![0] == 'Y' || input[0] == 'y'){
            this.metricUnit = "imperial"
            this.temperatureUnit = "°F"
            this.speedUnit = "m/h"
        }
        else if (input[0] != 'N' && input[0] != 'n') {
            println("Invalid choice")
            println("Please try again")
            metricSetter()
        }
    }

    fun setSunriseSunset(sunrise: Int, sunset: Int) {
        this.sunrise = sunrise
        this.sunset = sunset
    }
}

class WeatherPrinter(private val weatherResult: JsonObject, private val timeZone: String, private val temperatureUnit: String, private val speedUnit: String, private val sunrise: Int, private val sunset: Int) {

    private fun UVIndex(uv:Double):String {
        val index:String
        when {
            uv < 3 -> {
                index = "Low"
            }
            uv < 6 -> {
                index = "Moderate"
            }
            uv < 8 -> {
                index = "High"
            }
            uv < 11 -> {
                index = "Very High"
            }
            uv < 6 -> {
                index = "Moderate"
            }
            else -> {
                index = "Extreme"
            }
        }
        return index
    }

    private fun windDirection(deg:Int):String {
        val directions:List<String> = listOf(" N", " NNE", " NE", " ENE", " E", " ESE", " SE", " SSE", " S", " SSW", " SW", " WSW", " W", " WNW", " NW", " NNW")
        return directions[(deg / (360.0 / 16)).roundToInt() % 16]
    }

    fun currentWeather() {
        println("Current temperature: ${weatherResult.lookup<Double>("current.temp")[0].roundToInt()}${temperatureUnit}")
        println("Feels like: ${weatherResult.lookup<Double>("current.feels_like")[0].roundToInt()}${temperatureUnit}")
        println("${weatherResult.lookup<Double>("daily.temp.max")[0].roundToInt()}${temperatureUnit}" +
                " / ${weatherResult.lookup<Double>("daily.temp.min")[0].roundToInt()}${temperatureUnit}")
        println("${weatherResult.lookup<String>("current.weather.main")[0]}\n")

        println("UV index: ${UVIndex(weatherResult.lookup<Double>("current.uvi")[0])}" +
                " (${weatherResult.lookup<Double>("current.uvi")[0].roundToInt()})")
        println("Humidity: ${weatherResult.lookup<Int>("current.humidity")[0]}%")
        println("Wind speed: ${weatherResult.lookup<Double>("current.wind_speed")[0].roundToInt()} $speedUnit")
        println("Wind direction:${windDirection(weatherResult.lookup<Int>("current.wind_deg")[0])}")

        println("Sunrise: ${dateTime(sunrise, timeZone, "K:mm a")}")
        println("Sunset: ${dateTime(sunset, timeZone, "K:mm a")}\n")
    }

    fun hourlyWeather() {
        println("Hourly weather:")
        for (i in 4..25 step 4) {
            print(dateTime(weatherResult.lookup<Int>("hourly.dt")[i], timeZone, "K:mm a"))
            print(": ${weatherResult.lookup<Double>("hourly.temp")[i].roundToInt()}${temperatureUnit}, ")
            println("Probability of rain: ${(weatherResult.lookup<Double>("hourly.pop")[i] * 100).roundToInt()}%")
        }
    }

    fun weeklyWeather() {
        println("\nForecast for next week:")
        for (i in 1..6) {
            print(dateTime(weatherResult.lookup<Int>("daily.dt")[i], timeZone, "EEE, MMMM d"))
            print(": ${weatherResult.lookup<Double>("daily.temp.max")[i].roundToInt()}${temperatureUnit} / ")
            print("${weatherResult.lookup<Double>("daily.temp.min")[i].roundToInt()}${temperatureUnit}, ")
            println("Probability of rain: ${(weatherResult.lookup<Double>("daily.pop")[i] * 100).roundToInt()}%")
        }
    }
}

fun getLocation():String {
    print("Please enter a location:")
    return try {
        val location:String = readLine()!!
        if(location == ""){
            println("Location can't be empty")
            getLocation()
        }
        location
    } catch (e: Exception) {
        println(e)
        getLocation()
    }

}

fun testLocation(location:String): String {
    return try {
        Geo().makeRequest(Geo().makeUrl(encode(location, "utf-8")))
    } catch (e: Exception) {
        println("Invalid location entered")
        getLocation()
    }
}

fun stringToJSON(string: String): JsonObject {
    val parser: Parser = Parser.default()
    val stringBuilder: StringBuilder = StringBuilder(string)
    return parser.parse(stringBuilder) as JsonObject
}

fun Double.round(decimals: Int = 2): Double = "%.${decimals}f".format(this).toDouble()

fun degreeConversion(deg:String): String {
    val direction: Map<String, Int> = mapOf(" N" to 1, " S" to -1, " E" to 1, " W" to -1)
    val new = deg.replace('°',' ').replace('\'',' ').replace("\'\'"," ")
    val newList = new.split("  ")
    return (newList[0].toInt() + newList[1].toInt()/60.0 * direction.getValue(newList[3])).round(3).toString()
}

fun dateTime(time:Int, zone:String, format:String = "EEE, MMMM d K:mm a"): String {
    return try {
        val zoneId = ZoneId.of(zone)
        val instant = Instant.ofEpochSecond(time.toLong())
        val formatter = DateTimeFormatter.ofPattern(format, Locale.ENGLISH)
        instant.atZone(zoneId).format(formatter)
    } catch (e: Exception) {
        e.toString()
    }
}


fun main(args: Array<String>){

    try {
        val request = stringToJSON(testLocation(getLocation()))
        val lat:String = degreeConversion((request).lookup<String>("results.annotations.DMS.lat")[0])
        val lon:String = degreeConversion((request).lookup<String>("results.annotations.DMS.lng")[0])

        val myWeather = Weather(lat, lon)
        myWeather.metricSetter()
        myWeather.setSunriseSunset(request.lookup<Int>("results.annotations.sun.rise.apparent")[0], request.lookup<Int>("results.annotations.sun.set.apparent")[0])
        val weatherResult = stringToJSON(myWeather.makeRequest(myWeather.makeUrl()))
        val weather = WeatherPrinter(weatherResult, request.lookup<String>("results.annotations.timezone.name")[0], myWeather.temperatureUnit, myWeather.speedUnit, myWeather.sunrise, myWeather.sunset)

        println("\nShowing results for " + request.lookup<String>("results.formatted")[0])
        println(dateTime(weatherResult.lookup<Int>("current.dt")[0], request.lookup<String>("results.annotations.timezone.name")[0]) + "\n")

        weather.currentWeather()
        weather.hourlyWeather()
        weather.weeklyWeather()
    }
    catch (e: Exception) {
        println("Looks like the location wasn't found")
        main()
    }
}
