package com.example.findpokemons

// Importaciones necesarias
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*

data class ImageWithLocation(val image: Bitmap, var location: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FindPokemonsApp()
        }
    }
}

@Composable
fun FindPokemonsApp() {
    val isWelcomeScreen = remember { mutableStateOf(true) }

    if (isWelcomeScreen.value) {
        WelcomeScreen(onStartClicked = { isWelcomeScreen.value = false })
    } else {
        CameraScreen()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val imageList = remember { mutableStateListOf<ImageWithLocation>() }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Configuración para solicitar actualizaciones de ubicación
    val locationRequest = remember {
        LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = Priority.PRIORITY_HIGH_ACCURACY
            maxWaitTime = 15000
        }
    }

    // Callback para recibir actualizaciones de ubicación
    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                val locationText = if (location != null) {
                    "Lat: ${location.latitude}, Lon: ${location.longitude}"
                } else {
                    "Ubicación no disponible"
                }
                // Actualizar la última imagen con la nueva ubicación
                if (imageList.isNotEmpty()) {
                    val lastImage = imageList.last()
                    lastImage.location = locationText
                }
                // Detener las actualizaciones de ubicación después de obtenerla
                fusedLocationClient.removeLocationUpdates(this)
            }
        }
    }

    // Lanzador para solicitar el permiso de ubicación si no está concedido
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val locationText = "Lat: ${location.latitude}, Lon: ${location.longitude}"
                    if (imageList.isNotEmpty()) {
                        val lastImage = imageList.last()
                        lastImage.location = locationText
                    }
                } else {
                    fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        Looper.getMainLooper()
                    )
                }
            }
        } else {
            // Manejar el caso en que el permiso no fue concedido
            if (imageList.isNotEmpty()) {
                val lastImage = imageList.last()
                lastImage.location = "Permiso de ubicación denegado"
            }
        }
    }

    // Lanzador para iniciar la actividad de la cámara y manejar el resultado
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            imageBitmap?.let { bitmap ->
                // Agregar imagen a la lista con mensaje temporal
                imageList.add(ImageWithLocation(bitmap, "Permiso de ubicación no concedido"))

                // Verificar si el permiso de ubicación está concedido
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null) {
                            val locationText = "Lat: ${location.latitude}, Lon: ${location.longitude}"
                            if (imageList.isNotEmpty()) {
                                val lastImage = imageList.last()
                                lastImage.location = locationText
                            }
                        } else {
                            // Solicitar actualizaciones de ubicación si lastLocation es null
                            fusedLocationClient.requestLocationUpdates(
                                locationRequest,
                                locationCallback,
                                Looper.getMainLooper()
                            )
                        }
                    }
                } else {
                    // Solicitar el permiso de ubicación
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
        }
    }

    // Lanzador para solicitar el permiso de cámara y ubicación
    val multiplePermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false

        if (cameraGranted) {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraLauncher.launch(cameraIntent)
        } else {

        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "¡Captura a tu Pokémon!",
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
        )

        Button(
            onClick = {
                val cameraPermissionGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED

                if (cameraPermissionGranted) {
                    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    cameraLauncher.launch(cameraIntent)
                } else {
                    // Solicitar permisos de cámara y ubicación
                    multiplePermissionsLauncher.launch(
                        arrayOf(
                            Manifest.permission.CAMERA,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        )
                    )
                }
            },
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .height(48.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                "Capturar Imagen",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(imageList.size) { index ->
                Column(
                    modifier = Modifier
                        .padding(4.dp)
                        .graphicsLayer {
                            clip = true
                            shape = RoundedCornerShape(8.dp)
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        bitmap = imageList[index].image.asImageBitmap(),
                        contentDescription = "Imagen capturada",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                    )
                    Text(
                        text = imageList[index].location,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun WelcomeScreen(onStartClicked: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFB3E5FC))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "FindPokemons",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Button(
                onClick = onStartClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "¡Empezar!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
