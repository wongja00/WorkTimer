package com.example.worktimer.services

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.example.worktimer.data.WorkSession
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.google.api.client.http.ByteArrayContent
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class GoogleSignInService {
    private lateinit var googleSignInClient: GoogleSignInClient
    private var driveService: Drive? = null
    private lateinit var context: Context
    private var signInCallback: ((Boolean) -> Unit)? = null
    private var signInLauncher: ActivityResultLauncher<Intent>? = null

    fun setup(activity: ComponentActivity) {
        this.context = activity

        // Google Sign-In 옵션 설정
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        googleSignInClient = GoogleSignIn.getClient(activity, gso)

        // Activity Result Launcher 등록
        signInLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            handleSignInResult(result.data)
        }

        // 기존 계정이 있다면 드라이브 서비스 설정
        getCurrentAccount()?.let { account ->
            setupDriveService(account)
        }
    }

    fun getCurrentAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    fun signIn(onResult: (Boolean) -> Unit) {
        signInCallback = onResult
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher?.launch(signInIntent) ?: run {
            onResult(false)
        }
    }

    private fun handleSignInResult(data: Intent?) {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(Exception::class.java)
            setupDriveService(account)
            signInCallback?.invoke(true)
        } catch (e: Exception) {
            e.printStackTrace()
            signInCallback?.invoke(false)
        } finally {
            signInCallback = null
        }
    }

    fun signOut(onComplete: () -> Unit) {
        googleSignInClient.signOut().addOnCompleteListener {
            driveService = null
            onComplete()
        }
    }

    private fun setupDriveService(account: GoogleSignInAccount) {
        try {
            val credential = GoogleAccountCredential.usingOAuth2(
                context, listOf(DriveScopes.DRIVE_FILE)
            )
            credential.selectedAccount = account.account

            driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory(),
                credential
            )
                .setApplicationName("WorkTimer")
                .build()
        } catch (e: Exception) {
            e.printStackTrace()
            driveService = null
        }
    }

    suspend fun syncToCloud(workSessions: List<WorkSession>): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val drive = driveService ?: return@withContext false

                if (workSessions.isEmpty()) return@withContext true

                val gson = Gson()
                val jsonData = gson.toJson(workSessions)

                // 기존 파일 찾기
                val query = "name = 'worktimer_data.json' and parents in 'appDataFolder'"
                val fileList = drive.files().list().setQ(query).setSpaces("appDataFolder").execute()

                val fileMetadata = File().apply {
                    name = "worktimer_data.json"
                    parents = listOf("appDataFolder")
                }

                val mediaContent = ByteArrayContent("application/json", jsonData.toByteArray())

                if (fileList.files.isNotEmpty()) {
                    // 기존 파일 업데이트
                    drive.files().update(fileList.files[0].id, fileMetadata, mediaContent).execute()
                } else {
                    // 새 파일 생성
                    drive.files().create(fileMetadata, mediaContent).execute()
                }

                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun syncFromCloud(): List<WorkSession>? {
        return withContext(Dispatchers.IO) {
            try {
                val drive = driveService ?: return@withContext null

                val query = "name = 'worktimer_data.json' and parents in 'appDataFolder'"
                val fileList = drive.files().list().setQ(query).setSpaces("appDataFolder").execute()

                if (fileList.files.isEmpty()) return@withContext listOf()

                val fileId = fileList.files[0].id
                val outputStream = ByteArrayOutputStream()
                drive.files().get(fileId).executeMediaAndDownloadTo(outputStream)

                val jsonData = outputStream.toString("UTF-8")
                val gson = Gson()
                val type = object : TypeToken<List<WorkSession>>() {}.type
                gson.fromJson(jsonData, type)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}