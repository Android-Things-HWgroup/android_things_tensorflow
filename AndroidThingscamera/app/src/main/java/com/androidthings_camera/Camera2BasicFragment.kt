package com.androidthings_camera

import android.app.Fragment
import android.content.Context
import android.os.Bundle
import android.view.Surface.ROTATION_270
import android.view.Surface.ROTATION_180
import android.view.Surface.ROTATION_90
import android.view.Surface.ROTATION_0
import android.util.SparseIntArray
import android.view.*
import android.widget.Button
import android.graphics.SurfaceTexture
import android.view.TextureView
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.ImageReader
import android.os.Handler
import android.util.Size
import android.os.HandlerThread
import java.io.File
import kotlin.properties.Delegates


/**
 * Created by dudco on 2017. 7. 23..
 */

class Camera2BasicFragment: Fragment() {

    companion object {
        val TAG = "Camera2BasicFragment"
    }

    private var mBackgroundThread: HandlerThread? =  null
    private var mBackgroundHandler: Handler? =  null

    private fun startBackgound(){
        mBackgroundThread = HandlerThread("CameraBacktound")
        mBackgroundThread!!.start()
        mBackgroundHandler = Handler(mBackgroundThread!!.looper)
    }

    private fun stopBackgound(){
        mBackgroundThread!!.quitSafely()
        mBackgroundThread!!.join()
        mBackgroundThread = null
        mBackgroundHandler = null
    }

    var mTextureView by Delegates.notNull<AutoFitTextureView>()

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater!!.inflate(R.layout.fragment_camera2_basic, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
//        view?.findViewById<Button>(R.id.picture)?.setOnClickListener()
        mTextureView = view?.findViewById<AutoFitTextureView>(R.id.texture)!!
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        startBackgound()

        if(mTextureView.isAvailable){
            openCamera(mTextureView.width, mTextureView.height)
        }else{
            mTextureView.surfaceTextureListener = mSurfaceTextureListener;
        }
    }

    private fun openCamera(width: Int, height: Int){
        
    }

    private val mSurfaceTextureListener = object: TextureView.SurfaceTextureListener{
        override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture?, width: Int, height: Int) {
//            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.

        }

        override fun onSurfaceTextureUpdated(p0: SurfaceTexture?) {
//            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onSurfaceTextureDestroyed(p0: SurfaceTexture?): Boolean {
//            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            return true
        }

        override fun onSurfaceTextureAvailable(p0: SurfaceTexture?, width: Int, height: Int) {
//            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            openCamera(width, height)
        }

    }
}