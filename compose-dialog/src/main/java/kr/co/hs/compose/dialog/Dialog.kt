package kr.co.hs.compose.dialog

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.view.SoundEffectConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringDef
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat

//import kr.co.hs.securefile.presentation.compose.Checkbox
//import kr.co.hs.securefile.presentation.compose.RadioGroup
//import kr.co.hs.securefile.presentation.compose.SecFileOutlinedTextField


/**
 * [ShowRequestEnableAccessibilityService] start
 */
@Composable
inline fun <reified T : AccessibilityService> ShowRequestEnableAccessibilityService(
    title: String? = null,
    message: String,
    cancelable: Boolean = true,
    confirm: String = "Apply",
    cancel: String = "Deny",
    crossinline onDismiss: () -> Unit = {},
    crossinline onResult: (Boolean) -> Unit
) {
    val activity = LocalContext.current as Activity
    if (activity.isActiveAccessibilityService<T>()) {
        onResult(true)
    }

    var isShow by remember { mutableStateOf(!activity.isActiveAccessibilityService<T>()) }
    if (!isShow) return

    val launcherForRequestAccessibilityService =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
            onResult = {
                isShow = false
                onResult(activity.isActiveAccessibilityService<T>())
            }
        )

    DefaultDialogImpl(
        title = title,
        message = message,
        cancelable = cancelable,
        confirm = confirm,
        onConfirm = {
            launcherForRequestAccessibilityService
                .launch(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP))
            false
        },
        cancel = cancel,
        onCancel = {
            onResult(false)
            true
        },
        onDismiss = {
            isShow = false
            onDismiss.invoke()
        }
    )
}


inline fun <reified T : AccessibilityService> Context.isActiveAccessibilityService(): Boolean {
    val isEnabled = runCatching {
        Settings.Secure.getInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED) == 1
    }.getOrDefault(false)
    val accessibilityServiceName = "$packageName/${T::class.java.name}"
    val isRunningService = runCatching {
        Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).split(":").contains(accessibilityServiceName)
    }.getOrDefault(false)

    return isEnabled && isRunningService
}

/**
 * [ShowRequestEnableAccessibilityService] end
 */

@Target(AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)
@StringDef(
    value = [
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    ]
)
private annotation class StoragePermission

@Composable
fun ShowRequestAllFilesPermission(
    title: String? = null,
    message: String,
    @StoragePermission permission: String,
    cancelable: Boolean = true,
    confirm: String = "Apply",
    cancel: String = "Deny",
    onDismiss: (() -> Unit)? = null,
    onResult: (Boolean) -> Unit
) {
    val activity = LocalContext.current as Activity

    val storagePermissionState = getStoragePermissionState(activity, permission)
    if (!storagePermissionState.hasRequestManageExternalStorage && !storagePermissionState.hasRequestPermission) {
        // 관리권한, 쓰기 권한 모두 없으면 그냥 권한 있는걸로 보자
        onResult(true)
        return
    }

    if (storagePermissionState.grantedRequestPermission) {
        onResult(true)
    } else {
        if (storagePermissionState.alreadyDenied) {
            onResult(false)
        }
    }

    var isShow by remember { mutableStateOf(!storagePermissionState.grantedRequestPermission) }
    if (!isShow) return

    val launcherForRequestStorageManager =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
            onResult = {
                isShow = false
                val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Environment.isExternalStorageManager()
                } else {
                    false
                }
                onResult(granted)
            }
        )

    val launcherForRequestStoragePermission =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
            onResult = {
                isShow = false
                onResult(it.values.any { granted -> granted })
            }
        )

    DefaultDialogImpl(
        title = title,
        message = message,
        cancelable = cancelable,
        confirm = confirm,
        onConfirm = {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && storagePermissionState.hasRequestManageExternalStorage ->
                    launcherForRequestStorageManager.launch(
                        Intent(
                            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            Uri.parse("package:${activity.packageName}")
                        )
                    )

                else -> launcherForRequestStoragePermission.launch(
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                )
            }
            false
        },
        cancel = cancel,
        onCancel = {
            onResult(false)
            true
        },
        onDismiss = {
            isShow = false
            onDismiss?.invoke()
        }
    )
}

