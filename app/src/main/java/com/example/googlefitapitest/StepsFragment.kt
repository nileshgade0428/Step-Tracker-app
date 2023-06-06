package com.example.googlefitapitest

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.googlefitapitest.databinding.FragmentStepsBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.request.DataReadRequest
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

const val TAG = "MySignIn"

const val MY_PERMISSIONS_REQUEST_ACTIVITY_RECOGNITION = 100
const val GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 500

class StepsFragment : Fragment() {

    private lateinit var viewModel: StepsViewModel

    private lateinit var fitnessOptions: FitnessOptions
    private lateinit var account: GoogleSignInAccount

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding: FragmentStepsBinding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_steps,
            container,
            false
        )

        viewModel = ViewModelProvider(this).get(StepsViewModel::class.java)

        fitnessOptions = FitnessOptions.builder()
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .build()

        @RequiresApi(Build.VERSION_CODES.Q)
        if (context?.let {
                ContextCompat.checkSelfPermission(
                    it,
                    Manifest.permission.ACTIVITY_RECOGNITION
                )
            }
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                MY_PERMISSIONS_REQUEST_ACTIVITY_RECOGNITION
            )
        }

        account = GoogleSignIn.getAccountForExtension(context, fitnessOptions)

        viewModel.isStarted.observe(
            viewLifecycleOwner,
            {
                when (it) {
                    true -> {
                        binding.button.text = "Stop"
                    }
                    false -> {
                        binding.button.text = "Start"
                    }
                }
            })

        val fps = resources.getStringArray(R.array.fps_array)
        val adapter = context?.let { ArrayAdapter(it, R.layout.dropdown_item, fps) }

        binding.autoCompleteTextView.setAdapter(adapter)
        binding.autoCompleteTextView.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                val item = parent?.getItemAtPosition(position).toString()
                Toast.makeText(context, item, Toast.LENGTH_SHORT).show()
            }

        binding.button.setOnClickListener {

            if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
                GoogleSignIn.requestPermissions(
                    this, // your activity
                    GOOGLE_FIT_PERMISSIONS_REQUEST_CODE,
                    account,
                    fitnessOptions
                )
            } else {
                accessGoogleFit()
            }

            viewModel.changeButtonState()
        }

        return binding.root
    }

    private fun accessGoogleFit() {
        val end = LocalDateTime.now()
        val start = end.minusYears(1)
        val endSeconds = end.atZone(ZoneId.systemDefault()).toEpochSecond()
        val startSeconds = start.atZone(ZoneId.systemDefault()).toEpochSecond()

        val readRequest = DataReadRequest.Builder()
            .aggregate(DataType.AGGREGATE_STEP_COUNT_DELTA)
            .setTimeRange(startSeconds, endSeconds, TimeUnit.SECONDS)
            .bucketByTime(1, TimeUnit.DAYS)
            .build()
        val account = GoogleSignIn.getAccountForExtension(context, fitnessOptions)
        Fitness.getHistoryClient(context, account)
            .readData(readRequest)
            .addOnSuccessListener {
                // Use response data here
                Toast.makeText(context, " ✅ OnSuccess()", Toast.LENGTH_SHORT).show()
                Log.i(TAG, "OnSuccess()")
            }
            .addOnFailureListener { e -> Log.d(TAG, "OnFailure()", e) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (resultCode) {
            Activity.RESULT_OK -> when (requestCode) {
                GOOGLE_FIT_PERMISSIONS_REQUEST_CODE -> accessGoogleFit()
                else -> {
                    Log.i(TAG, " Result wasn't from Google Fit")
                }
            }
            else -> {
                Log.i(TAG, "Permission not granted")
            }
        }
    }
}