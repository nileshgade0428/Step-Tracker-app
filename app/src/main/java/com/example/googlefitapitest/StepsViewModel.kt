package com.example.googlefitapitest

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class StepsViewModel : ViewModel() {

    private val _isStarted = MutableLiveData<Boolean>()
    val isStarted: LiveData<Boolean> = _isStarted

    fun changeButtonState(){
        _isStarted.postValue(_isStarted.value == false)
    }
}