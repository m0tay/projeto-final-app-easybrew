package pm.easybrew

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import pm.easybrew.databinding.ActivityMainBinding
import pm.easybrew.objects.Beverage

class MainActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    companion object {
        private const val CACHE_TTL_MS = 10 * 60 * 1000L // 10 minutes
        private const val CACHE_KEY = "last_menu_cache"
        private const val CACHE_MACHINE_ID_KEY = "last_machine_id"
        private lateinit var nav: BottomNavigationView
    }

    private data class CachedMenu(val timestamp: Long, val machineId: String, val records: List<Beverage>)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }
        val sharedPref = getSharedPreferences("easybrew_session", MODE_PRIVATE)
        @SuppressLint("SetTextI18n")
        binding.userBalanceTextView.text = "${sharedPref.getString("balance", "0.0")} €"
        @SuppressLint("SetTextI18n")
        binding.welcomeUserTextView.text = "${getString(R.string.welcome)}, ${sharedPref.getString("first_name", "oops")}"

        if (savedInstanceState == null) {
            // valid menu cache?
            // TODO: descomentar isto
            // val cachedMachineId = getCachedMachineId()
            // valor hardcode para testes pessoais
            val cachedMachineId = "62b2ea0a-cc67-11f0-bff7-001dd8b7204b"

            if (cachedMachineId != null) {
                openRecyclerViewMenu(cachedMachineId)
            } else {
                openScanQRCodeFragment()
            }
        }

        nav = binding.bottomNavigationView

        nav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.navigation_menu -> {
                    // Check cache first
                    val cachedMachineId = getCachedMachineId()
                    if (cachedMachineId != null) {
                        RecyclerViewMenuFragment().apply {
                            arguments = Bundle().apply {
                                putString("machineId", cachedMachineId)
                            }
                        }
                    } else {
                        ScanQRCodeFragment()
                    }
                }
                R.id.navigation_account -> AccountFragment()
                else -> null
            }
            fragment?.let {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.frameLayout, it)
                    .addToBackStack(null)
                    .commit()
                true
            } ?: false
        }
    }

    private fun openScanQRCodeFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, ScanQRCodeFragment())
            .commit()
    }

    private fun openRecyclerViewMenu(machineId: String) {
        val fragment = RecyclerViewMenuFragment().apply {
            arguments = Bundle().apply {
                putString("machineId", machineId)
            }
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, fragment)
            .commit()
    }

    private fun getCachedMachineId(): String? {
        val sharedPref = getSharedPreferences("easybrew_session", MODE_PRIVATE)
        val raw = sharedPref.getString(CACHE_KEY, null) ?: return null
        
        val cached = try {
            Gson().fromJson(raw, CachedMenu::class.java)
        } catch (_: Exception) {
            return null
        }

        // Check if cache is still valid
        return if (System.currentTimeMillis() - cached.timestamp <= CACHE_TTL_MS) {
            cached.machineId
        } else {
            null
        }
    }
}