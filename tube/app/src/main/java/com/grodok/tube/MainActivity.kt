package com.grodok.tube

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    var mFileList: List<FileInfo> = ArrayList()
    lateinit var mArrayAdapter: ArrayAdapter<String>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        download_audio.setOnClickListener {
            status("downloading audio....")
            download(true, it)
        }
        download_video.setOnClickListener {
            status("downloading video....")
            download(false, it)
        }
        mArrayAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        list.adapter = mArrayAdapter
        list.setOnItemClickListener { adapterView, view, i, l ->
            val file = mFileList.get(i)
            status("Trying to play video....")
            file.let {
                SimpleExoPlayerActivity.startPlayerActivity(this, file.path)
            }
        }
        reloadList()
    }

    private fun reloadList() {
        status("loading list....")
        mArrayAdapter.clear()
        mFileList = Manager.loadFileList(this)
        mArrayAdapter.addAll(mFileList.map { x -> x.title })
        mArrayAdapter.notifyDataSetChanged()
        status("list loaded.")
    }

    fun download(audio: Boolean, view: View) {
        if (link.text.toString().length < 5) {
            Snackbar.make(view, "Please write some URL", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
            return
        }
        status("Fetching meta info ....")
        Manager.getDownloadUrl(
            this,
            link.text.toString(),
            {
                status("Downloading file ....")
                link.setText("")
                Manager.downlaodFile(this, it, {
                    Manager.loadFileList(this)
                })
            }
            , {
                status(it)
            },
            audio
        )
    }

    override fun onDestroy() {
        Manager.onDestroy(this)
        super.onDestroy()
    }

    fun status(msg: String) {
        status.text = msg
    }
}
