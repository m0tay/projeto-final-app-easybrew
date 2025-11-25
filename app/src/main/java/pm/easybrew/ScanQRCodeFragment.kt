package pm.easybrew

import android.content.Context.MODE_PRIVATE
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import pm.easybrew.databinding.FragmentScanQrCodeBinding

class ScanQRCodeFragment : Fragment(R.layout.fragment_scan_qr_code) {

    private lateinit var binding: FragmentScanQrCodeBinding

    private var token: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // token = arguments?.getString("jwt")
        token = requireContext()
            .getSharedPreferences("easybrew_session", MODE_PRIVATE)
            .getString("token", null)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding = FragmentScanQrCodeBinding.bind(view)

//        view.findViewById<TextView>(R.id.scanTextView).text = "Menu $token"
        binding.scanTextView.text = "Menu $token"
    }
}