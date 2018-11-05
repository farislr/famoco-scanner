package app.kiostix.kiostixscanner.auth

import java.util.*

open class EmailAuth {
    val email = "dev@kios.io"
    val pass = "DevPejaten5"

    fun getProps(): Properties {
        val props = Properties()
        props["mail.smtp.auth"] = "true"
        props["mail.smtp.starttls.enable"] = "true"
        props["mail.smtp.host"] = "smtp.gmail.com"
        props["mail.smtp.port"] = "587"

        return props
    }
}