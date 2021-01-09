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


data class MetaInfo(val id: String, val title: String, val media: String, val img: String, val isAudio: Boolean)
data class FileInfo(val title:String, val path:String, val isAudio: Boolean);

object Manager {
    private var list: ArrayList<Long> = ArrayList()
    private val onComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctxt: Context, intent: Intent) {
            val referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            list.remove(referenceId)
            if (list.isEmpty()) {
                Toast.makeText(ctxt, "All Download complete", Toast.LENGTH_SHORT);
            }
        }
    }

    fun onCreate(activity: Activity) {
        activity.registerReceiver(onComplete,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    fun onDestroy(activity: Activity) {
        activity.unregisterReceiver(onComplete);
    }

    fun getDownloadUrl(activity: Activity, url: String, cb: (str: MetaInfo) -> Unit, err:(str:String)->Unit, audioOnly: Boolean = false) {
        val client: OkHttpClient = OkHttpClient();
        try {
            val url1 = "https://simplestore.dipankar.co.in/api/utils/youtubedl/${if (audioOnly) "audio" else "video"}?id=${url}"
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
                                title = title.replace("[^a-zA-Z0-9 ]".toRegex(), "");
                                title = title.replace(" ","_")
                                cb(MetaInfo(out.getString("id"), title, out.getString("media"), out.getString("img"), audioOnly))
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

    fun downlaodFile(activity: Activity, metaInfo: MetaInfo, cb: (str: MetaInfo) -> Unit) {
        val downloadManager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri: Uri = Uri.parse(metaInfo.media)
        val request = DownloadManager.Request(uri)
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
        request.setTitle("Downloadig file...")
        request.setDescription(metaInfo.title)

        request.allowScanningByMediaScanner()
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        //set the local destination for download file to a path within the application's external files directory
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "/tube/${metaInfo.id}-${metaInfo.title}${if (metaInfo.isAudio) ".mp3" else ".mp4"}")
        request.setVisibleInDownloadsUi(true);
        request.setMimeType("*/*")
        val referenceId = downloadManager!!.enqueue(request)
        list.add(referenceId);
    }

    fun loadFileList(activity: Activity):List<FileInfo>{
        val result = ArrayList<FileInfo>()
        try {
            val sdCardRoot: File = Environment.getExternalStorageDirectory()
            val yourDir = File(sdCardRoot, "${Environment.DIRECTORY_DOWNLOADS}/tube/")
            for (f in yourDir.listFiles()) {
                if (f.isFile())
                    result.add(FileInfo(f.name, f.absolutePath, f.extension == "mp3"))
            }
        } catch (e:java.lang.Exception){
            e.printStackTrace()
        }

        return result;
    }
}
