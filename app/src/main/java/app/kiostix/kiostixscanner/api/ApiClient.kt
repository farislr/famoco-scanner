package app.kiostix.kiostixscanner.api

class ApiClient {
    private val apiUrl = "http://devapi.kiostix.com"
    val login = "$apiUrl/login"
    val devices = "$apiUrl/device"
    val getTxt = "$apiUrl/device/txt"
}