fun getStoragePermissionState(activity: Activity, permission: String): StoragePermissionState {
    val packageInfo =
        activity.packageManager.getPackageInfo(activity.packageName, PackageManager.GET_PERMISSIONS)
    val requestedPermissions = packageInfo.requestedPermissions ?: emptyArray()

    val hasRequestManageExternalStorage =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && activity.applicationInfo.targetSdkVersion >= Build.VERSION_CODES.R) {
            requestedPermissions.contains(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
        } else {
            false
        }

    val hasRequestStoragePermission = requestedPermissions.contains(permission)
    if (!hasRequestManageExternalStorage && !hasRequestStoragePermission) {
        // 관리권한, 쓰기 권한 모두 없으면 그냥 권한 있는걸로 보자
        return StoragePermissionState(
            grantedRequestPermission = true,
            alreadyDenied = false,
            hasRequestManageExternalStorage = false,
            hasRequestPermission = false
        )
    }

    val grantedRequestPermission =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && activity.applicationInfo.targetSdkVersion >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            activity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        }

    val alreadyDenied =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && activity.applicationInfo.targetSdkVersion >= Build.VERSION_CODES.R) {
            false
        } else {
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }

    if (grantedRequestPermission) {
        return StoragePermissionState(
            grantedRequestPermission = true,
            alreadyDenied = alreadyDenied,
            hasRequestManageExternalStorage = hasRequestStoragePermission,
            hasRequestPermission = hasRequestStoragePermission
        )
    } else {
        if (alreadyDenied) {
            return StoragePermissionState(
                grantedRequestPermission = false,
                alreadyDenied = true,
                hasRequestManageExternalStorage = hasRequestStoragePermission,
                hasRequestPermission = hasRequestStoragePermission
            )
        }
    }

    return StoragePermissionState(
        grantedRequestPermission = false,
        alreadyDenied = false,
        hasRequestManageExternalStorage = hasRequestStoragePermission,
        hasRequestPermission = hasRequestStoragePermission
    )
}

data class StoragePermissionState(
    val grantedRequestPermission: Boolean,
    val alreadyDenied: Boolean,
    val hasRequestManageExternalStorage: Boolean,
    val hasRequestPermission: Boolean
)

@Composable
fun ShowRequestAllFilesWritePermission(
    title: String? = null,
    message: String,
    cancelable: Boolean = true,
    confirm: String = "Apply",
    cancel: String = "Deny",
    onDismiss: (() -> Unit)? = null,
    onResult: (Boolean) -> Unit
) = ShowRequestAllFilesPermission(
    title = title,
    message = message,
    permission = Manifest.permission.WRITE_EXTERNAL_STORAGE,
    cancelable = cancelable,
    confirm = confirm,
    cancel = cancel,
    onDismiss = onDismiss,
    onResult = onResult
)

@Composable
fun ShowRequestAllFilesReadPermission(
    title: String? = null,
    message: String,
    cancelable: Boolean = true,
    confirm: String = "Apply",
    cancel: String = "Deny",
    onDismiss: (() -> Unit)? = null,
    onResult: (Boolean) -> Unit
) = ShowRequestAllFilesPermission(
    title = title,
    message = message,
    permission = Manifest.permission.READ_EXTERNAL_STORAGE,
    cancelable = cancelable,
    confirm = confirm,
    cancel = cancel,
    onDismiss = onDismiss,
    onResult = onResult
)

@Target(AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER)
@StringDef(
    value = [
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_AUDIO
    ]
)
private annotation class ReadMediaPermission

