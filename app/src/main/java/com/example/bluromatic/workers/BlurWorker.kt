package com.example.bluromatic.workers

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.bluromatic.DELAY_TIME_MILLIS
import com.example.bluromatic.KEY_BLUR_LEVEL
import com.example.bluromatic.KEY_IMAGE_URI
import com.example.bluromatic.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val TAG = "BlurWorker"

//doWork()로 해야할 것을 정의
class BlurWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    //리소스 주소+블러레벨
    val resourceUri = inputData.getString(KEY_IMAGE_URI)
    val blurLevel = inputData.getInt(KEY_BLUR_LEVEL, 1)


    override suspend fun doWork(): Result {
        //상태알림 -> 사용자에게 블러처리한다고 알림.
        makeStatusNotification(
            applicationContext.resources.getString(R.string.blurring_image),
            applicationContext
        )
        //CoroutineWorker는 기본적으로 Dispatchers.Default로 실행되지만 withContext()를 호출하고 원하는 디스패처를 전달하여 변경할 수 있다.
        return withContext(Dispatchers.IO) {
            return@withContext try {
                //IllegalArgumentException
                require(!resourceUri.isNullOrBlank()) {
                    val errorMessage =
                        applicationContext.resources.getString(R.string.invalid_input_uri)
                    Log.e(TAG, errorMessage)
                    errorMessage
                }
                //이미지 소스가 URI로 전달되므로 URI가 가리키는 콘텐츠를 읽으려면 contentResolver
                val resolver = applicationContext.contentResolver

                delay(DELAY_TIME_MILLIS)
                /*
                val picture = BitmapFactory.decodeResource(
                    applicationContext.resources,
                    R.drawable.android_cupcake
                )*/
                val picture = BitmapFactory.decodeStream(
                    resolver.openInputStream(Uri.parse(resourceUri))
                )
                //bitmap blur 처리
                //val output = blurBitmap(picture, 1)
                val output = blurBitmap(picture, blurLevel)
                // Write bitmap to a temp file
                val outputUri = writeBitmapToFile(applicationContext, output)
                //출력 데이터 객체가 이제 URI를 사용하므로 알림을 표시하는 코드가 더 이상 필요하지 않아 이 코드를 삭제한다.
                /*
                makeStatusNotification(
                    "Output is $outputUri",
                    applicationContext
                )*/
                val outputData = workDataOf(KEY_IMAGE_URI to outputUri.toString())
                //Result.success()
                Result.success(outputData)
            } catch (throwable: Throwable) {
                Log.e(
                    TAG,
                    applicationContext.resources.getString(R.string.error_applying_blur),
                    throwable
                )
                Result.failure()
            }
        }
    }
}