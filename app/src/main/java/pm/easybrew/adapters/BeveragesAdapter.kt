package pm.easybrew.adapters

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.edit
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pm.easybrew.MainActivity
import pm.easybrew.R
import pm.easybrew.api.RetrofitClient
import pm.easybrew.objects.Beverage
import pm.easybrew.objects.MakeRequest
import pm.easybrew.objects.ValidateTokenRequest
import java.io.IOException

class BeveragesAdapter(
    val beverages: ArrayList<Beverage>,
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val machineId: String
): RecyclerView.Adapter<BeveragesAdapter.ViewHolder>() {
    
    companion object {
        private val TAG = BeveragesAdapter::class.java.simpleName
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_beverage, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        val b = beverages[position]
        holder.beverageName.text = b.name
        holder.beverageDescription.text = b.description
        holder.beveragePreparation.text = b.preparation
        holder.beveragePrice.text = "${b.price} €"

        holder.beverageBtnMake.setOnClickListener {
            val sharedPref = context.getSharedPreferences("easybrew_session", Context.MODE_PRIVATE)
            val balanceStr = sharedPref.getString("balance", "0.0")
            val balance = balanceStr?.toDoubleOrNull() ?: 0.0
            
            if (balance < b.price) {
                Toast.makeText(context, context.getString(R.string.insufficient_balance), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            showPreparationDialog(b)
        }
    }

    override fun getItemCount(): Int {
        return beverages.size
    }

    private fun showPreparationDialog(beverage: Beverage) {
        val preparations = beverage.preparation.split(",").map { it.trim() }
        
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_preparation_selection, null)
        val radioGroup = dialogView.findViewById<RadioGroup>(R.id.radioGroupPreparation)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirmOrder)
        val txtBeverageName = dialogView.findViewById<TextView>(R.id.txtBeverageName)
        val txtBeveragePrice = dialogView.findViewById<TextView>(R.id.txtBeveragePrice)
        
        txtBeverageName.text = beverage.name
        txtBeveragePrice.text = "${beverage.price} €"
        
        var selectedPreparation = "hot"
        preparations.forEach { prep ->
            val radioButton = RadioButton(context)
            radioButton.text = getPreparationTranslation(prep)
            radioButton.tag = prep
            radioButton.compoundDrawablePadding = 16
            
            val iconRes = getPreparationIcon(prep)
            if (iconRes != null) {
                radioButton.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0)
            }
            
            radioGroup.addView(radioButton)
            
            if (prep == "hot" || (selectedPreparation == "hot" && radioGroup.childCount == 1)) {
                radioButton.isChecked = true
                selectedPreparation = prep
            }
            
            radioButton.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedPreparation = prep
                }
            }
        }
        
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        btnConfirm.setOnLongClickListener {
            btnConfirm.isEnabled = false
            makeBeverage(beverage, selectedPreparation, btnConfirm)
            dialog.dismiss()
            true
        }
        
        dialog.show()
    }

    private fun getPreparationTranslation(preparation: String): String {
        return when (preparation.lowercase()) {
            "hot" -> context.getString(R.string.preparation_hot)
            "warm" -> context.getString(R.string.preparation_warm)
            "iced" -> context.getString(R.string.preparation_iced)
            "cold" -> context.getString(R.string.preparation_cold)
            else -> preparation.capitalize()
        }
    }

    private fun getPreparationIcon(preparation: String): Int? {
        return when (preparation.lowercase()) {
            "hot", "warm" -> R.drawable.temp_hot
            "iced", "cold" -> R.drawable.temp_iced
            else -> null
        }
    }

    private fun makeBeverage(beverage: Beverage, preparation: String, button: Button) {
        val sharedPref = context.getSharedPreferences("easybrew_session", Context.MODE_PRIVATE)
        var token = sharedPref.getString("token", null)

        if (token.isNullOrBlank()) {
            Toast.makeText(context, context.getString(R.string.no_token_found), Toast.LENGTH_SHORT).show()
            return
        }

        button.isEnabled = false
        val originalText = button.text
        button.text = context.getString(R.string.preparing)

        lifecycleScope.launch {
            try {
                val validateResp = withContext(Dispatchers.IO) {
                    RetrofitClient.api.validateToken(ValidateTokenRequest(token!!))
                }

                if (!validateResp.isSuccessful) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.session_expired),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                val newToken = validateResp.body()?.jwt
                if (newToken != null && newToken != token) {
                    sharedPref.edit { putString("token", newToken) }
                    token = newToken
                }

                val tokenToUse = newToken ?: token
                val jwtPayload = RetrofitClient.getJWTPayload(tokenToUse!!)

                if (jwtPayload == null) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.invalid_token),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }
                
                sharedPref.edit {
                    putString("user_id", jwtPayload["id"] as? String)
                    putString("balance", jwtPayload["balance"] as? String)
                    putString("first_name", jwtPayload["first_name"] as? String)
                }

                val request = MakeRequest(
                    jwt = tokenToUse!!,
                    machine_id = machineId,
                    user_id = jwtPayload["id"] as String,
                    beverage_id = beverage.id,
                    preparation = preparation
                )

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.api.machineMake(request)
                }

                if (response.isSuccessful) {
                    val body = response.body()
                    Log.i(TAG, body?.message ?: "Beverage prepared success")
                    
                    val newBalance = body?.new_balance
                    if (newBalance != null) {
                        sharedPref.edit {
                            putString("balance", newBalance.toString())
                        }
                        
                        if (context is MainActivity) {
                            context.updateBalance()
                        }
                    }
                    
                    Toast.makeText(
                        context,
                        body?.message ?: context.getString(R.string.beverage_prepared_successfully),
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = try {
                        val json = Gson().fromJson(errorBody, Map::class.java)
                        json["message"] as? String ?: context.getString(R.string.failed_to_prepare_beverage)
                    } catch (e: Exception) {
                        context.getString(R.string.failed_to_prepare_beverage)
                    }
                    Log.w(TAG, errorMessage)
                    Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                }
            } catch (e: IOException) {
                Toast.makeText(
                    context,
                    context.getString(R.string.network_error),
                    Toast.LENGTH_SHORT
                ).show()
                Log.e(TAG, "Network error", e)
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    context.getString(R.string.an_error_occured) + "${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                Log.e(TAG, e.message ?: "Unknown error", e)
            } finally {
                button.isEnabled = true
                button.text = originalText
            }
        }
    }

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        var beverageCard: CardView = itemView.findViewById(R.id.cardView)
        var beverageName: TextView = itemView.findViewById<TextView>(R.id
            .beverageName)
        var beverageDescription: TextView = itemView.findViewById<TextView>(R.id
        .beverageDescription)
        var beveragePreparation: TextView = itemView.findViewById<TextView>(R.id
        .beveragePreparation)
        var beveragePrice: TextView = itemView.findViewById<TextView>(R.id
            .beveragePrice)
        var beverageImage: ImageView = itemView.findViewById<ImageView>(R.id
            .beverageImageView)
        var beverageBtnMake: Button = itemView.findViewById<Button>(R.id
            .btnMake)
    }
}