@Composable
private fun ShowRequestMediaReadPermission(
    title: String? = null,
    message: String,
    @ReadMediaPermission permission: String,
    cancelable: Boolean = true,
    confirm: String = "Apply",
    cancel: String = "Deny",
    onDismiss: (() -> Unit)? = null,
    onResult: (Boolean) -> Unit
) {
    val activity = LocalContext.current as Activity
    val packageInfo =
        activity.packageManager.getPackageInfo(activity.packageName, PackageManager.GET_PERMISSIONS)
    val requestedPermissions = packageInfo.requestedPermissions ?: emptyArray()

    val hasRequestManageExternalStorage =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && activity.applicationInfo.targetSdkVersion >= Build.VERSION_CODES.R) {
            requestedPermissions.contains(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
        } else {
            false
        }

    val hasRequestMediaPermission =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestedPermissions.contains(permission)
        } else {
            false
        }
    val hasRequestStoragePermission =
        requestedPermissions.contains(Manifest.permission.READ_EXTERNAL_STORAGE)

    var hasRequestPermission = hasRequestManageExternalStorage

    if (!hasRequestPermission) {
        hasRequestPermission = hasRequestMediaPermission
    }

    if (!hasRequestPermission) {
        hasRequestPermission = hasRequestStoragePermission
    }

    if (!hasRequestPermission) {
        onResult(false)
        return
    }

    var grantedRequestPermission =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && activity.applicationInfo.targetSdkVersion >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            false
        }
    var alreadyDenied = false

    if (!grantedRequestPermission) {
        grantedRequestPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            alreadyDenied =
                ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
            activity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        } else {
            alreadyDenied = ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    if (grantedRequestPermission) {
        onResult(true)
    } else {
        if (alreadyDenied) {
            onResult(false)
        }
    }

    var isShow by remember { mutableStateOf(!grantedRequestPermission) }
    if (!isShow) return

    val launcherForRequestStorageManager =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
            onResult = {
                isShow = false
                val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Environment.isExternalStorageManager()
                } else {
                    false
                }
                onResult(granted)
            }
        )

    val launcherForRequestMediaPermission =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = {
                isShow = false
                onResult(it)
            }
        )

    val launcherForRequestStoragePermission =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions(),
            onResult = {
                isShow = false
                onResult(it.values.any { granted -> granted })
            }
        )

    DefaultDialogImpl(
        title = title,
        message = message,
        cancelable = cancelable,
        confirm = confirm,
        onConfirm = {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && hasRequestManageExternalStorage ->
                    launcherForRequestStorageManager.launch(
                        Intent(
                            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            Uri.parse("package:${activity.packageName}")
                        )
                    )

                hasRequestMediaPermission -> launcherForRequestMediaPermission.launch(permission)
                else -> launcherForRequestStoragePermission.launch(
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                )
            }
            false
        },
        cancel = cancel,
        onCancel = {
            onResult(false)
            true
        },
        onDismiss = {
            isShow = false
            onDismiss?.invoke()
        }
    )
}

fun isGrantedMediaPermission(activity: Activity, permission: String): Boolean {
    val packageInfo =
        activity.packageManager.getPackageInfo(activity.packageName, PackageManager.GET_PERMISSIONS)
    val requestedPermissions = packageInfo.requestedPermissions ?: emptyArray()

    val hasRequestManageExternalStorage =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && activity.applicationInfo.targetSdkVersion >= Build.VERSION_CODES.R) {
            requestedPermissions.contains(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
        } else {
            false
        }

    val hasRequestMediaPermission =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestedPermissions.contains(permission)
        } else {
            false
        }
    val hasRequestStoragePermission =
        requestedPermissions.contains(Manifest.permission.READ_EXTERNAL_STORAGE)

    var hasRequestPermission = hasRequestManageExternalStorage

    if (!hasRequestPermission) {
        hasRequestPermission = hasRequestMediaPermission
    }

    if (!hasRequestPermission) {
        hasRequestPermission = hasRequestStoragePermission
    }

    if (!hasRequestPermission) {
        return false
    }

    var grantedRequestPermission =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && activity.applicationInfo.targetSdkVersion >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            false
        }
    var alreadyDenied = false

    if (!grantedRequestPermission) {
        grantedRequestPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            alreadyDenied =
                ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
            activity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        } else {
            alreadyDenied = ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    if (grantedRequestPermission) {
        return true
    } else {
        if (alreadyDenied) {
            return false
        }
    }

    return false
}

