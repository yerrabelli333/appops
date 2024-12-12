package com.example.myapplication


import android.Manifest
import android.Manifest.permission.RECORD_AUDIO
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.TextureView
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.JobIntentService
import androidx.core.app.JobIntentService.enqueueWork
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel



class MainActivity : AppCompatActivity() {
    private val TAG = "AppOpsCallbackAkshay"
    private val THRESHOLD = 30f

    private lateinit var accesscamera: Button

    private lateinit var etCameraCount: EditText
    private lateinit var etMicrophoneCount: EditText
    private lateinit var etLocationCount: EditText

    private lateinit var fusedLocationClient:FusedLocationProviderClient

    private lateinit var locationManager: LocationManager

    private fun loadModelFile(): ByteBuffer {
        val assetManager = assets
        val fileDescriptor = assetManager.openFd("vae_model_new.tflite")
        val inputStream = fileDescriptor.createInputStream()
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val length = fileDescriptor.length
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, length).order(ByteOrder.nativeOrder())
    }
    companion object {

        const val REQUEST_MICROPHONE_PERMISSION = 101
        const val REQUEST_LOCATION_PERMISSION = 102



    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        accesscamera = findViewById(R.id.accesscamera)

        etCameraCount = findViewById(R.id.cameracount)
        etMicrophoneCount = findViewById(R.id.microphonecount)
        etLocationCount = findViewById(R.id.locationcount)
        val access = findViewById<EditText>(R.id.accessTextView)
        val resultTextView = findViewById<EditText>(R.id.resultTextView)
        fusedLocationClient=LocationServices.getFusedLocationProviderClient(this)
        val forecheckbox: CheckBox = findViewById(R.id.fore)
        val backcheckbox: CheckBox = findViewById(R.id.back)



        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager


        val predictbutton = findViewById<Button>(R.id.predictButton)
        val serviceIntent = Intent(this, Monitor::class.java)
        startService(serviceIntent)
        predictbutton.setOnClickListener {
            access.setText("")
            resultTextView.setText("")
            getPermissionDataAndPredict()

        }
        val stopservice=findViewById<Button>(R.id.stopservice)
        stopservice.setOnClickListener {
            Log.i(TAG, "service is stopping")
            val serviceIntent = Intent(this, Monitor::class.java)
            stopService(serviceIntent)
        }
        accesscamera.setOnClickListener {
            Log.i(TAG, "service is starting")
            val cameracount = etCameraCount.text.toString().toIntOrNull() ?: 0
            val forechecked = forecheckbox.isChecked
            val loccount = etLocationCount.text.toString().toIntOrNull() ?: 0
            val miccount = etMicrophoneCount.text.toString().toIntOrNull() ?: 0




            val backchecked = backcheckbox.isChecked

            for (i in 1..cameracount) {
                val intent = Intent(this, CaptureActivity::class.java)
                if(forechecked){
                    startActivity(intent)
                }



            }
            if(forechecked){
                accessMicrophone()
                accessLocation()
            }
            if(backchecked){

                val serviceintent=Intent(this,MyBackgroundService::class.java).apply{
                        putExtra("cameracount",cameracount)
                        putExtra("microphonecount",miccount)
                        putExtra("locationcount",loccount)
                }
                ContextCompat.startForegroundService(this,serviceintent)








            }






        }

    }




    private fun accessMicrophone() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO), REQUEST_MICROPHONE_PERMISSION)
        } else {
            val miccount = etMicrophoneCount.text.toString().toIntOrNull() ?: 0

            startRecording()
        }
    }
    private fun accessLocation() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||ContextCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED  ) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),REQUEST_LOCATION_PERMISSION )
        } else {
            val loccount = etLocationCount.text.toString().toIntOrNull() ?: 0
            repeat(loccount){
                startLocationUpdates()
            }
        }
    }
    private fun startRecording() {
        var microphonecount = etMicrophoneCount.text.toString().toIntOrNull() ?: 0
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
            Thread.sleep(500)
            audioRecord.stop()
            audioRecord.release()
        }
    }
    private fun startLocationUpdates() {
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
            Toast.makeText(this@MainActivity, "location accessed: Lat=${location.latitude}, Long=${location.longitude}", Toast.LENGTH_SHORT).show()
        }
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
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {

            REQUEST_MICROPHONE_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    val miccount = etMicrophoneCount.text.toString().toIntOrNull() ?: 0
                    startRecording()
                } else {
                    Toast.makeText(this, "Microphone permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_LOCATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED ) {
                    val loccount = etLocationCount.text.toString().toIntOrNull() ?: 0
                    repeat(loccount){
                        startLocationUpdates()
                    }
                } else {
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun getPermissionDataAndPredict() {
        val serviceIntent = Intent(this, Monitor::class.java)
        val componentName = ComponentName(this, Monitor::class.java)
        val accessBuilder = StringBuilder()
        val resultBuilder=StringBuilder()
        val access = findViewById<EditText>(R.id.accessTextView)
        val resultTextView = findViewById<EditText>(R.id.resultTextView)
        bindService(serviceIntent, object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                val monitorService = (service as Monitor.LocalBinder).getService()
                val appPermissionData = monitorService.getAppPermissionData()
                appPermissionData.forEach { (packageName, permissions) ->
                    var cameracount=0
                    var microphonecount=0
                    var locationcount=0
                    var totalcount=0
                    val batteryconsumption=0.11863744664568243
                    var cpuusage=0.06232530033560306
                    var fore=0
                    var play=0
                    var duration=0
                    var isForeground=false

                    permissions.forEach { (op, info) ->
                        if(op=="android:camera"){
                            cameracount=info.count
                        }
                        if(op=="android:record_audio"){
                            microphonecount=info.count
                        }
                        if(op=="android:monitor_location"  ){
                            locationcount += info.count
                        }
                        if(op=="android:monitor_location_high_power"){
                            locationcount+=info.count
                        }
                        totalcount=cameracount+locationcount+microphonecount
                        fore = if (info.isForeground) 0 else 20
                         duration= (info.duration.toInt().toDouble()/463218).toInt()
                        isForeground=info.isForeground
                    }


                    Log.i(TAG,"packagename:$packageName,microphonecount:$microphonecount,cameracount:$cameracount,locationcount:$locationcount,forg:$isForeground")
                    accessBuilder.append("$packageName,Mic:$microphonecount,cam:$cameracount,loc:$locationcount,forg:$isForeground\n")

                    val inputValues = listOf(
                        microphonecount.toString(),
                        cameracount.toString(),
                        locationcount.toString(),
                        totalcount.toString(),
                        batteryconsumption.toString(),
                        cpuusage.toString(),
                        fore.toString(),
                        play.toString(),
                        duration.toString()
                    ).map { it.trim().toFloatOrNull() }
                    if (inputValues.all { it != null }) {
                        val inputFeature = ByteBuffer.allocateDirect(4 * 16 * 9).order(ByteOrder.nativeOrder())
                        inputValues.forEach { inputFeature.putFloat(it!!) }
                        val outputFeature = ByteBuffer.allocateDirect(1 * 4).order(ByteOrder.nativeOrder())
                        val inputs = mapOf("x" to inputFeature)
                        val outputs = mapOf("loss" to outputFeature)
                        val interpreter: Interpreter=Interpreter(loadModelFile())
                        interpreter.runSignature(inputs, outputs,"train_vae")
                        val x=outputs["loss"]
                        x?.rewind()
                        val floatArray=FloatArray(1)
                        x?.asFloatBuffer()?.get(floatArray)
                        if(floatArray[0]>THRESHOLD){
                            Log.i(TAG,"Anamoly detected for app:$packageName\n")
                            resultBuilder.append("Anamoly detected for app:$packageName\n")
                        }
                        if(floatArray[0]<=THRESHOLD){
                            Log.i(TAG,"$packageName is Normal\n")
                            resultBuilder.append("$packageName is Normal\n")
                        }
                    } else {
                        Log.i(TAG,"error in model")
                    }

                }
                access.setText(accessBuilder.toString())
                resultTextView.setText(resultBuilder.toString())



                unbindService(this)
            }
            override fun onServiceDisconnected(name: ComponentName) {
                Log.e(TAG, "Service disconnected")
            }
        }, Context.BIND_AUTO_CREATE)
    }









}
