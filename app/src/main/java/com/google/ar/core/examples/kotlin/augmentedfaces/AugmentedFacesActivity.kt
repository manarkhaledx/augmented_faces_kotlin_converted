package com.google.ar.core.examples.kotlin.augmentedfaces

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.ArCoreApk
import com.google.ar.core.ArCoreApk.InstallStatus
import com.google.ar.core.AugmentedFace
import com.google.ar.core.CameraConfig
import com.google.ar.core.CameraConfigFilter
import com.google.ar.core.Config
import com.google.ar.core.Config.AugmentedFaceMode
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.examples.java.augmentedfaces.R
import com.google.ar.core.examples.kotlin.common.helpers.CameraPermissionHelper.hasCameraPermission
import com.google.ar.core.examples.kotlin.common.helpers.CameraPermissionHelper.launchPermissionSettings
import com.google.ar.core.examples.kotlin.common.helpers.CameraPermissionHelper.requestCameraPermission
import com.google.ar.core.examples.kotlin.common.helpers.CameraPermissionHelper.shouldShowRequestPermissionRationale
import com.google.ar.core.examples.kotlin.common.helpers.DisplayRotationHelper
import com.google.ar.core.examples.kotlin.common.helpers.FullScreenHelper.setFullScreenOnWindowFocusChanged
import com.google.ar.core.examples.kotlin.common.helpers.SnackbarHelper
import com.google.ar.core.examples.kotlin.common.helpers.TrackingStateHelper
import com.google.ar.core.examples.kotlin.common.rendering.BackgroundRenderer
import com.google.ar.core.examples.kotlin.common.rendering.ObjectRenderer
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException
import java.io.IOException
import java.util.EnumSet
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


class AugmentedFacesActivity : AppCompatActivity(), GLSurfaceView.Renderer { //implements GLSurfaceView.Renderer for rendering OpenGL graphics.

    private var surfaceView: GLSurfaceView? = null //Displays AR graphics.

    private var installRequested = false //Tracks if ARCore installation was requested.

    private var session: Session? = null //Manages ARCore session.
    private val messageSnackbarHelper = SnackbarHelper() //Shows a snackbar message.
    private var displayRotationHelper: DisplayRotationHelper? = null //Handles display rotations.
    private val trackingStateHelper = TrackingStateHelper(this) //Manages the display of the AR tracking state.

    private val backgroundRenderer = BackgroundRenderer() //Renders the camera feed.
    private val augmentedFaceRenderer = AugmentedFaceRenderer() //Renders augmented faces.
    private val noseObject = ObjectRenderer() //Renders the nose.
    private val rightEarObject = ObjectRenderer() //Renders the right ear.
    private val leftEarObject = ObjectRenderer() //Renders the left ear.

    // Temporary matrix allocated here to reduce number of allocations for each frame.
    private val noseMatrix = FloatArray(16) //Matrix for the nose.
    private val rightEarMatrix = FloatArray(16) //Matrix for the right ear.
    private val leftEarMatrix = FloatArray(16)//Matrix for the left ear.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) //Sets the content view to the layout.
        surfaceView = findViewById(R.id.surfaceview) //Retrieves the GLSurfaceView from the layout.
        displayRotationHelper = DisplayRotationHelper(this) //Handles display rotations.

        // Set up renderer.
        surfaceView?.let {
            it.preserveEGLContextOnPause = true //Preserves the EGL context when the activity is paused.
            it.setEGLContextClientVersion(2) //Sets the OpenGL ES version to 2.
            it.setEGLConfigChooser(8, 8, 8, 8, 16, 0) // Alpha used for plane blending.
            it.setRenderer(this) //Sets the renderer.
            it.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY //Renders continuously.
            it.setWillNotDraw(false) //Allows the GLSurfaceView to call onDrawFrame().
        }




