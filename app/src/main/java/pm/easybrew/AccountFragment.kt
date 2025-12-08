package pm.easybrew

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import pm.easybrew.databinding.FragmentAccountBinding

class AccountFragment : Fragment() {
    private lateinit var binding: FragmentAccountBinding

    private var token: String? = null

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
            // "no justice for all", clean 'em all
            val sharedPref = requireContext().getSharedPreferences("easybrew_session", MODE_PRIVATE)
            val editor = sharedPref.edit()

            editor.remove("token")

            val allKeys = sharedPref.all.keys.filter { it.startsWith("menu_cache_") }
            allKeys.forEach { key ->
                editor.remove(key)
            }

            editor.apply()

            val intent = Intent(requireContext(), RegisterLoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }
}