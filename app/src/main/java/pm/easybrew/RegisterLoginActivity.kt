package pm.easybrew

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class RegisterLoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // grab email and password
        val email = findViewById<TextView>(R.id.editEmail)
        val password = findViewById<TextView>(R.id.editPassword)


        // evaluate them if register
        findViewById<Button>(R.id.btnRegister).setOnClickListener { view ->
            // validate email and password

            // call api for registering

            // show success or failure

            // send to MainActivity
        }
        // verify them if login
        findViewById<Button>(R.id.btnLogin).setOnClickListener { view ->
            // call api for login

            // show success or failure

            // send to MainActivity
        }
    }
}