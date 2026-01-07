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
import pm.easybrew.databinding.FragmentRecyclerViewMenuBinding
import pm.easybrew.objects.Beverage
import pm.easybrew.objects.MenuRequest
import pm.easybrew.objects.ValidateTokenRequest


class RecyclerViewMenuFragment : Fragment() {
    private val beverages = ArrayList<Beverage>()
    private var machineId: String = ""
    private lateinit var adapter: BeveragesAdapter
    private lateinit var binding: FragmentRecyclerViewMenuBinding

    companion object {
        private const val CACHE_TTL_MS = 10 * 60 * 1000L // 10 minutes
        private const val CACHE_KEY = "last_menu_cache"
    }

    private data class CachedMenu(
        val timestamp: Long,
        val machineId: String,
        val records: List<Beverage>
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_recycler_view_menu, container, false)
        machineId = arguments?.getString("machineId") ?: ""

        val rv = view.findViewById<RecyclerView>(R.id.recyclerViewMenu)
        rv?.layoutManager = LinearLayoutManager(requireContext())
        adapter = BeveragesAdapter(beverages, requireContext(), viewLifecycleOwner.lifecycleScope, machineId)
        rv?.adapter = adapter

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentRecyclerViewMenuBinding.bind(view)

        binding.btnRescan.setOnLongClickListener {
            if (!isAdded || activity == null || context == null) return@setOnLongClickListener true

            // muss das so sein?
            val intent = Intent(requireContext(), CleanRecyclerMenuReceiver::class.java)
            requireContext().sendBroadcast(intent)
            val activity = requireActivity()
            activity.supportFragmentManager.popBackStackImmediate()
            activity.supportFragmentManager
                .beginTransaction()
                .replace(R.id.frameLayout, ScanQRCodeFragment())
                .commit()

            true
        }

        if (machineId.isNotEmpty()) {
            viewLifecycleOwner.lifecycleScope.launch {
                loadMenu(machineId)
            }
        } else {
            if (isAdded && context != null) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.no_machine_id),
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        }
    }

    private suspend fun loadMenu(machineId: String) {
        if (!isAdded || context == null) return

        val cached = loadCache()
        if (cached != null &&
            cached.machineId == machineId &&
            (System.currentTimeMillis() - cached.timestamp) <= CACHE_TTL_MS
        ) {
            updateBeverageList(cached.records)
            return
        }

        // Fetch fresh menu instead, Sis. Anger
        if (!isAdded || activity == null) return
        val sharedPref = requireActivity().getSharedPreferences("easybrew_session", MODE_PRIVATE)
        var token = sharedPref.getString("token", null)

        if (token.isNullOrBlank()) {
            if (isAdded && context != null) {
                Toast.makeText(requireContext(), getString(R.string.no_token_found), Toast.LENGTH_SHORT)
                    .show()
            }
            return
        }

        try {
            val validateResp = withContext(Dispatchers.IO) {
                RetrofitClient.api.validateToken(ValidateTokenRequest(token))
            }

            if (!isAdded || context == null) return

            if (!validateResp.isSuccessful) {
                // invalid -> clear and redirect to login
                sharedPref.edit { 
                    remove("token")
                    remove("user_id")
                    remove("balance")
                    remove("first_name")
                }
                Toast.makeText(
                    requireContext(),
                    getString(R.string.session_expired),
                    Toast.LENGTH_LONG
                ).show()
                startActivity(Intent(requireContext(), RegisterLoginActivity::class.java))
                requireActivity().finish()
                return
            }

            // Atualizar token se retornou um novo
            val newToken = validateResp.body()?.jwt
            if (newToken != null && newToken != token) {
                sharedPref.edit { putString("token", newToken) }
                token = newToken
            }
            
            // Atualizar dados do usuário do novo token
            val tokenToUse = newToken ?: token
            val jwtPayload = RetrofitClient.getJWTPayload(tokenToUse)
            if (jwtPayload != null) {
                sharedPref.edit {
                    putString("user_id", jwtPayload["id"] as? String)
                    putString("balance", jwtPayload["balance"] as? String)
                    putString("first_name", jwtPayload["first_name"] as? String)
                }
            }

            val response = withContext(Dispatchers.IO) {
                RetrofitClient.api.menu(MenuRequest(token, machineId))
            }

            if (!isAdded || context == null) return

            if (response.isSuccessful) {
                val records = response.body()?.records ?: emptyList()
                updateBeverageList(records)
                saveCache(machineId, records)
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.failed_to_fetch_menu) + "${response.code()}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            if (isAdded && context != null) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.network_error) + "${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
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
        if (!isAdded || activity == null) return
        val sharedPref = requireActivity().getSharedPreferences("easybrew_session", MODE_PRIVATE)
        val cached = CachedMenu(
            timestamp = System.currentTimeMillis(),
            machineId = machineId,
            records = records
        )
        sharedPref.edit { putString(CACHE_KEY, Gson().toJson(cached)) }
    }

    private fun loadCache(): CachedMenu? {
        if (!isAdded || activity == null) return null
        val sharedPref = requireActivity().getSharedPreferences("easybrew_session", MODE_PRIVATE)
        val raw = sharedPref.getString(CACHE_KEY, null) ?: return null
        return try {
            Gson().fromJson(raw, CachedMenu::class.java)
        } catch (_: Exception) {
            null
        }
    }
}