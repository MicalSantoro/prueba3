package com.example.prueba3

import android.Manifest
import android.content.ClipData
import android.content.Context
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OnImageSavedCallback
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.expandHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File


enum class Pantallas {
    FORMULARIO,
    CAMARA,
    MAPA,
    IMAGEN
}
class PermisosVM: ViewModel() { //para guardar los permisos
    val pantallaActual = mutableStateOf(Pantallas.FORMULARIO)
    val onCameraPermissionOk:() -> Unit = {}
    var locationPermissionOk:() -> Unit = {}
}

class RegistroVM: ViewModel() {//guardar los valores de las variables que cambian
val lugar = mutableStateOf("")
    val foto = mutableStateOf<Uri?>(null)
    val latitud = mutableStateOf(0.0);
    val longitud = mutableStateOf(0.0);
}

class MainActivity : ComponentActivity() { //llamando a las clases viewmodels creadas

    val cameraVm:  PermisosVM by viewModels()
    val registroVm: RegistroVM by viewModels()
    val permisosVM: PermisosVM by viewModels()


    lateinit var cameraController:LifecycleCameraController

    val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permisosVM.locationPermissionOk()
        if(it[Manifest.permission.CAMERA] == true) {
            cameraVm.onCameraPermissionOk()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraController = LifecycleCameraController(this)
        cameraController.bindToLifecycle(this)
        cameraController.cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        setContent {
            AppUI(permissionLauncher, cameraController)
        }
    }
}

@Composable
fun AppUI(
    permissionLauncher: ActivityResultLauncher<Array<String>>,
    cameraController: LifecycleCameraController,
) {
    val permisosVM:  PermisosVM = viewModel()
    val registroVM:  RegistroVM = viewModel()

    when(permisosVM.pantallaActual.value) {
        Pantallas.FORMULARIO -> {
            FormularioUI(permisosVM = permisosVM, registroVM = registroVM)
        }
        Pantallas.CAMARA-> {
            PCamaraUI(permissionLauncher = permissionLauncher, cameraController = cameraController, permisosVM = permisosVM, registroVM = registroVM)
        }
        Pantallas.MAPA -> {
            MapaOsmUI(permisosVM = permisosVM, registroVM = registroVM, permissionLauncher = permissionLauncher)
        }
        Pantallas.IMAGEN -> {
            VistaFotoUI(permisosVM = permisosVM, registroVM = registroVM)
        }
    }


}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FormularioUI(permisosVM: PermisosVM, registroVM: RegistroVM) {
    var lugar by remember { mutableStateOf("") }


    LazyColumn(
        modifier = Modifier.fillMaxWidth()){
        stickyHeader {
            Text(

                "Registro de Viajes",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .background(Color.LightGray)
                    .padding(all = 25.dp)
                    .fillMaxWidth(),



                )}
    }
    Column(

        modifier = Modifier.padding(all =90.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally

    ) {
        TextField(
            value = lugar,
            onValueChange = { lugar = it},
            label = { Text("¿Qué lugar ha visitado?") },
            keyboardActions = KeyboardActions(
                onDone = {
                    registroVM.lugar.value = lugar
                }
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done
            )
        )

        Spacer(modifier =Modifier.height(50.dp))

        Button(onClick =  { permisosVM.pantallaActual.value = Pantallas.CAMARA}
        ) {
            Text(text = "Agregar Foto")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            registroVM.foto.value?.let {
                Image(
                    painter = BitmapPainter(uri2imageBitmap(it, LocalContext.current)),
                    contentDescription = "Foto",
                    modifier = Modifier
                        .size(60.dp)
                        .clickable { permisosVM.pantallaActual.value = Pantallas.IMAGEN }
                )
            }

            Spacer(modifier =Modifier.height(50.dp))
            Column(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = registroVM.lugar.value,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Text(
                    text = "Latitud: ${registroVM.latitud.value} Longitud: ${registroVM.longitud.value}",
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        // Botón para ver el mapa
        Button(
            onClick = {
                permisosVM.pantallaActual.value = Pantallas.MAPA
            }
        ) {
            Text("Agregar Ubicacion")

}
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PCamaraUI(cameraController:LifecycleCameraController, permissionLauncher: ActivityResultLauncher<Array<String>>,
             permisosVM: PermisosVM, registroVM: RegistroVM){
    permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))

    val context = LocalContext.current

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            PreviewView(it).apply { controller = cameraController }
        })
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Bottom){
        stickyHeader() {
            Image(painter = painterResource(id = R.drawable.camara),
                contentDescription = "Tomar foto",
                modifier = Modifier
                    .background(Color.Gray)
                    .padding(all = 10.dp)
                    .fillMaxWidth()
                    .clickable {
                        tomarFoto(
                            cameraController = cameraController,
                            file = makePublicPhotoFile(context),
                            context = context
                        )
                        {
                            registroVM.foto.value = it
                            permisosVM.pantallaActual.value = Pantallas.FORMULARIO
                        }
                    }

            )
        }
    }
}

@Composable
fun MapaOsmUI(permisosVM: PermisosVM, registroVM: RegistroVM, permissionLauncher: ActivityResultLauncher<Array<String>>) {
    val context = LocalContext.current

    permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,
                                      Manifest.permission.ACCESS_COARSE_LOCATION))

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally

    ) {

        Button(onClick = { permisosVM.pantallaActual.value = Pantallas.FORMULARIO})
        {

            Text("Volver")
        }
        Spacer(modifier =Modifier.height(70.dp))

        Button(onClick = { //funcionalidad del mapa
            Log.d("Location", "Lat: ${registroVM.latitud.value} Long: ${registroVM.longitud.value}")
            getLocation(context) {
                if (it != null) {
                    registroVM.latitud.value = it.latitude
                }
                if (it != null) {
                    registroVM.longitud.value = it.longitude
                }
                Log.d("Location", "Lat: ${registroVM.latitud.value} Long: ${registroVM.longitud.value}")

            }}) {
            Text("Tomar Ubicación")
        }

        Text(
            text = "Latitud: ${registroVM.latitud.value} Longitud: ${registroVM.longitud.value}",
            modifier = Modifier.padding(vertical = 4.dp)
        )
        Spacer(modifier =Modifier.height(100.dp))

        AndroidView(factory = { MapView(it).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            Configuration.getInstance().userAgentValue = context.packageName
            controller.setZoom(15.0)
        }
        }, update = {
            it.overlays.removeIf{true}
            it.invalidate()

            val geoPoint = GeoPoint(registroVM.latitud.value, registroVM.longitud.value)
            it.controller.animateTo(geoPoint)

            val marker = Marker(it)
            marker.position = geoPoint
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            it.overlays.add(marker)
        })


    }

}


