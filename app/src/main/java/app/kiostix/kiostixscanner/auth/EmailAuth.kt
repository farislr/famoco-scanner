package app.kiostix.kiostixscanner.auth

import java.util.*


open class EmailAuth {
//    val emailAuth = "dev@kios.io"
//    val pass = "DevPejaten5"
    val email = "support@ultraklin.com"
    val pass = "\$LINI->P@ssw0rd=Ok"

    fun init(): Properties {
        val props = Properties()
        props["mail.smtp.host"] = "true"
        props["mail.smtp.starttls.enable"] = "true"
        props["mail.smtp.host"] = "smtp.gmail.com"
        props["mail.smtp.port"] = "587"
        props["mail.smtp.auth"] = "true"
        return props
    }

}