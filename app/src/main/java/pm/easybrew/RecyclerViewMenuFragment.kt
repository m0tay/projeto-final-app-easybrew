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
import pm.easybrew.models.Beverage
import pm.easybrew.objects.MenuRequest
import pm.easybrew.objects.ValidateTokenRequest


class RecyclerViewMenuFragment : Fragment() {
    private val beverages = ArrayList<Beverage>()
    private var machineId: String = ""
    private lateinit var adapter: BeveragesAdapter
    private val gson = Gson()

    companion object {
        private const val CACHE_TTL_MS = 10 * 60 * 1000L // 10 minutes
    }

    private data class CachedMenu(val timestamp: Long, val records: List<Beverage>)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_recycler_view_menu, container, false)
        machineId = arguments?.getString("machineId") ?: ""

        setupRecycler(view)

        if (machineId.isNotEmpty()) {
            loadMenu(machineId)
        } else {
            Toast.makeText(requireContext(), "No machine ID provided", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    private fun setupRecycler(view: View) {
        val rv = view.findViewById<RecyclerView>(R.id.recyclerViewMenu)
            ?: run {
                Toast.makeText(requireContext(), "RecyclerView not found (check id)", Toast.LENGTH_LONG).show()
                return
            }
        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = BeveragesAdapter(beverages)
        rv.adapter = adapter
    }

    private fun loadMenu(machineId: String) {
        val cached = loadCache(machineId)
        if (cached != null && (System.currentTimeMillis() - cached.timestamp) <= CACHE_TTL_MS) {
            beverages.clear()
            beverages.addAll(cached.records)
            adapter.notifyDataSetChanged()
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
                    sharedPref.edit().remove("token").apply()
                    Toast.makeText(requireContext(), "Session expired. Please log in again.", Toast.LENGTH_LONG).show()
                    startActivity(Intent(requireContext(), RegisterLoginActivity::class.java))
                    requireActivity().finish()
                    return@launch
                }

                val newToken = validateResp.body()?.jwt ?: token
                if (newToken != token) {
                    sharedPref.edit().putString("token", newToken).apply()
                    token = newToken
                }

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.api.menu(MenuRequest(token!!, machineId))
                }

                if (response.isSuccessful) {
                    val records = response.body()?.records ?: emptyList()
                    beverages.clear()
                    beverages.addAll(records)
                    adapter.notifyDataSetChanged()
                    saveCache(machineId, records)
                } else {
                    Toast.makeText(requireContext(), "Error fetching menu: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cacheKey(machineId: String) = "menu_cache_$machineId"

    private fun saveCache(machineId: String, records: List<Beverage>) {
        val sharedPref = requireActivity().getSharedPreferences("easybrew_session", MODE_PRIVATE)
        val cached = CachedMenu(timestamp = System.currentTimeMillis(), records = records)
        sharedPref.edit { putString(cacheKey(machineId), gson.toJson(cached)) }
    }

    private fun loadCache(machineId: String): CachedMenu? {
        val sharedPref = requireActivity().getSharedPreferences("easybrew_session", MODE_PRIVATE)
        val raw = sharedPref.getString(cacheKey(machineId), null) ?: return null
        return try {
            gson.fromJson(raw, CachedMenu::class.java)
        } catch (_: Exception) {
            null
        }
    }
}