package app.kiostix.kiostixscanner

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import app.kiostix.kiostixscanner.api.ApiClient
import app.kiostix.kiostixscanner.auth.LoginActivity
import app.kiostix.kiostixscanner.model.DeviceIdSpinnerModel
import app.kiostix.kiostixscanner.model.Ticket
import app.kiostix.kiostixscanner.model.User
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import io.realm.Realm
import io.realm.kotlin.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.sync_action_layout.*
import kotlinx.android.synthetic.main.scanning_layout.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import android.widget.AdapterView
import app.kiostix.kiostixscanner.adapter.DeviceIdAdapter
import com.zebra.adc.decoder.BarCodeReader
import com.zebra.adc.decoder.BarCodeReader.ParamNum.LASER_ON_PRIM
import org.json.JSONObject
import java.lang.Exception

class MainActivity : AppCompatActivity(),
        AdapterView.OnItemSelectedListener,
        BarCodeReader.DecodeCallback,
        BarCodeReader.ErrorCallback {

    private val realm: Realm? = Realm.getDefaultInstance()
    private val apiClient = ApiClient()
    private lateinit var currentDeviceId: String

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        if (!checkSession()) {
//            backToLogin()
//        }

        val getTicket = realm?.where<Ticket>()?.findAll()
        if (getTicket!!.size > 0) {
            SyncLayout.visibility = View.GONE
            ScanLayout.visibility = View.VISIBLE
        }

        DownloadCard.setOnClickListener {
            val param = JSONObject()
            param.put("device_id", currentDeviceId)
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
                            uiThread { _: MainActivity ->
                                if (exception != null) {
                                    toast("Error Downloading data, please try again")
                                } else {
                                    val dataArray = JSONArray(result)
                                    var ticket: Ticket
                                    realm?.executeTransaction {realm ->
                                        realm.delete<Ticket>()
                                    }
                                    for (i in 0 until dataArray.length()) {
                                        val data = dataArray.getJSONObject(i)
                                        realm?.executeTransaction {realm ->
                                            ticket = realm.createObject()
                                            ticket.eventName = data.getString("event_name")
                                            ticket.scheduleData = data.getString("schedule_data")
                                        }
                                    }
                                    toast("Data saved")
                                    SyncLayout.visibility = View.GONE
                                    ScanLayout.visibility = View.VISIBLE
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
        ScanCard.setOnClickListener {
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
    }

    override fun onStart() {
        super.onStart()
        initBarcode()
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
        super.onStop()
        releaseBarcode()
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
                        val deviceIdAdapter = DeviceIdAdapter(this, android.R.layout.simple_spinner_dropdown_item, arrayList)
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

    private fun downloadFile(txtLink: Any): String {
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
                    realm.delete<Ticket>()
                }
                SyncLayout.visibility = View.VISIBLE
                ScanLayout.visibility = View.GONE
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        currentDeviceId = (parent?.selectedItem as DeviceIdSpinnerModel).deviceId
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onDecodeComplete(symbology: Int, length: Int, data: ByteArray?, reader: BarCodeReader?) {
        if (length == 0 || data == null || data.size == 0) {
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
        BarcodeEdtxt.setText(decodedText)
        realm?.executeTransaction { _ ->
            val ticket = realm.where<Ticket>().findAll()
            var failed: Boolean = true
            loop@ for (ii in 0 until ticket.size) {
                val data = JSONArray(ticket[ii]?.scheduleData)
                for (i in 0 until data.length()) {
                    val sArr = data.getJSONObject(i)
                    for (i1 in 0 until sArr.length()) {
                        val sObj = sArr.getJSONArray("ticket_data")
                        for (i2 in 0 until sObj.length()) {
                           val tArr = sObj.getJSONObject(i2)
                            for (i3 in 0 until tArr.length()) {
                                val tObj = tArr.getJSONArray("transaction_data")
                                for (i4 in 0 until tObj.length()) {
                                    val trArr = tObj.getJSONObject(i4)
                                    for (i5 in 0 until trArr.length()) {
                                        val getBarcode = trArr.getString("barcode")
                                        if (getBarcode == decodedText) {
                                            failed = false
                                            toast("success")
                                            break@loop
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
                toast("failed")
            }
        }
        mode = Mode.IDLE
    }

    private fun startScan() {
        bcr?.startDecode()
        mode = Mode.SCANNING
    }

    /**
     * 3. Stop a barcode scan.
     */
    private fun stopScan() {
        bcr?.stopDecode()
        mode = Mode.IDLE
    }

    private fun clearText() {
        BarcodeEdtxt.setText("")
    }
}