        installRequested = false //Sets installRequested to false.
    }

    override fun onDestroy() {
        if (session != null) {
            // Explicitly close ARCore Session to release native resources.
            session!!.close()
            session = null
        }

        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()

        if (session == null) {
            var exception: Exception? = null
            var message: String? = null
            try {
                when (ArCoreApk.getInstance().requestInstall(this, !installRequested)) { //Requests ARCore installation.
                    InstallStatus.INSTALL_REQUESTED -> { //If installation is requested.
                        installRequested = true //Sets installRequested to true.
                        return
                    }

                    InstallStatus.INSTALLED -> {} //If ARCore is installed, do nothing.

                    else -> {}
                }

                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!hasCameraPermission(this)) {
                    requestCameraPermission(this)
                    return
                }

                // Create the session and configure it to use a front-facing (selfie) camera.
                session = Session(this, EnumSet.noneOf( //Creates a new ARCore session.
                    Session.Feature::class.java //Specifies the features to enable.
                )
                )
                val cameraConfigFilter = CameraConfigFilter(session) //Creates a new camera config filter.
                cameraConfigFilter.setFacingDirection(CameraConfig.FacingDirection.FRONT) //Sets the camera facing direction to front.
                val cameraConfigs = session!!.getSupportedCameraConfigs(cameraConfigFilter) //Retrieves the supported camera configs.
                if (cameraConfigs.isNotEmpty()) { //If there are supported camera configs.
                    // Element 0 contains the camera config that best matches the session feature
                    // and filter settings.
                    session!!.cameraConfig = cameraConfigs[0] //Sets the camera config to the first supported camera config.
                } else {
                    message = "This device does not have a front-facing (selfie) camera"
                    exception = UnavailableDeviceNotCompatibleException(message)
                }
                configureSession()
            } catch (e: UnavailableArcoreNotInstalledException) {
                message = "Please install ARCore"
                exception = e
            } catch (e: UnavailableUserDeclinedInstallationException) {
                message = "Please install ARCore"
                exception = e
            } catch (e: UnavailableApkTooOldException) {
                message = "Please update ARCore"
                exception = e
            } catch (e: UnavailableSdkTooOldException) {
                message = "Please update this app"
                exception = e
            } catch (e: UnavailableDeviceNotCompatibleException) {
                message = "This device does not support AR"
                exception = e
            } catch (e: Exception) {
                message = "Failed to create AR session"
                exception = e
            }

            if (message != null) {
                messageSnackbarHelper.showError(this, message)
                Log.e(TAG, "Exception creating session", exception)
                return
            }
        }

        // Note that order matters - see the note in onPause(), the reverse applies here.
        try {
            session!!.resume()
        } catch (e: CameraNotAvailableException) {
            messageSnackbarHelper.showError(this, "Camera not available. Try restarting the app.")
            session = null
            return
        }

        surfaceView!!.onResume()
        displayRotationHelper!!.onResume()
    }

    public override fun onPause() {
        super.onPause()
        if (session != null) {
            // Note that the order matters - GLSurfaceView is paused first so that it does not try
            // to query the session. If Session is paused before GLSurfaceView, GLSurfaceView may
            // still call session.update() and get a SessionPausedException.
            displayRotationHelper!!.onPause()
            surfaceView!!.onPause()
            session!!.pause()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        results: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (!hasCameraPermission(this)) {
            Toast.makeText(
                this,
                "Camera permission is needed to run this application",
                Toast.LENGTH_LONG
            )
                .show()
            if (!shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                launchPermissionSettings(this)
            }
            finish()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        setFullScreenOnWindowFocusChanged(this, hasFocus)
    }

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)

        // Prepare the rendering objects. This involves reading shaders, so may throw an IOException.
        try {
            // Create the texture and pass it to ARCore session to be filled during update().

            backgroundRenderer.createOnGlThread( /*context=*/this)
            augmentedFaceRenderer.createOnGlThread(this, "models/freckles.png")
            augmentedFaceRenderer.setMaterialProperties(0.0f, 1.0f, 0.1f, 6.0f)
            noseObject.createOnGlThread( /*context=*/this, "models/nose.obj", "models/nose_fur.png")
            noseObject.setMaterialProperties(0.0f, 1.0f, 0.1f, 6.0f)
            noseObject.setBlendMode(ObjectRenderer.BlendMode.AlphaBlending)
            rightEarObject.createOnGlThread(this, "models/forehead_right.obj", "models/ear_fur.png")
            rightEarObject.setMaterialProperties(0.0f, 1.0f, 0.1f, 6.0f)
            rightEarObject.setBlendMode(ObjectRenderer.BlendMode.AlphaBlending)
            leftEarObject.createOnGlThread(this, "models/forehead_left.obj", "models/ear_fur.png")
            leftEarObject.setMaterialProperties(0.0f, 1.0f, 0.1f, 6.0f)
            leftEarObject.setBlendMode(ObjectRenderer.BlendMode.AlphaBlending)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to read an asset file", e)
        }
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        displayRotationHelper!!.onSurfaceChanged(width, height)
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10) {
        // Clear screen to notify driver it should not load any pixels from previous frame.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        if (session == null) {
            return
        }
        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        displayRotationHelper!!.updateSessionIfNeeded(session!!)

        try {
            session!!.setCameraTextureName(backgroundRenderer.textureId)

            // Obtain the current frame from ARSession. When the configuration is set to
            // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
            // camera framerate.
            val frame = session!!.update()
            val camera = frame.camera

            // Get projection matrix.
            val projectionMatrix = FloatArray(16)
            camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100.0f)

            // Get camera matrix and draw.
            val viewMatrix = FloatArray(16)
            camera.getViewMatrix(viewMatrix, 0)

            // Compute lighting from average intensity of the image.
            // The first three components are color scaling factors.
            // The last one is the average pixel intensity in gamma space.
            val colorCorrectionRgba = FloatArray(4)
            frame.lightEstimate.getColorCorrection(colorCorrectionRgba, 0)

            // If frame is ready, render camera preview image to the GL surface.
            backgroundRenderer.draw(frame)

            // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
            trackingStateHelper.updateKeepScreenOnFlag(camera.trackingState)

            // ARCore's face detection works best on upright faces, relative to gravity.
            // If the device cannot determine a screen side aligned with gravity, face
            // detection may not work optimally.
            val faces = session!!.getAllTrackables(
                AugmentedFace::class.java
            )
            for (face in faces) {
                if (face.trackingState != TrackingState.TRACKING) {
                    break
                }

                val scaleFactor = 1.0f

                // Face objects use transparency so they must be rendered back to front without depth write.
                GLES20.glDepthMask(false)

                // Each face's region poses, mesh vertices, and mesh normals are updated every frame.

                // 1. Render the face mesh first, behind any 3D objects attached to the face regions.
                val modelMatrix = FloatArray(16)
                face.centerPose.toMatrix(modelMatrix, 0)
                augmentedFaceRenderer.draw(
                    projectionMatrix, viewMatrix, modelMatrix, colorCorrectionRgba, face
                )

                // 2. Next, render the 3D objects attached to the forehead.
                face.getRegionPose(AugmentedFace.RegionType.FOREHEAD_RIGHT)
                    .toMatrix(rightEarMatrix, 0)
                rightEarObject.updateModelMatrix(rightEarMatrix, scaleFactor)
                rightEarObject.draw(
                    viewMatrix,
                    projectionMatrix,
                    colorCorrectionRgba,
                    DEFAULT_COLOR
                )

                face.getRegionPose(AugmentedFace.RegionType.FOREHEAD_LEFT)
                    .toMatrix(leftEarMatrix, 0)
                leftEarObject.updateModelMatrix(leftEarMatrix, scaleFactor)
                leftEarObject.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, DEFAULT_COLOR)

                // 3. Render the nose last so that it is not occluded by face mesh or by 3D objects attached
                // to the forehead regions.
                face.getRegionPose(AugmentedFace.RegionType.NOSE_TIP).toMatrix(noseMatrix, 0)
                noseObject.updateModelMatrix(noseMatrix, scaleFactor)
                noseObject.draw(viewMatrix, projectionMatrix, colorCorrectionRgba, DEFAULT_COLOR)
            }
        } catch (t: Throwable) {
            // Avoid crashing the application due to unhandled exceptions.
            Log.e(TAG, "Exception on the OpenGL thread", t)
        } finally {
            GLES20.glDepthMask(true)
        }
    }

    private fun configureSession() {
        val config = Config(session)
        config.setAugmentedFaceMode(AugmentedFaceMode.MESH3D)
        session!!.configure(config)
    }

    companion object {
        private val TAG: String = AugmentedFacesActivity::class.java.simpleName

        private val DEFAULT_COLOR = floatArrayOf(0f, 0f, 0f, 0f)
    }
}