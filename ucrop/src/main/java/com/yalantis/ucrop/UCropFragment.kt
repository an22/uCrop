package com.yalantis.ucrop

import android.graphics.Bitmap.CompressFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.yalantis.ucrop.UCrop.Companion.EXTRA_INPUT_URI
import com.yalantis.ucrop.UCrop.Companion.EXTRA_OUTPUT_URI
import com.yalantis.ucrop.UCrop.Options.Companion.EXTRA_ASPECT_RATIO_SELECTED_BY_DEFAULT
import com.yalantis.ucrop.UCropActivity.ALL
import com.yalantis.ucrop.UCropActivity.ROTATE
import com.yalantis.ucrop.UCropActivity.SCALE
import com.yalantis.ucrop.bitmap.BitmapLoadUtils
import com.yalantis.ucrop.bitmap.BitmapReader
import com.yalantis.ucrop.databinding.UcropFragmentPhotoboxBinding
import com.yalantis.ucrop.model.AspectRatio
import com.yalantis.ucrop.view.GestureCropImageView
import com.yalantis.ucrop.view.drawable.CropOverlayDrawable
import com.yalantis.ucrop.view.drawable.CropOverlayDrawable.ElementConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UCropFragment : Fragment() {

    private var binding: UcropFragmentPhotoboxBinding? = null

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
        with(requireArguments()) {
            val imageView = requireNotNull(binding).ucrop
            parseImageProps()
            parseUISettings()
            parseCompressionSettings(imageView)
            parseImageTransformSettings(imageView)
            parseCropAreaSettings(imageView)
            parseCropSettings(imageView)
        }
    }

    private fun Bundle.parseImageProps() {
        val inputUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelable(EXTRA_INPUT_URI, Uri::class.java)
        } else {
            getParcelable(EXTRA_INPUT_URI)
        }
        val outputUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelable(EXTRA_OUTPUT_URI, Uri::class.java)
        } else {
            getParcelable(EXTRA_OUTPUT_URI)
        }
        GlobalScope.launch {
            val bitmap = with(Dispatchers.IO) {
                val size = BitmapLoadUtils.calculateMaxBitmapSize(requireContext())
                bitmapReader.readBitmap(requireContext(), inputUri!!, size, size)
            }
            withContext(Dispatchers.Main) {
                requireNotNull(binding).ucrop.setImageBitmap(bitmap.bitmap)
            }
        }
    }

    private fun Bundle.parseCompressionSettings(view: GestureCropImageView) {
        if (containsKey(UCrop.Options.EXTRA_COMPRESSION_FORMAT_NAME)) {
            val format =
                CompressFormat.valueOf(getString(UCrop.Options.EXTRA_COMPRESSION_FORMAT_NAME).orEmpty())
        }
        if (containsKey(UCrop.Options.EXTRA_COMPRESSION_QUALITY)) {
            val quality = getInt(UCrop.Options.EXTRA_COMPRESSION_QUALITY)
        }
    }

    private fun Bundle.parseImageTransformSettings(view: GestureCropImageView) {
        if (containsKey(UCrop.Options.EXTRA_ALLOWED_GESTURES)) {
            val allowedGestures =
                getIntArray(UCrop.Options.EXTRA_ALLOWED_GESTURES)?.toList().orEmpty()
            view.isScaleEnabled = allowedGestures.contains(ALL) || allowedGestures.contains(SCALE)
            view.isRotateEnabled = allowedGestures.contains(ALL) || allowedGestures.contains(ROTATE)
        }
        if (containsKey(UCrop.Options.EXTRA_MAX_SCALE_MULTIPLIER)) {
            val maxScaleMultiplier = getFloat(UCrop.Options.EXTRA_MAX_SCALE_MULTIPLIER)
            view.maxScaleMultiplier = maxScaleMultiplier
        }
        if (containsKey(UCrop.Options.EXTRA_MAX_BITMAP_SIZE)) {
            val maxBitmapSize = getInt(UCrop.Options.EXTRA_MAX_BITMAP_SIZE)
        }
        if (containsKey(UCrop.Options.EXTRA_IMAGE_TO_CROP_BOUNDS_ANIM_DURATION)) {
            val boundsAnimDuration = getInt(UCrop.Options.EXTRA_IMAGE_TO_CROP_BOUNDS_ANIM_DURATION)
            view.imageToCropBoundsAnimDuration = boundsAnimDuration.toLong()
        }
        if (containsKey(UCrop.Options.EXTRA_DOUBLE_TAP_ANIM_DURATION)) {
            val doubleTapAnimDuration = getInt(UCrop.Options.EXTRA_DOUBLE_TAP_ANIM_DURATION)
            view.doubleTapAnimDuration = doubleTapAnimDuration.toLong()
        }
    }

    private fun Bundle.parseCropAreaSettings(view: GestureCropImageView) {
        if (containsKey(UCrop.Options.EXTRA_DIMMED_LAYER_COLOR)) {
            val dimColor = getInt(UCrop.Options.EXTRA_DIMMED_LAYER_COLOR)
            view.dimColor = dimColor
        }
        if (containsKey(UCrop.Options.EXTRA_CIRCLE_DIMMED_LAYER)) {
            val isCircleDimmed = getBoolean(UCrop.Options.EXTRA_CIRCLE_DIMMED_LAYER)
            view.cropAreaMode = when (isCircleDimmed) {
                true -> CropOverlayDrawable.CropAreaMode.CIRCLE
                else -> CropOverlayDrawable.CropAreaMode.RECTANGLE
            }
        }
        if (containsKey(UCrop.Options.EXTRA_SHOW_CROP_FRAME)) {
            val isCropFrameVisible = getBoolean(UCrop.Options.EXTRA_SHOW_CROP_FRAME)
            view.shouldDrawOverlay = isCropFrameVisible
        }
        val frameConfigBuilder = ElementConfig.Builder()
        if (containsKey(UCrop.Options.EXTRA_CROP_FRAME_COLOR)) {
            val cropFrameColor = getInt(UCrop.Options.EXTRA_CROP_FRAME_COLOR)
            frameConfigBuilder.color = cropFrameColor
        }
        if (containsKey(UCrop.Options.EXTRA_CROP_FRAME_STROKE_WIDTH)) {
            val cropFrameStrokeWidth = getInt(UCrop.Options.EXTRA_CROP_FRAME_STROKE_WIDTH)
            frameConfigBuilder.strokeWidth = cropFrameStrokeWidth.toFloat()
        }
        val gridConfigBuilder = ElementConfig.Builder()
        if (containsKey(UCrop.Options.EXTRA_SHOW_CROP_GRID)) {
            val showCropGrid = getBoolean(UCrop.Options.EXTRA_SHOW_CROP_GRID)
            gridConfigBuilder.isVisible = showCropGrid
        }
        if (containsKey(UCrop.Options.EXTRA_CROP_GRID_ROW_COUNT)) {
            val rowCount = getInt(UCrop.Options.EXTRA_CROP_GRID_ROW_COUNT)
            view.gridRowCount = rowCount
        }
        if (containsKey(UCrop.Options.EXTRA_CROP_GRID_COLUMN_COUNT)) {
            val columnCount = getInt(UCrop.Options.EXTRA_CROP_GRID_COLUMN_COUNT)
            view.gridColumnCount = columnCount
        }
        if (containsKey(UCrop.Options.EXTRA_CROP_GRID_COLOR)) {
            val gridColor = getInt(UCrop.Options.EXTRA_CROP_GRID_COLOR)
            gridConfigBuilder.color = gridColor
        }
        val cornerConfig = ElementConfig.Builder()
        if (containsKey(UCrop.Options.EXTRA_CROP_GRID_CORNER_COLOR)) {
            val gridCornerColor = getInt(UCrop.Options.EXTRA_CROP_GRID_CORNER_COLOR)
            cornerConfig.color = gridCornerColor
        }
        if (containsKey(UCrop.Options.EXTRA_CROP_GRID_STROKE_WIDTH)) {
            val gridStrokeWidth = getInt(UCrop.Options.EXTRA_CROP_GRID_STROKE_WIDTH)
            cornerConfig.strokeWidth = gridStrokeWidth.toFloat()
        }
        view.frameConfig = frameConfigBuilder.build()
        view.frameGridConfig = gridConfigBuilder.build()
        view.frameCornerConfig = cornerConfig.build()
    }

    private fun Bundle.parseUISettings() {
        if (containsKey(UCrop.Options.EXTRA_TOOL_BAR_COLOR)) {
            val toolbarColor = getInt(UCrop.Options.EXTRA_TOOL_BAR_COLOR)
        }
        if (containsKey(UCrop.Options.EXTRA_STATUS_BAR_COLOR)) {
            val statusBarColor = getInt(UCrop.Options.EXTRA_STATUS_BAR_COLOR)
        }
        if (containsKey(UCrop.Options.EXTRA_UCROP_COLOR_CONTROLS_WIDGET_ACTIVE)) {
            val controlsColor = getInt(UCrop.Options.EXTRA_UCROP_COLOR_CONTROLS_WIDGET_ACTIVE)
        }
        if (containsKey(UCrop.Options.EXTRA_UCROP_WIDGET_COLOR_TOOLBAR)) {
            val toolbarWidgetColor = getInt(UCrop.Options.EXTRA_UCROP_WIDGET_COLOR_TOOLBAR)
        }
        if (containsKey(UCrop.Options.EXTRA_UCROP_WIDGET_CANCEL_DRAWABLE)) {
            val cancelResId = getInt(UCrop.Options.EXTRA_UCROP_WIDGET_CANCEL_DRAWABLE)
        }
        if (containsKey(UCrop.Options.EXTRA_UCROP_WIDGET_CROP_DRAWABLE)) {
            val cropDrawable = getInt(UCrop.Options.EXTRA_UCROP_WIDGET_CROP_DRAWABLE)
        }
        if (containsKey(UCrop.Options.EXTRA_UCROP_LOGO_COLOR)) {
            val logoColor = getInt(UCrop.Options.EXTRA_UCROP_LOGO_COLOR)
        }
        if (containsKey(UCrop.Options.EXTRA_HIDE_BOTTOM_CONTROLS)) {
            val hideControls = getBoolean(UCrop.Options.EXTRA_HIDE_BOTTOM_CONTROLS)
        }
        if (containsKey(UCrop.Options.EXTRA_UCROP_ROOT_VIEW_BACKGROUND_COLOR)) {
            val bgColor = getInt(UCrop.Options.EXTRA_UCROP_ROOT_VIEW_BACKGROUND_COLOR)
        }
    }

    private fun Bundle.parseCropSettings(view: GestureCropImageView) {
        if (containsKey(UCrop.Options.EXTRA_FREE_STYLE_CROP)) {
            val isFreestyleCrop = getBoolean(UCrop.Options.EXTRA_FREE_STYLE_CROP)
            view.isFreestyleCrop = isFreestyleCrop
        }
        if (containsKey(UCrop.Options.EXTRA_ASPECT_RATIO_OPTIONS)) {
            val aspectRatios = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getParcelableArrayList(
                    UCrop.Options.EXTRA_ASPECT_RATIO_OPTIONS,
                    AspectRatio::class.java
                )
            } else {
                getParcelableArrayList(UCrop.Options.EXTRA_ASPECT_RATIO_OPTIONS)
            }
            val selectedIndex = getInt(EXTRA_ASPECT_RATIO_SELECTED_BY_DEFAULT)
        }
        if (containsKey(UCrop.EXTRA_ASPECT_RATIO_X) && containsKey(UCrop.EXTRA_ASPECT_RATIO_Y)) {
            val aspectX = getFloat(UCrop.EXTRA_ASPECT_RATIO_X)
            val aspectY = getFloat(UCrop.EXTRA_ASPECT_RATIO_Y)
            val aspectRatio = aspectX / aspectY
            view.aspectRatio = aspectRatio
        }
        if (containsKey(UCrop.EXTRA_MAX_SIZE_X) && containsKey(UCrop.EXTRA_MAX_SIZE_Y)) {
            val maxSizeX = getFloat(UCrop.EXTRA_ASPECT_RATIO_X)
            val maxSizeY = getFloat(UCrop.EXTRA_ASPECT_RATIO_Y)
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    companion object {
        const val DEFAULT_COMPRESS_QUALITY: Int = 90
        val DEFAULT_COMPRESS_FORMAT: CompressFormat = CompressFormat.JPEG

        const val TAG: String = "UCropFragment"

        private const val CONTROLS_ANIMATION_DURATION: Long = 50
        private const val TABS_COUNT = 3
        private const val SCALE_WIDGET_SENSITIVITY_COEFFICIENT = 15000
        private const val ROTATE_WIDGET_SENSITIVITY_COEFFICIENT = 42

        fun newInstance(args: Bundle): UCropFragment {
            return UCropFragment().apply {
                arguments = args
            }
        }
    }
}

