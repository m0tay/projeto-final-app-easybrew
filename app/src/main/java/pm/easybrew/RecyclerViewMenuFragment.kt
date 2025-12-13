package pm.easybrew

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pm.easybrew.adapters.BeveragesAdapter
import pm.easybrew.api.RetrofitClient
import pm.easybrew.objects.Beverage
import pm.easybrew.objects.MenuRequest
import pm.easybrew.objects.ValidateTokenRequest


class RecyclerViewMenuFragment : Fragment() {
    private val beverages = ArrayList<Beverage>()
    private var machineId: String = ""
    private lateinit var adapter: BeveragesAdapter

    companion object {
        private const val CACHE_TTL_MS = 10 * 60 * 1000L // 10 minutes
        private const val CACHE_KEY = "last_menu_cache"
    }

    private data class CachedMenu(val timestamp: Long, val machineId: String, val records: List<Beverage>)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_recycler_view_menu, container, false)
        machineId = arguments?.getString("machineId") ?: ""

        val rv = view.findViewById<RecyclerView>(R.id.recyclerViewMenu)
        rv?.layoutManager = LinearLayoutManager(requireContext())
        adapter = BeveragesAdapter(beverages)
        rv?.adapter = adapter

        if (machineId.isNotEmpty()) {
            loadMenu(machineId)
        } else {
            Toast.makeText(requireContext(), "No machine ID provided", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    private fun loadMenu(machineId: String) {
        val cached = loadCache()
        if (cached != null &&
            cached.machineId == machineId &&
            (System.currentTimeMillis() - cached.timestamp) <= CACHE_TTL_MS) {
            updateBeverageList(cached.records)
            return
        }

        // Fetch fresh menu instead, Sis. Anger
        val sharedPref = requireActivity().getSharedPreferences("easybrew_session", MODE_PRIVATE)
        var token = sharedPref.getString("token", null)

        if (token.isNullOrBlank()) {
            Toast.makeText(requireContext(), "No token found", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val validateResp = withContext(Dispatchers.IO) {
                    RetrofitClient.api.validateToken(ValidateTokenRequest(token!!))
                }
                if (!validateResp.isSuccessful) {
                    // invalid -> clear and redirect to login
                    sharedPref.edit { remove("token") }
                    Toast.makeText(requireContext(), "Session expired. Please log in again.", Toast.LENGTH_LONG).show()
                    startActivity(Intent(requireContext(), RegisterLoginActivity::class.java))
                    requireActivity().finish()
                    return@launch
                }

                val newToken = validateResp.body()?.jwt ?: token
                if (newToken != token) {
                    sharedPref.edit { putString("token", newToken) }
                    token = newToken
                }

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.api.menu(MenuRequest(token!!, machineId))
                }

                if (response.isSuccessful) {
                    val records = response.body()?.records ?: emptyList()
                    updateBeverageList(records)
                    saveCache(machineId, records)
                } else {
                    Toast.makeText(requireContext(), "Error fetching menu: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateBeverageList(newBeverages: List<Beverage>) {
        val oldSize = beverages.size
        beverages.clear()

        if (oldSize > 0) {
            adapter.notifyItemRangeRemoved(0, oldSize)
        }

        beverages.addAll(newBeverages)

        if (newBeverages.isNotEmpty()) {
            adapter.notifyItemRangeInserted(0, newBeverages.size)
        }
    }

    private fun saveCache(machineId: String, records: List<Beverage>) {
        val sharedPref = requireActivity().getSharedPreferences("easybrew_session", MODE_PRIVATE)
        val cached = CachedMenu(
            timestamp = System.currentTimeMillis(), 
            machineId = machineId, 
            records = records
        )
        sharedPref.edit { putString(CACHE_KEY, Gson().toJson(cached)) }
    }

    private fun loadCache(): CachedMenu? {
        val sharedPref = requireActivity().getSharedPreferences("easybrew_session", MODE_PRIVATE)
        val raw = sharedPref.getString(CACHE_KEY, null) ?: return null
        return try {
            Gson().fromJson(raw, CachedMenu::class.java)
        } catch (_: Exception) {
            null
        }
    }
}