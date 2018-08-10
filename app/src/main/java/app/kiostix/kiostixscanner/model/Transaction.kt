package app.kiostix.kiostixscanner.model

import io.realm.RealmObject
import java.util.*

open class Transaction: RealmObject() {

    var famocoId: String? = null
    var famocoName: String? = null
    var tEventName: String? = null
    var ticketName: String? = null
    var barcode: String? = null
    var inCount: String? = null
    var outCount: String? = null
    var lastIn: Date? = null
    var lastOut: Date? = null

}