@SuppressLint("InlinedApi")
@Composable
fun ShowRequestVideoReadPermission(
    title: String? = null,
    message: String,
    cancelable: Boolean = true,
    confirm: String = "Apply",
    cancel: String = "Deny",
    onDismiss: (() -> Unit)? = null,
    onResult: (Boolean) -> Unit
) = ShowRequestMediaReadPermission(
    title = title,
    message = message,
    permission = Manifest.permission.READ_MEDIA_VIDEO,
    cancelable = cancelable,
    confirm = confirm,
    cancel = cancel,
    onDismiss = onDismiss,
    onResult = onResult
)

@SuppressLint("InlinedApi")
@Composable
fun ShowRequestImageReadPermission(
    title: String? = null,
    message: String,
    cancelable: Boolean = true,
    confirm: String = "Apply",
    cancel: String = "Deny",
    onDismiss: (() -> Unit)? = null,
    onResult: (Boolean) -> Unit
) = ShowRequestMediaReadPermission(
    title = title,
    message = message,
    permission = Manifest.permission.READ_MEDIA_IMAGES,
    cancelable = cancelable,
    confirm = confirm,
    cancel = cancel,
    onDismiss = onDismiss,
    onResult = onResult
)

@SuppressLint("InlinedApi")
@Composable
fun ShowRequestAudioReadPermission(
    title: String? = null,
    message: String,
    cancelable: Boolean = true,
    confirm: String = "Apply",
    cancel: String = "Deny",
    onDismiss: (() -> Unit)? = null,
    onResult: (Boolean) -> Unit
) = ShowRequestMediaReadPermission(
    title = title,
    message = message,
    permission = Manifest.permission.READ_MEDIA_AUDIO,
    cancelable = cancelable,
    confirm = confirm,
    cancel = cancel,
    onDismiss = onDismiss,
    onResult = onResult
)


//@Composable
//fun ShowRequestPermissionConfirm(
//    title: String? = null,
//    message: String,
//    permissions: Array<String>,
//    cancelable: Boolean = true,
//    checkBox: String? = null,
//    onCheckedChange: ((Boolean) -> Unit)? = null,
//    confirm: String = "Apply",
//    cancel: String = "Deny",
//    onDismiss: (() -> Unit)? = null,
//    onResult: (Boolean) -> Unit
//) {
//    val activity = LocalContext.current as Activity
//    val packageInfo =
//        activity.packageManager.getPackageInfo(activity.packageName, PackageManager.GET_PERMISSIONS)
//    val requestedPermissions = packageInfo.requestedPermissions
//    val existsPermissions = permissions.filter { requestedPermissions?.contains(it) ?: false }
//
//    if (existsPermissions.isEmpty()) {
//        // 요청한 권한중 정의 된 권한이 1개도 없음.
//        onResult(false)
//        return
//    }
//
//    val grantedRequestPermissions = existsPermissions
//        .none { p -> activity.checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED }
//
//    val alreadyDenied = existsPermissions
//        .any { p -> ActivityCompat.shouldShowRequestPermissionRationale(activity, p) }
//
//    if (grantedRequestPermissions) {
//        onResult(true)
//    } else {
//        if (alreadyDenied) {
//            onResult(false)
//        }
//    }
//
//    var isShow by remember { mutableStateOf(!grantedRequestPermissions && !alreadyDenied) }
//    if (!isShow) return
//
//    val requestPermissionsLauncher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.RequestMultiplePermissions(),
//        onResult = {
//            it.values
//                .any { granted -> granted }
//                .apply {
//                    isShow = false
//                    onResult(this)
//                }
//        }
//    )
//
//    ShowConfirmDialog(
//        title = title,
//        message = message,
//        checkBox = checkBox,
//        onCheckedChange = onCheckedChange,
//        cancelable = cancelable,
//        confirm = confirm,
//        onConfirm = {
//            requestPermissionsLauncher.launch(existsPermissions.toTypedArray())
//            false
//        },
//        cancel = cancel,
//        onCancel = {
//            onResult(false)
//            true
//        },
//        onDismiss = {
//            isShow = false
//            onDismiss?.invoke()
//        }
//    )
//}

