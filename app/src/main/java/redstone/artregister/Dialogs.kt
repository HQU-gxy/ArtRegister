package redstone.artregister

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.widget.Button
import android.widget.ProgressBar

fun createLoadingDialog(context: Context): AlertDialog =
    AlertDialog.Builder(context).apply {
        setTitle(R.string.loading)
        setView(ProgressBar(context))
        setCancelable(false)
    }.create()

fun createReloadDialog(activity: Activity, title: String): AlertDialog {
    val button = Button(activity).apply {
        setText(R.string.reload)
        setOnClickListener { activity.recreate() }
    }
    return AlertDialog.Builder(activity).apply {
        setTitle(title)
        setCancelable(false)
        setView(button)
    }.show()
}