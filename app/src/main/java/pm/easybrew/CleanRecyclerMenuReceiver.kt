package pm.easybrew

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.edit

class CleanRecyclerMenuReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val sp = context!!.getSharedPreferences("easybrew_session", Context
            .MODE_PRIVATE)
        sp.edit { remove("last_menu_cache") }

        Toast.makeText(context, context.getString(R.string.cached_cleared), Toast
            .LENGTH_SHORT)
            .show()
    }
}