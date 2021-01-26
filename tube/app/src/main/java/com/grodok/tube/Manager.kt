package com.grodok.tube

import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import okhttp3.*
import org.json.JSONObject
import java.io.File
import java.io.IOException


data class MetaInfo(val id: String, val title: String, val audio: String,  val video: String, val img: String)
data class FileInfo(val title:String, val path:String, val isAudio: Boolean, val lastModified:Long);

object Manager {
    interface ManagerCallback{
        fun onDownloadComplete();
    }
    private var mCallback:ManagerCallback? = null;
    private var list: ArrayList<Long> = ArrayList()
    private val onComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctxt: Context, intent: Intent) {
            val referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            list.remove(referenceId)
            if (list.isEmpty()) {
                Toast.makeText(ctxt, "All Download complete", Toast.LENGTH_SHORT);
            }
            mCallback?.onDownloadComplete()
        }
    }

    fun onCreate(activity: Activity, callback: ManagerCallback) {
        activity.registerReceiver(onComplete,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        mCallback = callback;
        mCallback?.onDownloadComplete()
    }

    fun onDestroy(activity: Activity) {
        activity.unregisterReceiver(onComplete);
        mCallback = null;
    }

    fun getDownloadUrl(activity: Activity, url: String, cb: (str: MetaInfo) -> Unit, err:(str:String)->Unit) {
        val client: OkHttpClient = OkHttpClient();
        try {
            val url1 = "https://simplestore.dipankar.co.in/api/utils/youtubedl/info?id=${url}"
            Log.d("DIPANKAR", url1)
            val request: Request = Request.Builder().url(url1).build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    e.printStackTrace()
                    activity.runOnUiThread {
                        err("Network request failed.")
                        Toast.makeText(activity, "Not able to fetch audio URL", Toast.LENGTH_SHORT);
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        val json = JSONObject(response.body?.string());
                        if (json.getString("status") == "success") {
                            val out = json.getJSONObject("out")
                            activity.runOnUiThread {
                                var title = out.getString("title")
                                //title = title.replace("[^a-zA-Z0-9 ]".toRegex(), "");
                                title = title.replace(" ","_")
                                title = title.replace("?","");
                                cb(MetaInfo(out.getString("id"), title, out.getString("audio_url"), out.getString("video_url"), out.getString("img")))
                            }
                        } else {
                            throw Exception(json.getString("msg"));
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        err("SimpleStore Server Error ${e.message}")
                        activity.runOnUiThread {
                            Toast.makeText(activity, "Not able to fetch audio URL", Toast.LENGTH_SHORT);
                        }
                    }
                }
            });

        } catch (e: Exception) {
            err("Download meta fails: ${e.message}")
            e.printStackTrace()
            Toast.makeText(activity, "Not able to fetch audio URL", Toast.LENGTH_SHORT);
        }
    }

    fun downlaodFile(activity: Activity, metaInfo: MetaInfo, cb: (str: MetaInfo) -> Unit, isAudio: Boolean) {
        val downloadManager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri: Uri = Uri.parse(if(isAudio) metaInfo.audio else metaInfo.video)
        val request = DownloadManager.Request(uri)
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
        request.setTitle("Downloadig file...")
        request.setDescription(metaInfo.title)

        request.allowScanningByMediaScanner()
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        //set the local destination for download file to a path within the application's external files directory
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "/tube/${metaInfo.id}-${metaInfo.title}${if (isAudio) ".mp3" else ".mp4"}")
        request.setVisibleInDownloadsUi(true);
        request.setMimeType("*/*")
        val referenceId = downloadManager.enqueue(request)
        list.add(referenceId);
    }

    fun loadFileList(activity: Activity):List<FileInfo>{
        val result = ArrayList<FileInfo>()
        try {
            val sdCardRoot: File = Environment.getExternalStorageDirectory()
            val yourDir = File(sdCardRoot, "${Environment.DIRECTORY_DOWNLOADS}/tube/")
            if(yourDir.listFiles() != null) {
                for (f in yourDir.listFiles()) {
                    if (f.isFile())
                        result.add(
                            FileInfo(
                                f.name,
                                f.absolutePath,
                                f.extension == "mp3",
                                f.lastModified()
                            )
                        )
                }
            }
        } catch (e:java.lang.Exception){
            e.printStackTrace()
        }
        result.sortedByDescending {it.lastModified};
        return result;
    }
}
