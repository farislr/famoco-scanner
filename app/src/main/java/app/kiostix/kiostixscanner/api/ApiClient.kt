package app.kiostix.kiostixscanner.api

class ApiClient {
    private val apiUrl = "http://devapi.kiostix.com"
    private val apiUrlDev = "http://apiv3.kiostix.com"
    val cdn = "http://dev-web.ultraklin.com/api/lini-ultraklin-laravel/public/7.txt"
    val cdnDev = "http://bar_co_de.kiostix.com/EventAccess/Football_AEF_20180812.txt"
    val login = "$apiUrl/login"
    val devices = "$apiUrl/device"
    val getTxt = "$apiUrl/device/txt"
}