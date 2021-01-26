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
    var mMetaInfo:MetaInfo? = null;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fetch_meta.setOnClickListener {
            fetch(it)
        }
        download_audio.setOnClickListener {
            status("downloading audio....")
            download(true, it)
        }

        download_video.setOnClickListener {
            status("downloading video....")
            download(false, it)
        }
        reload_list.setOnClickListener {
            status("Reloading list...")
            reloadList();
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
        Manager.onCreate(this, object :Manager.ManagerCallback{
            override fun onDownloadComplete() {
                reloadList()
            }
        });
        reloadList()
    }

    private fun reloadList() {
        status("loading list....")
        mArrayAdapter.clear()
        mFileList = Manager.loadFileList(this)
        mArrayAdapter.addAll(mFileList.map { x -> "[${x.lastModified}]${x.title}" })
        mArrayAdapter.notifyDataSetChanged()
        status("list loaded.")
    }

    fun fetch(view: View){
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
                mMetaInfo = it;
                status("Info: ${mMetaInfo?.title}")
            }
            , {
                status(it)
            }
        )
    }

    fun download(audio: Boolean, view: View) {
        status("Downloading file ....")
        link.setText("")
        if(mMetaInfo == null){
            status("Please first fetch the info..")
        }
        mMetaInfo?.let {
            Manager.downlaodFile(this,it , {
                status("Download complete..")
                Manager.loadFileList(this)
            }, audio)
        }
    }

    override fun onResume() {
        super.onResume()
        reloadList();
    }

    override fun onDestroy() {
        Manager.onDestroy(this)
        super.onDestroy()
    }

    fun status(msg: String) {
        status.text = msg
    }
}
