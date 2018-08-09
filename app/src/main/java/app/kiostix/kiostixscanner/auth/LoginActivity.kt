package app.kiostix.kiostixscanner.auth

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import app.kiostix.kiostixscanner.R
import android.widget.Toast
import app.kiostix.kiostixscanner.MainActivity
import app.kiostix.kiostixscanner.api.ApiClient
import app.kiostix.kiostixscanner.model.User
import com.android.volley.*
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import io.realm.Realm
import io.realm.RealmResults
import io.realm.kotlin.*
import kotlinx.android.synthetic.main.activity_login.*
import org.json.JSONObject
import java.lang.reflect.Method

class LoginActivity : AppCompatActivity() {
    private var exit = false
    val apiClient = ApiClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val body = JSONObject()
        body.put("email", EmailField.text)
        body.put("password", PassField.text)

        val realm = Realm.getDefaultInstance()


        SignInCard.setOnClickListener {
            val queue: RequestQueue? = Volley.newRequestQueue(this)
            val login = object : JsonObjectRequest(Request.Method.POST,
                    apiClient.login,
                    body,
                    Response.Listener { response ->
//                        Toast.makeText(this, response.toString(), Toast.LENGTH_SHORT).show()
                        if (response.getString("message").equals("success")) {
                            realm.executeTransaction { it2: Realm? ->
                                val realmResult: RealmResults<User> = realm.where<User>().findAll()
                                realmResult.deleteFirstFromRealm()
                                val user = realm.createObject<User>()
                                val data = response?.getJSONObject("data")
                                user.fullname = data?.getString("fullname")
                                user.email = data?.getString("email")
                                user.token = data?.getString("token")
                            }
                            val toMainActivity = Intent(this, MainActivity::class.java)
                            startActivity(toMainActivity)
                        } else {
                            Toast.makeText(this, response.getString("message"), Toast.LENGTH_SHORT).show()
                        }
                    },
                    Response.ErrorListener { error: VolleyError? ->
                        Toast.makeText(this, error.toString(), Toast.LENGTH_SHORT).show()
                    }) {

                @Throws(AuthFailureError::class)
                override fun getHeaders(): Map<String, String> {
                    val headers = HashMap<String, String>()
                    headers.put("Authorization", "a2lvc3RpeEFQSTAxMDMwNDIwMTg=")
                    headers.put("Content-Type", "application/json")
                    return headers
                }
            }
            queue?.add(login)
        }

    }

    override fun onBackPressed() {
        if (exit) {
            finish() // finish activity
        } else {
            Toast.makeText(this, "Press Back again to Exit.",
                    Toast.LENGTH_SHORT).show()
            exit = true
            Handler().postDelayed({ exit = false }, 3 * 1000)
        }
    }
}
