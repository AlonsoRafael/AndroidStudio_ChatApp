package com.example.chatapp

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import java.util.UUID

data class FileInfo(
    val url: String,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String?
)

class SupabaseStorageUtils(val context: Context) {

    val supabase = createSupabaseClient(
        "YOUR_SUPABASE_URL_HERE", // Substitua pela sua URL do Supabase
        "YOUR_SUPABASE_ANON_KEY_HERE" // Substitua pela sua chave anon do Supabase
    ) {
        install(Storage)
    }

    suspend fun uploadImage(uri: Uri): String? {
        return uploadFile(uri, IMAGES_BUCKET)
    }

    suspend fun uploadVideo(uri: Uri): FileInfo? {
        return uploadFileWithInfo(uri, VIDEOS_BUCKET)
    }

    suspend fun uploadAudio(uri: Uri): FileInfo? {
        Log.d("SupabaseStorageUtils", "uploadAudio chamado com URI: $uri")
        return uploadFileWithInfo(uri, AUDIOS_BUCKET)
    }

    suspend fun uploadFile(uri: Uri): FileInfo? {
        return uploadFileWithInfo(uri, FILES_BUCKET)
    }

    private suspend fun uploadFile(uri: Uri, bucketName: String): String? {
        try {
            val extension = getFileExtension(uri)
            val fileName = "${UUID.randomUUID()}.$extension"
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            supabase.storage.from(bucketName).upload(fileName, inputStream.readBytes())
            val publicUrl = supabase.storage.from(bucketName).publicUrl(fileName)
            return publicUrl
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private suspend fun uploadFileWithInfo(uri: Uri, bucketName: String): FileInfo? {
        try {
            Log.d("SupabaseStorageUtils", "uploadFileWithInfo iniciado para bucket: $bucketName")
            val extension = getFileExtension(uri)
            val originalName = getFileName(uri)
            val fileSize = getFileSize(uri)
            val mimeType = context.contentResolver.getType(uri)
            val fileName = "${UUID.randomUUID()}.$extension"
            
            Log.d("SupabaseStorageUtils", "Detalhes do arquivo - Nome: $originalName, Tamanho: $fileSize, Tipo: $mimeType")
            
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            
            // Tentar primeiro o bucket específico, depois usar fallback
            val finalBucketName = try {
                supabase.storage.from(bucketName).upload(fileName, inputStream.readBytes())
                bucketName
            } catch (e: Exception) {
                Log.w("SupabaseStorageUtils", "Bucket $bucketName não encontrado, tentando bucket padrão")
                try {
                    // Usar bucket de imagens como fallback (assumindo que existe)
                    val fallbackStream = context.contentResolver.openInputStream(uri)
                    supabase.storage.from(IMAGES_BUCKET).upload(fileName, fallbackStream!!.readBytes())
                    IMAGES_BUCKET
                } catch (fallbackException: Exception) {
                    Log.e("SupabaseStorageUtils", "Erro no upload: ${e.message}")
                    throw e
                }
            }
            
            val publicUrl = supabase.storage.from(finalBucketName).publicUrl(fileName)
            
            Log.d("SupabaseStorageUtils", "Upload concluído - URL: $publicUrl")
            
            return FileInfo(
                url = publicUrl,
                fileName = originalName ?: fileName,
                fileSize = fileSize,
                mimeType = mimeType
            )
        } catch (e: Exception) {
            Log.e("SupabaseStorageUtils", "Erro no upload: ${e.message}")
            e.printStackTrace()
            return null
        }
    }

    private fun getFileExtension(uri: Uri): String {
        Log.d("SupabaseStorageUtils", "getFileExtension para URI: $uri")
        
        // Primeiro tentar pelo path do arquivo
        val pathExtension = uri.path?.substringAfterLast(".", "")
        if (!pathExtension.isNullOrEmpty()) {
            Log.d("SupabaseStorageUtils", "Extensão encontrada no path: $pathExtension")
            return pathExtension
        }
        
        // Se não encontrar no path, usar MIME type
        val mimeType = context.contentResolver.getType(uri)
        Log.d("SupabaseStorageUtils", "MIME type: $mimeType")
        
        return when {
            mimeType?.startsWith("image/") == true -> "jpg"
            mimeType?.startsWith("video/") == true -> "mp4"
            mimeType?.startsWith("audio/") == true -> when {
                mimeType.contains("m4a") || mimeType.contains("mp4") -> "m4a"
                mimeType.contains("wav") -> "wav"
                mimeType.contains("ogg") -> "ogg"
                else -> "mp3"
            }
            else -> "bin"
        }.also { extension ->
            Log.d("SupabaseStorageUtils", "Extensão final: $extension")
        }
    }

    private fun getFileName(uri: Uri): String? {
        var fileName: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1) {
                cursor.moveToFirst()
                fileName = cursor.getString(nameIndex)
            }
        }
        return fileName
    }

    private fun getFileSize(uri: Uri): Long {
        var fileSize = 0L
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex != -1) {
                cursor.moveToFirst()
                fileSize = cursor.getLong(sizeIndex)
            }
        }
        return fileSize
    }

    companion object {
        const val IMAGES_BUCKET = "chatapp_images"
        const val VIDEOS_BUCKET = "chatapp_videos"
        const val AUDIOS_BUCKET = "chatapp_audios"
        const val FILES_BUCKET = "chatapp_files"
        
        suspend fun uploadImageToSupabase(context: Context, uri: Uri): String? {
            val utils = SupabaseStorageUtils(context)
            return utils.uploadImage(uri)
        }
        
        suspend fun uploadImageToSupabase(uri: Uri, fileName: String): String? {
            // Para compatibilidade, ignora fileName e usa UUID
            return null // Placeholder - precisa de context
        }
    }
}