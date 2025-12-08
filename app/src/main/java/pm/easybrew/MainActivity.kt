package pm.easybrew

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.gson.Gson
import pm.easybrew.databinding.ActivityMainBinding
import pm.easybrew.models.Beverage

class MainActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val gson = Gson()

    companion object {
        private const val CACHE_TTL_MS = 10 * 60 * 1000L // 10 minutes
    }

    private data class CachedMenu(val timestamp: Long, val records: List<Beverage>)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        if (savedInstanceState == null) {
            // valid menu cache?
            val cachedMachineId = getLatestCachedMachineId()
            if (cachedMachineId != null) {
                openRecyclerViewMenu(cachedMachineId)
            } else {
                openScanQRCodeFragment()
            }
        }

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.navigation_menu -> {
                    // Check cache first
                    val cachedMachineId = getLatestCachedMachineId()
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

    private fun getLatestCachedMachineId(): String? {
        val sharedPref = getSharedPreferences("easybrew_session", MODE_PRIVATE)
        val allKeys = sharedPref.all.keys.filter { it.startsWith("menu_cache_") }

        var latestMachineId: String? = null
        var latestTimestamp = 0L

        for (key in allKeys) {
            val raw = sharedPref.getString(key, null) ?: continue
            val cached = try {
                gson.fromJson(raw, CachedMenu::class.java)
            } catch (_: Exception) {
                continue
            }

            if (System.currentTimeMillis() - cached.timestamp <= CACHE_TTL_MS) {
                if (cached.timestamp > latestTimestamp) {
                    latestTimestamp = cached.timestamp
                    latestMachineId = key.removePrefix("menu_cache_")
                }
            }
        }

        return latestMachineId
    }
}