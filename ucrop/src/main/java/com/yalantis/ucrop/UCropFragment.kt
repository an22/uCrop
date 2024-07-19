package com.yalantis.ucrop

import android.graphics.Bitmap.CompressFormat
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IntDef
import androidx.fragment.app.Fragment
import com.yalantis.ucrop.UCrop.Companion.EXTRA_INPUT_URI
import com.yalantis.ucrop.bitmap.BitmapLoadUtils
import com.yalantis.ucrop.bitmap.BitmapReader
import com.yalantis.ucrop.databinding.UcropFragmentPhotoboxBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UCropFragment : Fragment() {

    private var binding:UcropFragmentPhotoboxBinding? = null

    private val bitmapReader = BitmapReader()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = UcropFragmentPhotoboxBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        GlobalScope.launch {
            val bitmap = with(Dispatchers.IO) {
                val output = requireArguments().getParcelable<Uri>(EXTRA_INPUT_URI)!!
                val size = BitmapLoadUtils.calculateMaxBitmapSize(requireContext())
                bitmapReader.readBitmap(requireContext(), output, size, size)
            }
            withContext(Dispatchers.Main) {
                requireNotNull(binding).ucrop.setImageBitmap(bitmap.bitmap)
            }
        }
    }
    companion object {
        const val DEFAULT_COMPRESS_QUALITY: Int = 90
        val DEFAULT_COMPRESS_FORMAT: CompressFormat = CompressFormat.JPEG

        const val NONE: Int = 0
        const val SCALE: Int = 1
        const val ROTATE: Int = 2
        const val ALL: Int = 3

        const val TAG: String = "UCropFragment"

        private const val CONTROLS_ANIMATION_DURATION: Long = 50
        private const val TABS_COUNT = 3
        private const val SCALE_WIDGET_SENSITIVITY_COEFFICIENT = 15000
        private const val ROTATE_WIDGET_SENSITIVITY_COEFFICIENT = 42


        @IntDef(NONE, SCALE, ROTATE, ALL)
        @Retention(AnnotationRetention.SOURCE)
        annotation class GestureTypes

        fun newInstance(args:Bundle):UCropFragment {
            return UCropFragment().apply {
                arguments = args
            }
        }
    }
}

