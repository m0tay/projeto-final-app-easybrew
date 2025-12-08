package pm.easybrew.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import pm.easybrew.R
import pm.easybrew.models.Beverage

class BeveragesAdapter(val beverages: ArrayList<Beverage>): RecyclerView
    .Adapter<BeveragesAdapter.ViewHolder>() {
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
    }

    override fun getItemCount(): Int {
        return beverages.size
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