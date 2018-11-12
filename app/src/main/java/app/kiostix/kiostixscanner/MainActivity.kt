package app.kiostix.kiostixscanner

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.os.Bundle
import android.os.Environment
import android.os.Vibrator
import android.support.v4.app.DialogFragment
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import app.kiostix.kiostixscanner.adapter.DeviceIdAdapter
import app.kiostix.kiostixscanner.api.ApiClient
import app.kiostix.kiostixscanner.auth.EmailAuth
import app.kiostix.kiostixscanner.auth.LoginActivity
import app.kiostix.kiostixscanner.dialogs.AnyDialogs
import app.kiostix.kiostixscanner.model.*
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.zebra.adc.decoder.BarCodeReader
import com.zebra.adc.decoder.BarCodeReader.ParamNum.LASER_ON_PRIM
import de.siegmar.fastcsv.writer.CsvAppender
import de.siegmar.fastcsv.writer.CsvWriter
import es.dmoral.toasty.Toasty
import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.delete
import io.realm.kotlin.where
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.scanning_layout.*
import kotlinx.android.synthetic.main.sync_action_layout.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import javax.activation.DataHandler
import javax.activation.FileDataSource
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity(),
        AdapterView.OnItemSelectedListener,
        BarCodeReader.DecodeCallback,
        BarCodeReader.ErrorCallback,
        AnyDialogs.EmailDialogListener {

    private val realm: Realm? = Realm.getDefaultInstance()
    private val apiClient = ApiClient()
    private var nfcAdapter : NfcAdapter? = null
    private var nfcPendingIntent: PendingIntent? = null
    private val KEY_LOG_TEXT = "logText"
    val emailAuth = EmailAuth()
    var PARAM_NUM = 765
    var PARAM_VAL1 = 0
    var PARAM_BUM_TIMEOUT: Int = LASER_ON_PRIM.toInt()
    var PARAM_VAL_TIMEOUT: Int = 990

    init {
        try {
            System.loadLibrary("IAL")
            System.loadLibrary("SDL")
            System.loadLibrary("barcodereader44")
        } catch (e: Exception) {
            toast(e.toString())
        }
    }

    enum class Mode {
        UNAVAILABLE,
        IDLE,
        SCANNING,
    }

    var mode: Any? = null
    private var bcr: BarCodeReader? = null
    private lateinit var vibrate: Vibrator
    companion object {
        var gateName = String()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        if (!checkSession()) {
//            backToLogin()
//        }

        GateSpinner.adapter = ArrayAdapter.createFromResource(this, R.array.gate_name, R.layout.gate_spinner_dropdown)
        GateSpinner.onItemSelectedListener = GateChoice()

        vibrate = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        val getTicket = realm?.where<Ticket>()?.findAll()
        val device = realm?.where<Device>()?.findFirst()

        if (device != null) {
            FamocoID.text = device.deviceName
        }

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if (nfcAdapter == null) {
            // Stop here, we definitely need NFC
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show()
            finish()
            return

        }

        if (!nfcAdapter!!.isEnabled) {
            toast("NFC is disabled.")
        } else {
            toast("NFC Enabled")
        }

        nfcPendingIntent = PendingIntent.getActivity(this, 0,
                Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0)

        if (getTicket!!.size > 0) {
            SyncLayout.visibility = View.GONE
            ScanLayout.visibility = View.VISIBLE
        }

        val count = realm?.where<Transaction>()?.count()
        if (count != 0L) {
            val lastTransaction = realm?.where<Transaction>()?.findAll()?.last()
            ApprovedSub.text = count.toString()
            EventNameSub.text = lastTransaction?.tEventName
            TicketNameSub.text = lastTransaction?.ticketName
        }

        var useData: Int = UrlRb.id
        GetDataChoiceRg.setOnCheckedChangeListener { group, checkedId ->
            useData = checkedId
            when (group.checkedRadioButtonId) {
                UrlRb.id -> {
                    UrlCard.visibility = View.VISIBLE
                    DeviceCard.visibility = View.GONE
                }
                DeviceRb.id -> {
                    UrlCard.visibility = View.GONE
                    DeviceCard.visibility = View.VISIBLE
                }
            }
        }

        DownloadCard.setOnClickListener {
            ProgressBar.visibility = View.VISIBLE
            val param = JSONObject()
            if (useData == UrlRb.id) {
                doAsync {
                    var exception: Exception? = null
                    var result: String? = null
                    try {
                        result = downloadFile(UrlInput.text.toString())
                    } catch (e: Exception) {
                        exception = e
                    }
                    uiThread { _ ->
                        if (exception != null) {
//                                    toast("Error Downloading data, please try again")
                            toast(exception.toString())
                        } else {
                            realm?.executeTransactionAsync { r ->
                                r.createAllFromJson(Ticket::class.java, result!!)
                            }
                            toast("Data saved")
                            SyncLayout.visibility = View.GONE
                            ScanLayout.visibility = View.VISIBLE
                            ProgressBar.visibility = View.GONE
                        }
                    }
                }
                ProgressBar.visibility = View.GONE
                return@setOnClickListener
            }
            realm?.executeTransaction {r ->
                val device = r.where<Device>().findAll()
                param.put("device_id", device[0]?.deviceId)
            }
            val queue = Volley.newRequestQueue(this)
            val getTxt = object : JsonObjectRequest(
                    Request.Method.POST,
                    apiClient.getTxt,
                    param,
                    Response.Listener { response ->
//                        toast(response.toString())
                        val data = response.getJSONArray("data")
                        doAsync {
                            var exception: Exception? = null
                            var result: String? = null
                            try {
                                result = downloadFile(data.getJSONObject(0)["url"])
                            } catch (e: Exception) {
                                exception = e
                            }
                            uiThread { _ ->
                                if (exception != null) {
//                                    toast("Error Downloading data, please try again")
                                    toast(exception.toString())
                                } else {
                                    realm?.executeTransactionAsync { r ->
                                        r.createAllFromJson(Ticket::class.java, result!!)
                                    }
                                    toast("Data saved")
                                    SyncLayout.visibility = View.GONE
                                    ScanLayout.visibility = View.VISIBLE
                                    ProgressBar.visibility = View.GONE
                                }
                            }
                        }
                    },
                    Response.ErrorListener { error ->
                        toast(error.toString())
                    }
            ) {
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    headers.put("Authorization", "a2lvc3RpeEFQSTAxMDMwNDIwMTg=")
                    headers.put("Content-Type", "application/json")
                    return headers
                }
            }
            queue.add(getTxt)
        }
        ScanInCard.setOnClickListener {
            when (mode) {
                Mode.IDLE -> {
                    startScan()
                    clearText()
                }
                Mode.SCANNING -> stopScan()
                Mode.UNAVAILABLE -> toast("Unavailable")
            }
        }
        ManualCard.setOnClickListener {
            handleScanResult(BarcodeEdtxt.text.toString())
            clearText()
        }
        initDeviceId()
        if (intent != null) {
            // Check if the app was started via an NDEF intent
//            logMessage("Found intent in onCreate", intent.action.toString())
            processIntent(intent)
        }
    }

    operator fun JSONArray.iterator(): Iterator<JSONObject> = (0 until length()).asSequence().map { get(it) as JSONObject }.iterator()

    override fun onStart() {
        super.onStart()
        initBarcode()
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, nfcPendingIntent, null, null)
    }

    private fun initBarcode() {
        bcr = BarCodeReader.open(1, this)
        if (bcr == null) throw RuntimeException("Cannot open barcode")
        bcr?.setDecodeCallback(this)
        bcr?.setErrorCallback(this)
        bcr?.setParameter(PARAM_NUM, PARAM_VAL1)
        bcr?.setParameter(PARAM_BUM_TIMEOUT, PARAM_VAL_TIMEOUT)
        mode = Mode.IDLE
    }

    override fun onStop() {
        releaseBarcode()
        super.onStop()
    }

    override fun onPause() {
        releaseBarcode()
        nfcAdapter?.disableForegroundDispatch(this)
        super.onPause()
    }


    private fun releaseBarcode() {
        bcr?.setDecodeCallback(null)
        bcr?.setErrorCallback(null)
        bcr?.release()
        bcr = null
        mode = Mode.UNAVAILABLE
    }

    private fun checkSession():Boolean {
        var pass = false
        val user = realm?.where<User>()?.findFirst()
        if (user != null) {
            pass = true
        }

        return pass
    }

    private fun backToLogin() {
        val toLoginActivity = Intent(this, LoginActivity::class.java)
        toLoginActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        toLoginActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        toLoginActivity.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        startActivity(toLoginActivity)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
//        logMessage("Found intent in onNewIntent", intent?.action.toString())
        // If we got an intent while the app is running, also check if it's a new NDEF message
        // that was discovered
        if (intent != null) processIntent(intent)
    }

    private fun initDeviceId() {
        val queue = Volley.newRequestQueue(this)
        val getDeviceId = object : JsonObjectRequest(
                Request.Method.GET,
                apiClient.devices,
                null,
                Response.Listener { response ->
                    if (response.getString("message") == "success") {
                        val getDevicesList = response.getJSONArray("data")
                        val arrayList = ArrayList<DeviceIdSpinnerModel>()
                        for (i in 0 until getDevicesList.length()) {
                            val data = getDevicesList.getJSONObject(i)
                            arrayList.add(DeviceIdSpinnerModel(
                                    data.getString("device_id"),
                                    data.getString("device_name")
                            ))
                        }
                        val deviceIdAdapter = DeviceIdAdapter(this, R.layout.new_default_spinner_dropdown, arrayList)
                        DeviceSpinner.adapter = deviceIdAdapter
                        DeviceSpinner.onItemSelectedListener = this
                    } else {
                        toast("error")
                    }
                },
                Response.ErrorListener { error: VolleyError? ->
                    Toast.makeText(this, error.toString(), Toast.LENGTH_SHORT).show()
                }) {

            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = "a2lvc3RpeEFQSTAxMDMwNDIwMTg="
                headers["Content-Type"] = "application/json"
                return headers
            }
        }
        queue.add(getDeviceId)
    }

    private fun processIntent(checkIntent: Intent) {
        // Check if intent has the action of a discovered NFC tag
        // with NDEF formatted contents
        if (checkIntent.action == NfcAdapter.ACTION_NDEF_DISCOVERED) {
//            logMessage("New NDEF intent", checkIntent.toString())

            // Retrieve the raw NDEF message from the tag
            val rawMessages = checkIntent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
//            logMessage("Raw messages", rawMessages.size.toString())

            // Complete variant: parse NDEF messages
            if (rawMessages != null) {
                val messages = arrayOfNulls<NdefMessage?>(rawMessages.size)// Array<NdefMessage>(rawMessages.size, {})
                for (i in rawMessages.indices) {
                    messages[i] = rawMessages[i] as NdefMessage
                }
                // Process the messages array.
                processNdefMessages(messages)
            }

            // Simple variant: assume we have 1x URI record
            //if (rawMessages != null && rawMessages.isNotEmpty()) {
            //    val ndefMsg = rawMessages[0] as NdefMessage
            //    if (ndefMsg.records != null && ndefMsg.records.isNotEmpty()) {
            //        val ndefRecord = ndefMsg.records[0]
            //        if (ndefRecord.toUri() != null) {
            //            logMessage("URI detected", ndefRecord.toUri().toString())
            //        } else {
            //            // Other NFC Tags
            //            logMessage("Payload", ndefRecord.payload.contentToString())
            //        }
            //    }
            //}

        }
    }

    private fun processNdefMessages(ndefMessages: Array<NdefMessage?>) {
        // Go through all NDEF messages found on the NFC tag
        for (curMsg in ndefMessages) {
            if (curMsg != null) {
                // Print generic information about the NDEF message
//                logMessage("Message", curMsg.toString())
                // The NDEF message usually contains 1+ records - print the number of recoreds
//                logMessage("Records", curMsg.records.size.toString())

                // Loop through all the records contained in the message
                for (curRecord in curMsg.records) {
                    if (curRecord.toUri() != null) {
                        // URI NDEF Tag
//                        logMessage("- URI", curRecord.toUri().toString())
                    } else {
                        // Other NDEF Tags - simply print the payload
//                        logMessage("- Contents", curRecord.payload.contentToString())
//                        logMessage("msg : ", String(curRecord.payload, Charsets.US_ASCII))
                        val payloadText = String(curRecord.payload, Charsets.US_ASCII).substring(3)
                        initBarcode()
                        handleScanResult(payloadText)
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putCharSequence(KEY_LOG_TEXT, "log")
        super.onSaveInstanceState(outState)
    }

    private fun logMessage(header: String, text: String?) {
        toast(header + text)
//        scrollDown()
    }

    private fun downloadFile(txtLink: Any): String? {
        val url = URL(txtLink as String)
        val connection = url.openConnection()
        connection.connectTimeout = 60000

        val inBuff = BufferedReader(InputStreamReader(connection.getInputStream()))

        return inBuff.readLine()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.ResetItem -> {
                realm?.executeTransaction {
                    val transactions = it.where<Transaction>().findAll()
                    if (transactions.size > 0 ) {
                        val famoco = it.where<Device>().findFirst()
                        val history = it.createObject<History>()
                        val jsonData = JSONObject()
                        val jsonArray = JSONArray()
                        val formatDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        history.device_name = famoco?.deviceName
                        history.created_at = formatDateTime.format(Date())
                        transactions.forEach { v ->
                            jsonData.put("famocoId", v.famocoId)
                            jsonData.put("famocoName", v.famocoName)
                            jsonData.put("tEventName", v.tEventName)
                            jsonData.put("ticketName", v.ticketName)
                            jsonData.put("barcode", v.barcode)
                            jsonData.put("inCount", v.inCount.get())
                            jsonData.put("outCount", v.outCount.get())
                            jsonData.put("lastIn", v.lastIn.toString())
                            jsonData.put("lastOut", v.lastOut.toString())
                            jsonData.put("inside", v.inside)
                            jsonArray.put(jsonData)
                        }
                        history.transaction_data = jsonArray.toString()
                    }
                    it.delete<Ticket>()
                    it.delete<Transaction>()
                }
                SyncLayout.visibility = View.VISIBLE
                ScanLayout.visibility = View.GONE
            }
            R.id.ExportItem -> {
                val count = realm?.where<History>()?.count()
                if (count != 0L) {
                    exportRealm()
                }
            }
            R.id.ExportJsonItem -> {
                val count = realm?.where<History>()?.count()
                if (count != 0L) {
                    exportRealmToJson()
                }
            }
            R.id.SendEmail -> {
                AnyDialogs().show(supportFragmentManager, "Send email dialog")
//                sendEmail()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    class GateChoice: AdapterView.OnItemSelectedListener {
        private val main = MainActivity

        override fun onNothingSelected(p0: AdapterView<*>?) {
            //
        }

        override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, p3: Long) {
            main.gateName = parent?.getItemAtPosition(pos).toString()
        }

    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        realm?.executeTransaction {
            it.delete<Device>()
            val device = it.createObject<Device>()
            device.deviceId = (parent?.selectedItem as DeviceIdSpinnerModel).deviceId
            device.deviceName = (parent.selectedItem as DeviceIdSpinnerModel).deviceName
            FamocoID.text = device.deviceName
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onDecodeComplete(symbology: Int, length: Int, data: ByteArray?, reader: BarCodeReader?) {
        if (length == 0 || data == null || data.isEmpty()) {
            return
        }
        try {
            val decodedText = String(data).substring(0, length)
            handleScanResult(decodedText)
        } catch (e: Exception) {

        }

    }

    override fun onEvent(p0: Int, p1: Int, p2: ByteArray?, p3: BarCodeReader?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onError(p0: Int, p1: BarCodeReader?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun handleScanResult(decodedText: String) {
        ProgressBar.visibility = View.VISIBLE
        BarcodeEdtxt.setText(decodedText)
        var message = String()
        var failed = true
        var vTicketName = String()
        var vEventName = String()
        var vApproved = String()
        var used = false
        realm?.executeTransactionAsync({ realm ->
            val ticket = realm.where<Ticket>().findAll()
            loop@ for (ii in 0 until ticket.size) {
                val data = JSONArray(ticket[ii]?.schedule_data)
                for (i in 0 until data.length()) {
                    val sArr = data.getJSONObject(i)
                    for (i1 in 0 until sArr.length()) {
                        val sObj = sArr.getJSONArray("ticket_data")
                        for (i2 in 0 until sObj.length()) {
                           val tArr = sObj.getJSONObject(i2)
                            for (i3 in 0 until tArr.length()) {
                                val ticketName = tArr["ticket_name"] as String
                                val tObj = tArr.getJSONArray("transaction_data")
                                for (i4 in 0 until tObj.length()) {
                                    val trArr = tObj.getJSONObject(i4)
                                    for (i5 in 0 until trArr.length()) {
                                        val getBarcode = trArr.getString("barcode")
                                        if (getBarcode == decodedText && !getBarcode.isBlank() ) {
                                            failed = false
                                            val formatDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                            val transaction = realm.where<Transaction>().equalTo("barcode", decodedText).findFirst()
                                            if (transaction != null) {
                                                // in
                                                if (gateName == "In") {
                                                    used = true
                                                    transaction.inside = true
                                                    transaction.lastIn = formatDateTime.format(Date())
                                                    transaction.inCount.increment(1)
                                                    message = "Ticket telah digunakan"
                                                    break@loop
                                                }
                                                // out
                                                else {
                                                    used = true
                                                    transaction.inside = false
                                                    transaction.lastOut = formatDateTime.format(Date())
                                                    transaction.outCount.increment(1)
                                                    message = "Ticket telah digunakan"
                                                    break@loop
                                                }
                                            } else {
                                                if (gateName == "In") {
                                                    val transaction = realm.createObject<Transaction>(getBarcode)
                                                    val device = realm.where<Device>().findAll()
                                                    transaction.famocoId = device[0]?.deviceId as String
                                                    transaction.famocoName = device[0]?.deviceName as String
                                                    transaction.tEventName = ticket[ii]?.event_name
                                                    transaction.ticketName = ticketName
                                                    transaction.lastIn = formatDateTime.format(Date())
                                                    transaction.inCount.set(0)
                                                    transaction.outCount.set(0)
                                                    transaction.inside = true
                                                    val count = realm.where<Transaction>().findAll().count()
                                                    vTicketName = transaction.ticketName.toString()
                                                    vEventName = transaction.tEventName.toString()
                                                    vApproved = count.toString()
                                                    transaction.inCount.increment(1)
                                                    message = "Ticket berhasil, Silahkan Masuk"
                                                    break@loop
                                                } else {
                                                    val transaction = realm.createObject<Transaction>(getBarcode)
                                                    val device = realm.where<Device>().findAll()
                                                    transaction.famocoId = device[0]?.deviceId as String
                                                    transaction.famocoName = device[0]?.deviceName as String
                                                    transaction.tEventName = ticket[ii]?.event_name
                                                    transaction.ticketName = ticketName
                                                    transaction.lastIn = formatDateTime.format(Date())
                                                    transaction.inCount.set(0)
                                                    transaction.outCount.set(0)
                                                    transaction.inside = false
                                                    val count = realm.where<Transaction>().findAll().count()
                                                    vTicketName = transaction.ticketName.toString()
                                                    vEventName = transaction.tEventName.toString()
                                                    vApproved = count.toString()
                                                    transaction.outCount.increment(1)
                                                    message = "Ticket berhasil, Silahkan Keluar"
                                                    break@loop
                                                }
                                            }
                                        } else {
                                            failed = true
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (failed) {
                vibrate.vibrate(50)
                message = "Ticket tidak terdaftar"
            }

        }, {
            if (!vApproved.isBlank()) {
                ApprovedSub.text = vApproved
                EventNameSub.text = vEventName
                TicketNameSub.text = vTicketName
            }
            scannedResultAlert(message, failed, used)
            ProgressBar.visibility = View.GONE
        }, {
            toast(it.toString())
        })
        mode = Mode.IDLE
    }

    private fun startScan() {
        bcr?.startDecode()
        mode = Mode.SCANNING
    }

    private fun stopScan() {
        bcr?.stopDecode()
        mode = Mode.IDLE
    }

    private fun clearText() {
        BarcodeEdtxt.setText("")
    }

    private fun scannedResultAlert(message: String, failed: Boolean, used: Boolean) {
        if (failed) {
            Toasty.error(this, message).show()
        } else {
            if (used) {
                Toasty.error(this, message).show()
            }
            else {
                Toasty.success(this, message).show()
            }
        }
    }

    private fun exportRealm() {
        val file = File(Environment.getExternalStorageDirectory().path+"/default.realm")
        if (file.exists()) {
            file.delete()
        }
        realm?.writeCopyTo(file)
        Toasty.info(this, "Successfully exported to Realm").show()
    }

    private fun exportRealmToJson() {
        val history = realm?.where<History>()?.findAll()
        var i = 0
        history?.forEach { v ->
            try {
                val root = File(Environment.getExternalStorageDirectory(), "exported_transaction")
                if (!root.exists()) root.mkdirs()
                i++
                val json = File(root, String.format(v.device_name+"-"+i+"-"+"-"+v.created_at+".json"))
                val writer = FileWriter(json)
                writer.append(v.toString())
                writer.flush()
                writer.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        Toasty.info(this, "Successfully exported to JSON").show()
    }

    private fun sendEmail(email: String) {
        ProgressBar.visibility = View.VISIBLE
        val session = Session.getInstance(emailAuth.init(), object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(emailAuth.email, emailAuth.pass)
            }
        })
        if (email.isBlank()) {
            ProgressBar.visibility = View.GONE
            toast("Email is blank")
            return
        }
        try {
            val msg = MimeMessage(session)
            val to = email
            msg.addRecipient(Message.RecipientType.TO, InternetAddress(to))
            msg.subject = "KiosTix Famoco Device Report"
            val multiPart = MimeMultipart()
            val body = MimeBodyPart()
            body.setText("Latest transaction report")
            multiPart.addBodyPart(body)
            val history = realm?.where<History>()?.findAll()
            var i = 0
            val root = File(Environment.getExternalStorageDirectory(), "exported_transaction")
            history?.forEach { v ->
                val attach = MimeBodyPart()
                i++
                val filename = String.format(v.device_name+"-"+i+"-"+"-"+v.created_at+".csv")
                try {
                    if (!root.exists()) root.mkdirs()
                    val json = File(root, filename)
                    val csv = CsvWriter()
                    try {
                        val csvAppender: CsvAppender = csv.append(FileWriter(json))
                        csvAppender.appendLine(
                                "famocoId",
                                "famocoName",
                                "tEventName",
                                "ticketName",
                                "barcode",
                                "inCount",
                                "outCount",
                                "lastIn",
                                "lastOut",
                                "inside?"
                        )
                        val transactions = JSONArray(v.transaction_data)
                        for (item in transactions) {
                            csvAppender.appendLine(
                                    item["famocoId"].toString(),
                                    item["famocoName"].toString(),
                                    item["tEventName"].toString(),
                                    item["ticketName"].toString(),
                                    item["barcode"].toString(),
                                    item["inCount"].toString(),
                                    item["outCount"].toString(),
                                    item["lastIn"].toString(),
                                    item["lastOut"].toString(),
                                    item["inside"].toString()
                            )
                        }
                        csvAppender.flush()
                        csvAppender.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                        toast(e.printStackTrace().toString())
                    }
                    val dataSource = FileDataSource(json)
                    attach.dataHandler = DataHandler(dataSource)
                    attach.fileName = filename
                    multiPart.addBodyPart(attach)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            msg.setContent(multiPart)
//            msg.setText("test body")

            doAsync {
                var catch: MessagingException? = null
                try {
                    Transport.send(msg)
                } catch (mex: MessagingException) {
                    catch = mex
                }
                uiThread {
                    if (catch != null) {
                        ProgressBar.visibility = View.GONE
                        toast(catch.message.toString())
                        return@uiThread
                    }
                    ProgressBar.visibility = View.GONE
                    toast("Send email success")
                }
            }
        } catch (mex: MessagingException) {
            ProgressBar.visibility = View.GONE
            toast("Failed to send email \n$mex")
        }
    }

    override fun onSendClick(dialog: DialogFragment, emailInput: String) {
        sendEmail(emailInput)
    }
 }
