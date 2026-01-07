package pm.easybrew

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import pm.easybrew.api.RetrofitClient
import pm.easybrew.databinding.FragmentAccountBinding
import pm.easybrew.objects.AddBalanceRequest
import pm.easybrew.objects.ValidateTokenRequest
import java.io.IOException

class AccountFragment : Fragment() {
    private lateinit var binding: FragmentAccountBinding

    private var token: String? = null

    companion object {
        private val TAG = AccountFragment::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // token = arguments?.getString("jwt")
        token = requireContext()
            .getSharedPreferences("easybrew_session", MODE_PRIVATE)
            .getString("token", null)
//
//        requireContext().deleteSharedPreferences("easybrew_session")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLogout.setOnClickListener {
            val sharedPref = requireContext().getSharedPreferences("easybrew_session", MODE_PRIVATE)
            sharedPref.edit {
                remove("token")
                remove("last_menu_cache")
            }

            val intent = Intent(requireContext(), RegisterLoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }

        binding.btnUpdateAccount.setOnClickListener {
            val valueText = binding.editAddBalance.text.toString()

            // Validação: campo vazio
            if (valueText.isBlank()) {
                Toast.makeText(context, R.string.noValueProvided, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validação: formato numérico válido
            val valueToAddToBalance = valueText.toDoubleOrNull()
            if (valueToAddToBalance == null) {
                Toast.makeText(context, getString(R.string.invalid_value), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validação: valor deve ser positivo
            if (valueToAddToBalance <= 0) {
                Toast.makeText(context, getString(R.string.value_must_be_positive), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Chamar API para adicionar fundos
            lifecycleScope.launch {
                try {
                    val request = AddBalanceRequest(
                        jwt = token!!,
                        type = "deposit",
                        amount = valueToAddToBalance,
                        status = "completed"
                    )
                    
                    val response = RetrofitClient.api.addBalance(request)
                    
                    if (response.isSuccessful) {
                        Log.i(TAG, "Funds added successfully: ${response.body()?.message}")
                        
                        // Revalidar token para obter balance atualizado
                        val validateResponse = RetrofitClient.api.validateToken(ValidateTokenRequest(token!!))
                        
                        if (validateResponse.isSuccessful) {
                            val newToken = validateResponse.body()?.jwt
                            val tokenToUse = newToken ?: token!!
                            val jwtPayload = RetrofitClient.getJWTPayload(tokenToUse)
                            
                            if (jwtPayload != null) {
                                val sharedPref = requireContext().getSharedPreferences("easybrew_session", MODE_PRIVATE)
                                sharedPref.edit {
                                    if (newToken != null) {
                                        putString("token", newToken)
                                    }
                                    putString("balance", jwtPayload["balance"] as? String)
                                }
                                
                                Toast.makeText(context, getString(R.string.balance_added_successfully), Toast.LENGTH_SHORT).show()
                                binding.editAddBalance.text?.clear()
                                
                                // Atualizar balance na MainActivity diretamente
                                (activity as? MainActivity)?.updateBalance()
                            }
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = errorBody ?: getString(R.string.error_adding_balance)
                        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                        Log.w(TAG, "Error adding funds: $errorMessage")
                    }
                } catch (e: IOException) {
                    Toast.makeText(context, getString(R.string.network_error), Toast.LENGTH_SHORT).show()
                    Log.e(TAG, "Network error", e)
                } catch (e: Exception) {
                    Toast.makeText(context, getString(R.string.an_error_occured) + "${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e(TAG, e.message ?: "Unknown error", e)
                }
            }
        }
    }
}