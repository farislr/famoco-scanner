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
import app.kiostix.kiostixscanner.model.Ticket
import app.kiostix.kiostixscanner.model.User
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import io.realm.Realm
import io.realm.RealmResults
import io.realm.kotlin.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.sync_action_layout.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.jetbrains.anko.uiThread
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL


class MainActivity : AppCompatActivity() {

    private val realm: Realm? = Realm.getDefaultInstance()
    private val apiClient = ApiClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        if (!checkSession()) {
//            backToLogin()
//        }

        val user = realm?.where<Ticket>()?.findAll()
        if (user!!.size > 0) {
            SyncLayout.visibility = View.GONE
            ScanLayout.visibility = View.VISIBLE
        }

        DownloadCard.setOnClickListener {
            doAsync {
                val result = downloadFile()
                uiThread { _: MainActivity ->
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
        deviceId()
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

    private fun deviceId() {
        val queue = Volley.newRequestQueue(this)
        val getDeviceId = object : JsonObjectRequest(
                Request.Method.GET,
                apiClient.devices,
                null,
                Response.Listener { response ->
                    if (response.getString("message") == "success") {
                        val getDevicesList = response.getJSONArray("data")
                        val arrayList = ArrayList<String>()
                        for (i in 0 until getDevicesList.length()) {
                            val data = getDevicesList.getJSONObject(i)
                            arrayList.add(data.getString("device_id"))
                        }
                        val arrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, arrayList)
                        DeviceSpinner.adapter = arrayAdapter
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

    private fun downloadFile(): String {
        val url = URL(apiClient.cdnUrl)
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
                    realm.deleteAll()
                }
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