@Composable
fun ShowErrorDialog(
    title: String? = null,
    error: Throwable?,
    onDismiss: (() -> Unit)? = null
) {
    ShowAlertDialog(
        title = title,
        message = error?.message ?: "unknown error",
        onDismiss = onDismiss
    )
}

@Composable
fun ShowAlertDialog(
    title: String? = null,
    message: String,
    cancelable: Boolean = true,
    confirm: String = "Confirm",
    onConfirm: (() -> Boolean)? = null,
    onDismiss: (() -> Unit)? = null
) {
    var isShow by remember { mutableStateOf(true) }
    if (!isShow) return

    DefaultDialogImpl(
        title = title,
        message = message,
        cancelable = cancelable,
        confirm = confirm,
        onConfirm = { onConfirm?.invoke() ?: true },
        onDismiss = {
            isShow = false
            onDismiss?.invoke()
        }
    )
}

@Preview
@Composable
private fun PreviewShowAlertDialog1() = ShowAlertDialog(
    title = "title",
    message = "message"
)

@Preview
@Composable
private fun PreviewShowAlertDialog2() = ShowAlertDialog(
    message = "message"
)

//@Composable
//fun ShowConfirmDialog(
//    title: String? = null,
//    message: String,
//    checkBox: String? = null,
//    onCheckedChange: ((Boolean) -> Unit)? = null,
//    cancelable: Boolean = true,
//    confirm: String = "Confirm",
//    onConfirm: (() -> Boolean)? = null,
//    neutral: String? = null,
//    onNeutral: (() -> Boolean)? = null,
//    cancel: String = "Cancel",
//    onCancel: (() -> Boolean)? = null,
//    onDismiss: (() -> Unit)? = null
//) {
//    var isShow by remember { mutableStateOf(true) }
//    if (!isShow) return
//
//    DefaultDialogImpl(
//        title = title,
//        message = message,
//        content = if (checkBox != null) {
//            {
//                var checked by remember { mutableStateOf(false) }
//                val view = LocalView.current
//                Checkbox(
//                    checked = checked,
//                    label = checkBox,
//                    spaceWithLabel = 8.dp,
//                    onCheckedChange = {
//                        view.playSoundEffect(SoundEffectConstants.CLICK)
//                        checked = it
//                        onCheckedChange?.invoke(it)
//                    }
//                )
//            }
//        } else null,
//        cancelable = cancelable,
//        confirm = confirm,
//        onConfirm = { onConfirm?.invoke() ?: true },
//        neutral = neutral,
//        onNeutral = onNeutral,
//        cancel = cancel,
//        onCancel = { onCancel?.invoke() ?: true },
//        onDismiss = {
//            isShow = false
//            onDismiss?.invoke()
//        }
//    )
//}

//@Preview
//@Composable
//private fun PreviewShowConfirmDialog1() = ShowConfirmDialog(
//    title = "title",
//    message = "fdsfgrrwfrwfwfweffdsfgrrwfrwfwfweffdsfgrrwfrwfwfweffdsfgrrwfrwfwfweffdsfgrrwfrwfwfweffdsfgrrwfrwfwfweffdsfgrrwfrwfwfwef"
//)

//@Preview
//@Composable
//private fun PreviewShowConfirmDialog2() = ShowConfirmDialog(
//    message = "message",
//    cancel = "c"
//)

