package asia.rhinoventures.democamera

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.common.model.LocalModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView

    //    private lateinit var contentView: FrameLayout
    private lateinit var objectDetector: ObjectDetector
    private var graphicOverlay: GraphicOverlay? = null
    private var needUpdateGraphicOverlayImageSourceInfo = false
    val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
            } else {
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        previewView = findViewById(R.id.previewView)
//        contentView = findViewById(R.id.fl_content)
        graphicOverlay = findViewById(R.id.graphic_overlay)


        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // You can use the API that requires the permission.
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {

            }
            else -> {
                requestPermissionLauncher.launch(
                    Manifest.permission.CAMERA
                )
            }
        }


        val localModel = LocalModel.Builder()
            .setAssetFilePath("lite-model_object_detection_mobile_object_labeler_v1_1.tflite")
            .build()

        val option = CustomObjectDetectorOptions.Builder(localModel)
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableClassification()
            .setClassificationConfidenceThreshold(0.5f)
            .setMaxPerObjectLabelCount(1)
            .build()

//        val option = ObjectDetectorOptions.Builder()
//            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
//            .enableClassification()
//            .enableMultipleObjects()
//            .build()


        objectDetector = ObjectDetection.getClient(option)

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            attachPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun attachPreview(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder().build()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        val analytics = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        needUpdateGraphicOverlayImageSourceInfo = true

        analytics.setAnalyzer(ContextCompat.getMainExecutor(this), { imageProxy ->
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val image = imageProxy.image
            if (image != null) {
                if (needUpdateGraphicOverlayImageSourceInfo) {
                    val isImageFlipped = false
                    val rotationDegrees =
                        imageProxy.imageInfo.rotationDegrees
                    if (rotationDegrees == 0 || rotationDegrees == 180) {
                        graphicOverlay?.setImageSourceInfo(
                            imageProxy.width, imageProxy.height, isImageFlipped
                        )
                    } else {
                        graphicOverlay?.setImageSourceInfo(
                            imageProxy.height, imageProxy.width, isImageFlipped
                        )
                    }
                    needUpdateGraphicOverlayImageSourceInfo = false
                }

                val inputImage = InputImage.fromMediaImage(image, rotationDegrees)
                Log.d("XXXX", "Analytics process image")
                objectDetector.process(inputImage).addOnSuccessListener { detectedObjects ->
                    graphicOverlay?.clear()

                    detectedObjects.forEach { objectDetector ->
                        graphicOverlay?.let {
                            it.add(ObjectGraphic(it, objectDetector))
                        }
                    }
                    imageProxy.close()
                }.addOnFailureListener {
                    Log.e("OBJECT_DETECTION", "Cause: ${it.message}")
                    imageProxy.close()
                }
            }
        })


        preview.setSurfaceProvider(
            previewView.surfaceProvider
        )

        val camera = cameraProvider.bindToLifecycle(
            this, cameraSelector, analytics, preview
        )
    }
}