package com.i2hammad.admanagekit.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.i2hammad.admanagekit.sample.R

class SecondFragment : Fragment() {

    interface NavigationListener {
        fun onNavigateBack()
    }

    private var navigationListener: NavigationListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_second, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.btnGoBack).setOnClickListener {
            navigationListener?.onNavigateBack()
        }
    }

    fun setNavigationListener(listener: NavigationListener) {
        this.navigationListener = listener
    }

    companion object {
        fun newInstance(): SecondFragment {
            return SecondFragment()
        }
    }
}