//@Preview
//@Composable
//private fun PreviewShowConfirmDialog3() = ShowConfirmDialog(
//    title = "title",
//    message = "fdsfgrrwfrwfwfweffdsfgrrwfrwfwfweffdsfgrrwfrwfwfweffdsfgrrwfrwfwfweffdsfgrrwfrwfwfweffdsfgrrwfrwfwfweffdsfgrrwfrwfwfwef",
//    checkBox = "오늘 다시"
//)

//@Composable
//fun ShowSelectSingleDialog(
//    title: String? = null,
//    message: String? = null,
//    cancelable: Boolean = true,
//    initialSelectedIndex: Int,
//    list: List<String>,
//    confirm: String = "Confirm",
//    onConfirm: ((Int) -> Boolean)? = null,
//    cancel: String = "Cancel",
//    onCancel: (() -> Boolean)? = null,
//    onDismiss: (() -> Unit)? = null
//) {
//    var isShow by remember { mutableStateOf(true) }
//    if (!isShow) return
//
//    var selected by remember { mutableIntStateOf(initialSelectedIndex) }
//    var enabledConfirm by remember { mutableStateOf(true) }
//    enabledConfirm = initialSelectedIndex != selected
//
//    DefaultDialogImpl(
//        title = title,
//        message = message,
//        cancelable = cancelable,
//        confirm = confirm,
//        enabledConfirm = enabledConfirm,
//        onConfirm = { onConfirm?.invoke(selected) ?: true },
//        cancel = cancel,
//        onCancel = { onCancel?.invoke() ?: true },
//        onDismiss = {
//            isShow = false
//            onDismiss?.invoke()
//        },
//        content = {
//            RadioGroup(
//                modifier = Modifier.fillMaxWidth(),
//                list = list,
//                spaceWithLabel = 8.dp,
//                selected = initialSelectedIndex,
//                onSelectedIndex = { selected = it }
//            )
//        }
//    )
//}

//@Preview
//@Composable
//private fun PreviewShowSelectSingleDialog() = ShowSelectSingleDialog(
//    title = "asda",
//    message = "asdasdasdsa",
//    initialSelectedIndex = 0,
//    list = listOf("1", "2", "3"),
//    onConfirm = { true }
//)

