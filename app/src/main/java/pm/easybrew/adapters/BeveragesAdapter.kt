package pm.easybrew.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
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

        // Picasso.get() for image, not now, Awadama Fever

        holder.beverageBtnMake.setOnClickListener {
            makeBeverage(b, holder.beverageBtnMake)
        }
    }

    override fun getItemCount(): Int {
        return beverages.size
    }

    private fun makeBeverage(beverage: Beverage, button: Button) {
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

                val newToken = validateResp.body()?.jwt ?: token
                if (newToken != token) {
                    sharedPref.edit { putString("token", newToken) }
                    token = newToken
                }

                // Extracting from token user_id
                val jwtPayload = RetrofitClient.getJWTPayload(token!!)


                if (jwtPayload == null) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.invalid_token),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                val request = MakeRequest(
                    jwt = token!!,
                    machine_id = machineId,
                    user_id = jwtPayload["id"] as String,
                    beverage_id = beverage.id,
                    preparation = beverage.preparation
                )

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.api.make(request)
                }

                if (response.isSuccessful) {
                    val body = response.body()
                    Log.i(TAG, body?.message ?: "Beverage prepared success")
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