package com.xavi.imageia.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.xavi.imageia.databinding.FragmentNotificationsBinding
//en realidad es compte
class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val notificationsViewModel =
            ViewModelProvider(this).get(NotificationsViewModel::class.java)

        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        //val textView: TextView = binding.textNotifications
        val pasword: EditText = binding.passwordText
        val user: EditText = binding.userText
        val buton: Button = binding.confButton
        notificationsViewModel.text.observe(viewLifecycleOwner) {}
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}