package app.kiostix.kiostixscanner.api

class ApiClient {
    private val apiUrl = "http://devapi.kiostix.com"
    val cdnDev = "http://dev-web.ultraklin.com/api/lini-ultraklin-laravel/public/stressTest.txt"
    val login = "$apiUrl/login"
    val devices = "$apiUrl/device"
    val getTxt = "$apiUrl/device/txt"
}