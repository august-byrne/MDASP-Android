package com.augustbyrne.mdaspcompanion

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import com.augustbyrne.mdaspcompanion.databinding.FragmentFirstBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */

@AndroidEntryPoint
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val myViewModel: AppViewModel by viewModels()

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)

        binding.buttonFirst.setOnClickListener {
            myViewModel.toggleConnectionStatus()
            myViewModel.addItem()
        }

        myViewModel.myLiveThing.observe(viewLifecycleOwner) {
            binding.growingList.adapter = ArrayAdapter(requireContext(), R.layout.basic_list_item, it)
        }

        myViewModel.connectionStatus.observe(viewLifecycleOwner) { btConnectionStatus ->
            binding.connectionStatus.text = if (btConnectionStatus) "Connected" else "Disconnected"
        }

        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

/*        binding.buttonFirst.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }*/
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}