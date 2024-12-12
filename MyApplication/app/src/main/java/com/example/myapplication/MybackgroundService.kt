package com.example.myapplication

import android.Manifest
import android.Manifest.permission.RECORD_AUDIO
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat


class MyBackgroundService : Service() {

    private val notificationId = 1
    private lateinit var locationManager: LocationManager

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(notificationId, createNotification())

        val cameraCount = intent?.getIntExtra("cameraCount", 0) ?: 0
        val microphoneCount = intent?.getIntExtra("microphoneCount", 0) ?: 0
        val locationCount = intent?.getIntExtra("locationCount", 0) ?: 0



        for(i in 1..cameraCount){
            accessCamera()
        }
        accessMicrophone(microphoneCount)
        for(i in 1..locationCount){
            accessLocation()
        }


        return START_NOT_STICKY
    }

    private fun createNotification(): Notification {
        val notificationChannelId = "MY_BACKGROUND_SERVICE_CHANNEL"

        val notificationChannel = NotificationChannel(
            notificationChannelId,
            "Background Service",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(notificationChannel)

        val notificationBuilder = NotificationCompat.Builder(this, notificationChannelId)
        return notificationBuilder
            .setContentTitle("Background Service")
            .setContentText("Running...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    }

    private fun accessCamera() {
        val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager

        try {
            val cameraId = cameraManager.cameraIdList[0]
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    Log.d("MyBackgroundService", "Camera opened")



                    camera.close()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    Log.d("MyBackgroundService", "Camera disconnected")
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e("MyBackgroundService", "Camera error: $error")
                }
            }, null)
        } catch (e: CameraAccessException) {
            Log.e("MyBackgroundService", "Camera access exception: ${e.message}")
        } catch (e: SecurityException) {
            Log.e("MyBackgroundService", "Security exception: ${e.message}")
        }
    }

    private fun accessMicrophone(microphonecount:Int) {
        // Code to access the microphone
        Log.d("MyBackgroundService", "Accessing Microphone")
        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (ActivityCompat.checkSelfPermission(
                this,
                RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        for (i in 1..microphonecount) {
            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
            audioRecord.startRecording()
            Thread.sleep(300)
            audioRecord.stop()
            audioRecord.release()
        }
    }

    private fun accessLocation() {
        Log.i(TAG, "acesing location")
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val criteria = Criteria().apply {
            accuracy = Criteria.ACCURACY_FINE
            powerRequirement = Criteria.POWER_HIGH
        }
        val provider = locationManager.getBestProvider(criteria, true)
        if (provider != null) {
            locationManager.requestLocationUpdates(provider, 1000L, 10f, locationListener)
        } else {
            Toast.makeText(this, "No location provider found", Toast.LENGTH_SHORT).show()
        }
    }

    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            Toast.makeText(this@MyBackgroundService, "location accessed: Lat=${location.latitude}, Long=${location.longitude}", Toast.LENGTH_SHORT).show()
        }
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            // Handle status changes
        }
        override fun onProviderEnabled(provider: String) {
            // Handle provider enabled event
        }
        override fun onProviderDisabled(provider: String) {
            // Handle provider disabled event
        }
    }
}
