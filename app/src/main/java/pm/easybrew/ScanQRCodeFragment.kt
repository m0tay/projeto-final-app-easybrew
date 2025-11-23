package pm.easybrew

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class ScanQRCodeFragment : Fragment() {

    private var token: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // token = arguments?.getString("jwt")
        token = requireContext()
            .getSharedPreferences("easybrew_session", MODE_PRIVATE)
            .getString("token", null)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_scan_qr_code, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        view.findViewById<TextView>(R.id.scanTextView).text = "Menu $token"
    }
}