//@Composable
//fun ShowSelectMultipleDialog(
//    title: String? = null,
//    message: String? = null,
//    cancelable: Boolean = true,
//    initialSelectedIndex: List<Int>,
//    list: List<String>,
//    confirm: String = "Confirm",
//    onConfirm: ((List<Int>) -> Boolean)? = null,
//    cancel: String = "Cancel",
//    onCancel: (() -> Boolean)? = null,
//    onDismiss: (() -> Unit)? = null
//) {
//    var isShow by remember { mutableStateOf(true) }
//    if (!isShow) return
//
//    val selected = remember { mutableStateListOf<Int>().apply { addAll(initialSelectedIndex) } }
//    var enabledConfirm by remember { mutableStateOf(true) }
//    enabledConfirm = selected.toSet() != initialSelectedIndex.toSet()
//
//    DefaultDialogImpl(
//        title = title,
//        message = message,
//        cancelable = cancelable,
//        confirm = confirm,
//        enabledConfirm = enabledConfirm,
//        onConfirm = { onConfirm?.invoke(selected) ?: true },
//        cancel = cancel,
//        onCancel = { onCancel?.invoke() ?: true },
//        onDismiss = {
//            isShow = false
//            onDismiss?.invoke()
//        },
//        content = {
//            Column(
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                list.forEachIndexed { index, s ->
//                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
//
//                        val view = LocalView.current
//
//                        Card(
//                            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
//                            shape = CircleShape,
//                            onClick = {
//                                view.playSoundEffect(SoundEffectConstants.CLICK)
//                                if (selected.contains(index)) {
//                                    selected.remove(index)
//                                } else {
//                                    selected.add(index)
//                                }
//                            }
//                        ) {
//                            Checkbox(
//                                checked = selected.contains(index),
//                                label = s,
//                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
//                                spaceWithLabel = 8.dp,
//                                onCheckedChange = null
//                            )
//                        }
//                    }
//                }
//            }
//        }
//    )
//}

//@Preview
//@Composable
//private fun PreviewShowSelectMultipleDialog() = ShowSelectMultipleDialog(
//    title = "asda",
//    message = "asdasdasdsa",
//    initialSelectedIndex = listOf(0, 2),
//    list = listOf("1", "2", "3"),
//    onConfirm = { true }
//)

//@Composable
//fun ShowInputDialog(
//    title: String? = null,
//    message: String? = null,
//    placeHolder: String? = null,
//    text: String? = null,
//    onFocusedText: ((TextFieldValue) -> TextFieldValue)? = null,
//    visualTransformation: VisualTransformation = VisualTransformation.None,
//    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
//    keyboardActions: KeyboardActions = KeyboardActions.Default,
//    error: String? = null,
//    loading: Boolean = false,
//    cancelable: Boolean = true,
//    confirm: String = "Confirm",
//    onConfirm: ((String) -> Boolean)? = null,
//    cancel: String = "Cancel",
//    onCancel: (() -> Boolean)? = null,
//    onDismiss: (() -> Unit)? = null
//) {
//    var isShow by remember { mutableStateOf(true) }
//    if (!isShow) return
//
//    var inputText by remember { mutableStateOf(TextFieldValue(text ?: "")) }
//    var state by remember { mutableStateOf<FocusState?>(null) }
//
//    LaunchedEffect(state) {
//        if (state?.isFocused == true && onFocusedText != null) {
//            inputText = onFocusedText.invoke(inputText)
//        }
//    }
//
//    DefaultDialogImpl(
//        title = title,
//        message = message,
//        confirm = confirm,
//        onConfirm = {
//            onConfirm?.invoke(inputText.text) ?: true
//        },
//        loading = loading,
//        cancelable = cancelable,
//        cancel = cancel,
//        onCancel = {
//            onCancel?.invoke() ?: true
//        },
//        onDismiss = {
//            isShow = false
//            onDismiss?.invoke()
//        },
//        enabledConfirm = inputText.text.isNotEmpty(),
//        content = {
//            SecFileOutlinedTextField(
//                modifier = Modifier.onFocusChanged { state = it },
//                singleLine = true,
//                value = inputText,
//                isError = error != null,
//                onValueChange = { inputText = it },
//                label = placeHolder?.let { { Text(text = it) } },
//                supportingText = { error?.let { Text(text = it) } },
//                visualTransformation = visualTransformation,
//                keyboardActions = keyboardActions,
//                keyboardOptions = keyboardOptions
//            )
//        }
//    )
//}

//@Preview
//@Composable
//private fun PreviewShowInputDialog() {
//    ShowInputDialog(
//        title = "asd",
//        message = "asdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasdasd",
//        placeHolder = "place"
//    )
//}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultDialogImpl(
    modifier: Modifier = Modifier,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    theme: MaterialTheme = MaterialTheme,
    dialogPadding: PaddingValues = DialogPadding,
    title: String? = null,
    titlePadding: PaddingValues = TitlePadding,
    message: String? = null,
    textPadding: PaddingValues = TextPadding,
    content: @Composable (() -> Unit)? = null,
    cancelable: Boolean = true,
    loading: Boolean = false,
    confirm: String = "Confirm",
    onConfirm: () -> Boolean,
    neutral: String? = null,
    onNeutral: (() -> Boolean)? = null,
    enabledConfirm: Boolean = true,
    cancel: String = "Cancel",
    onCancel: (() -> Boolean)? = null,
    enabledCancel: Boolean = true,
    onDismiss: (() -> Unit)? = null
) {
    BasicAlertDialog(
        onDismissRequest = { if (cancelable && !loading) onDismiss?.invoke() }
    ) {

        Surface(
            modifier = modifier,
            shape = shape,
            color = containerColor,
            tonalElevation = tonalElevation,
        ) {
            Column(modifier = Modifier.padding(dialogPadding)) {
                title?.let {
                    val mergedStyle = LocalTextStyle.current.merge(theme.typography.headlineSmall)
                    CompositionLocalProvider(
                        LocalContentColor provides MaterialTheme.colorScheme.primary,
                        LocalTextStyle provides mergedStyle,
                        content = {
                            Box(
                                Modifier
                                    .padding(titlePadding)
                                    .align(Alignment.Start)
                            ) {
                                Text(text = it)
                            }
                        }
                    )
                }

                message?.let {
                    val mergedStyle = LocalTextStyle.current.merge(theme.typography.bodyMedium)
                    CompositionLocalProvider(
                        LocalContentColor provides MaterialTheme.colorScheme.primary,
                        LocalTextStyle provides mergedStyle,
                        content = {
                            Box(
                                Modifier
                                    .weight(weight = 1f, fill = false)
                                    .padding(textPadding)
                                    .align(Alignment.Start)
                            ) {
                                Text(text = it)
                            }
                        }
                    )
                }

                content?.let {
                    val mergedStyle = LocalTextStyle.current.merge(theme.typography.bodyMedium)
                    CompositionLocalProvider(
                        LocalContentColor provides MaterialTheme.colorScheme.primary,
                        LocalTextStyle provides mergedStyle,
                        content = {
                            Box(
                                Modifier
                                    .padding(textPadding)
                                    .align(Alignment.Start)
                            ) {
                                it.invoke()

                                if (loading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .width(40.dp)
                                            .align(Alignment.Center),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 5.dp
                                    )
                                }

                            }
                        }
                    )
                }

                Box(modifier = Modifier.align(Alignment.End)) {

                    Row {
                        val mergedStyle =
                            LocalTextStyle.current.merge(theme.typography.bodySmall)

                        onCancel?.let {
                            CompositionLocalProvider(
                                LocalContentColor provides MaterialTheme.colorScheme.primary,
                                LocalTextStyle provides mergedStyle,
                                content = {
                                    val view = LocalView.current
                                    DialogTextButton(
                                        enabled = enabledCancel && !loading,
                                        text = cancel,
                                        onClick = {
                                            view.playSoundEffect(SoundEffectConstants.CLICK)
                                            if (onCancel.invoke()) {
                                                onDismiss?.invoke()
                                            }
                                        }
                                    )
                                }
                            )
                        }

                        neutral?.let {
                            Spacer(modifier = Modifier.weight(1f))
                            CompositionLocalProvider(
                                LocalContentColor provides MaterialTheme.colorScheme.primary,
                                LocalTextStyle provides mergedStyle,
                                content = {
                                    val view = LocalView.current
                                    DialogTextButton(
                                        enabled = enabledConfirm && !loading,
                                        text = it,
                                        onClick = {
                                            view.playSoundEffect(SoundEffectConstants.CLICK)
                                            onNeutral?.invoke() ?: onDismiss?.invoke()
                                        }
                                    )
                                }
                            )
                        }

                        CompositionLocalProvider(
                            LocalContentColor provides MaterialTheme.colorScheme.primary,
                            LocalTextStyle provides mergedStyle,
                            content = {
                                val view = LocalView.current
                                DialogTextButton(
                                    enabled = enabledConfirm && !loading,
                                    text = confirm,
                                    onClick = {
                                        view.playSoundEffect(SoundEffectConstants.CLICK)
                                        if (onConfirm.invoke()) {
                                            onDismiss?.invoke()
                                        }
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogTextButton(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    text: String,
    onClick: () -> Unit
) {
    TextButton(
        modifier = modifier,
        colors = ButtonDefaults.textButtonColors(
            disabledContentColor = MaterialTheme.colorScheme.inversePrimary
        ),
        enabled = enabled,
        onClick = onClick
    ) {
        Text(text = text)
    }
}

private val DialogPadding = PaddingValues(all = 24.dp)
private val TitlePadding = PaddingValues(bottom = 16.dp)
private val TextPadding = PaddingValues(bottom = 24.dp)