@Composable
fun VistaFotoUI(registroVM: RegistroVM, permisosVM: PermisosVM) {
    registroVM.foto.value?.let {
        Image(
            painter = BitmapPainter(uri2imageBitmap(it, LocalContext.current)),
            contentDescription = "Foto",
            modifier = Modifier
                .fillMaxSize()
                .clickable { //permite que al clickear al fotografía, esta se muestre en grande
                    permisosVM.pantallaActual.value = Pantallas.FORMULARIO
                }
        )
    }
}


//funciones secundarias

fun uri2imageBitmap(uri: Uri, context: Context) = BitmapFactory.decodeStream(
    context.contentResolver.openInputStream(uri)
).asImageBitmap()



fun tomarFoto(
    cameraController: LifecycleCameraController,
    file: File,
    context: Context,
    onCaptureImage: (Uri) -> Unit
) {
    val options = ImageCapture.OutputFileOptions.Builder(file).build()

    cameraController.takePicture(
        options,
        ContextCompat.getMainExecutor(context),
        object: OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                outputFileResults.savedUri?.let {
                    onCaptureImage(it)
                }
            }
            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraX", "Error al tomar la foto", exception)
            }
        }
    )
}

fun makePublicPhotoFile(context: Context): File = File(
    context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
    "${System.currentTimeMillis()}.jpg"
)

fun getLocation(context: Context, onSuccess: (location: Location?) -> Unit) {
    try {
        val service = LocationServices.getFusedLocationProviderClient(context)
        val task = service.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        )
        task.addOnSuccessListener { location ->
            Log.d("Location", "Location retrieved: $location")
            onSuccess(location)
        }
        task.addOnFailureListener { exception ->
            Log.e("Location", "Failed to retrieve location: $exception")
            onSuccess(null)
        }
    } catch (e: SecurityException) {
        Log.e("Location", "Failed to retrieve location: $e")
        onSuccess(null)
    }
}

