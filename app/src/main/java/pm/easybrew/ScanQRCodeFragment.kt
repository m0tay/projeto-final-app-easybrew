package pm.easybrew

import android.Manifest
import android.content.Context.MODE_PRIVATE
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import pm.easybrew.databinding.FragmentScanQrCodeBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanQRCodeFragment : Fragment(R.layout.fragment_scan_qr_code) {

    private lateinit var binding: FragmentScanQrCodeBinding

    private var token: String? = null

    private var machineId: String? = null

    private lateinit var cameraExecutor: ExecutorService
    private var camera: Camera? = null

    private val barcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
    )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(
                requireContext(),
                // trocar ts para i18n later, later ist noch
                getString(R.string.camera_permission_is_required), Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // token = arguments?.getString("jwt")
        token = requireContext().getSharedPreferences("easybrew_session", MODE_PRIVATE)
            .getString("token", null)

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentScanQrCodeBinding.bind(view)

        // Check camera permission and start camera
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, QRCodeAnalyzer())
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {

                cameraProvider.unbindAll()

                camera = cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
                Toast.makeText(
                    requireContext(),
                    getString(R.string.failed_to_start_camera),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private inner class QRCodeAnalyzer : ImageAnalysis.Analyzer {
        private var lastAnalyzedTimestamp = 0L
        private var isProcessing = false
        @androidx.annotation.OptIn(ExperimentalGetImage::class)
        override fun analyze(imageProxy: ImageProxy) {
            val currentTimestamp = System.currentTimeMillis()


            if (currentTimestamp - lastAnalyzedTimestamp < 500 || isProcessing) {
                imageProxy.close()
                return
            }

            lastAnalyzedTimestamp = currentTimestamp
            isProcessing = true
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )
                barcodeScanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            barcode.rawValue?.let { qrCodeValue ->
                                Log.d(TAG, "QR Code detected: $qrCodeValue")
                                handleQRCode(qrCodeValue)
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Barcode scanning failed", e)
                    }
                    .addOnCompleteListener {
                        isProcessing = false
                        imageProxy.close()
                    }
            } else {
                isProcessing = false
                imageProxy.close()
            }
        }
    }

    private fun handleQRCode(qrCodeValue: String) {
        // Check if fragment is still attached before proceeding
        if (!isAdded) {
            return
        }
        
        val activity = activity ?: return
        
        activity.runOnUiThread {
            // Double-check attachment on UI thread
            if (!isAdded) {
                return@runOnUiThread
            }
            
            binding.scanTextView.text = "Scanned: $qrCodeValue"
            Log.d(TAG, "QR Code detected: $qrCodeValue")

            val fragment = RecyclerViewMenuFragment().apply {
                arguments = Bundle().apply {
                    putString("machineId", qrCodeValue)
                }
            }

            activity.supportFragmentManager.popBackStackImmediate()

            // Navigate to menu after scan
            activity.supportFragmentManager.beginTransaction()
                .replace(R.id.frameLayout, fragment)
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        barcodeScanner.close()
        // hasta la vista baby
    }

    companion object {
        private const val TAG = "ScanQRCodeFragment"
    }
}