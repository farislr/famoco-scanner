package app.kiostix.kiostixscanner.model

import io.realm.MutableRealmInteger
import io.realm.RealmObject
import io.realm.annotations.Index
import io.realm.annotations.PrimaryKey
import java.util.*

open class Transaction: RealmObject() {

    var famocoId: String? = null

    var famocoName: String? = null

    var tEventName: String? = null

    var ticketName: String? = null

    @PrimaryKey @Index
    var barcode: String? = null

    val inCount = MutableRealmInteger.valueOf(0)

    val outCount = MutableRealmInteger.valueOf(0)

    var lastIn: String? = null

    var lastOut: String? = null

    var status: Boolean = false

}