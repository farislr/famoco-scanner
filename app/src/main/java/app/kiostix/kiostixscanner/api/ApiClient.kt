package app.kiostix.kiostixscanner.api

class ApiClient {
    private val apiUrl = "http://devapi.kiostix.com"
    val cdnUrl = "http://devcdn.kiostix.com/devices_txt/7.txt"
    val cdnUrl2 = "http://dev-web.ultraklin.com/api/lini-ultraklin-laravel/public/7.txt"
    val login = "$apiUrl/login"
    val devices = "$apiUrl/device"
    val getTxt = "$apiUrl/device/txt"
}