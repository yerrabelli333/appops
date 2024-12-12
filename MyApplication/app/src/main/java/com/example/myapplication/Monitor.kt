package com.example.myapplication


import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors


class Monitor : Service() {
    private val TAG = "AppOpsCallbackAkshay";
    private val activePermissions= mutableMapOf<String,Long>()
    data class PermissionInfo(val count:Int=0,val duration: Long=0,val isForeground: Boolean = false)
    private val appPermissionData=ConcurrentHashMap<String,MutableMap<String, PermissionInfo>>()




    private val opActiveChangedListener =
        AppOpsManager.OnOpActiveChangedListener { op, uid, packageName, active ->


            if (packageName == "android") return@OnOpActiveChangedListener

            packageName?.let {
                var isForeground = false

                val activityManager = this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val runningAppProcesses = activityManager.runningAppProcesses

                for (processInfo in runningAppProcesses) {
                    Log.d(TAG ,"Package Name: $packageName")
                    Log.d(TAG, "Importance: ${processInfo.importance}")
                    Log.d(TAG, "Process Name: ${processInfo.processName}")
                    Log.d(TAG, "Process Name: ${ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND }")

                    if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND ) {
                        isForeground = true
                        break
                    }
                }
                if (active) {
                    Log.i(TAG, isForeground.toString())


                    val permissionMap = appPermissionData.getOrPut(it) { ConcurrentHashMap() }
                    val permissionInfo = permissionMap.getOrDefault(op, PermissionInfo())
                    activePermissions[op] = System.currentTimeMillis()
                    permissionMap[op] = PermissionInfo(permissionInfo.count + 1, permissionInfo.duration, isForeground)
                } else {
                    Log.i(TAG, isForeground.toString())

                    val startTime = activePermissions.remove(op)
                    if (startTime != null) {
                        val duration = System.currentTimeMillis() - startTime
                        val permissionMap = appPermissionData[it]
                        val permissionInfo = permissionMap?.get(op)
                        if (permissionInfo != null) {
                            permissionMap[op] = PermissionInfo(permissionInfo.count, permissionInfo.duration + duration, isForeground)
                        }
                        if (permissionInfo != null) {
                            Log.i(TAG, "permission $op is used by $packageName, duration: $duration, Count : ${permissionInfo.count}, Foreground: ${if (isForeground) "Yes" else "No"}")
                        }
                    }
                }
            }
        }



    fun getAppPermissionData():Map<String,Map<String, PermissionInfo>> {
        return appPermissionData.toMap()
    }
    inner class LocalBinder: Binder(){
        fun getService(): Monitor {
            return this@Monitor
        }

    }



    override fun onCreate() {
        val mContext=applicationContext
        val mAppOpsManager: AppOpsManager = (mContext.getSystemService(APP_OPS_SERVICE) as AppOpsManager?)!!
        val mExecutorServiceForAppOps= Executors.newSingleThreadExecutor();
        val opString = arrayOf(
            AppOpsManager.OPSTR_CAMERA,
            AppOpsManager.OPSTR_RECORD_AUDIO,
            AppOpsManager.OPSTR_COARSE_LOCATION,
            AppOpsManager.OPSTR_FINE_LOCATION, AppOpsManager.OPSTR_MONITOR_HIGH_POWER_LOCATION,
            AppOpsManager.OPSTR_MONITOR_LOCATION, AppOpsManager.OPSTR_MOCK_LOCATION);
        mAppOpsManager.startWatchingActive( opString,mExecutorServiceForAppOps, opActiveChangedListener);

        super.onCreate()



    }



    override fun onBind(intent: Intent?): IBinder? {
        return LocalBinder()
    }



}

