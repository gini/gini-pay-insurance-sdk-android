package net.gini.pay.app

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.gini.pay.app.pager.PagerAdapter

class MainViewModel : ViewModel() {
    private val _pages: MutableStateFlow<List<PagerAdapter.Page>> = MutableStateFlow(emptyList())
    val pages: StateFlow<List<PagerAdapter.Page>> = _pages

    private var currentIndex = 0
    private var currentFileUri: Uri? = null

    fun getNextPageUri(context: Context): Uri {
        val uriForFile = FileProvider.getUriForFile(
            context.applicationContext,
            BuildConfig.APPLICATION_ID + ".fileprovider",
            File(context.filesDir, "page_$currentIndex.jpg")
        )
        currentFileUri = uriForFile
        return uriForFile
    }

    fun onPhotoSaved() {
        currentFileUri?.let { uri ->
            _pages.value = _pages.value.toMutableList().apply { add(PagerAdapter.Page(currentIndex, uri)) }
        }
        ++currentIndex
    }
}