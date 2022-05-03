package com.augustbyrne.mdaspcompanion

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(): ViewModel() {





    private var _connectionStatus: MutableLiveData<Boolean> = MutableLiveData(false)
    val connectionStatus: LiveData<Boolean>
        get() = _connectionStatus
    fun setConnectionStatus(value: Boolean) {
        _connectionStatus.value = value
    }
    fun toggleConnectionStatus() {
        _connectionStatus.value?.let {
            _connectionStatus.value = !it
        }
    }




    private var _myLiveThing: MutableLiveData<MutableList<Int>> = MutableLiveData()
    val myLiveThing: LiveData<MutableList<Int>>
        get() = _myLiveThing

    fun addItem() {
        _myLiveThing.value?.add(1)
    }







}