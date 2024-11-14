package com.example.findpokemons

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
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
import androidx.compose.ui.unit.*
import androidx.core.content.ContextCompat
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

data class ImageWithLocation(val image: Bitmap, val location: String)

class MainActivity : ComponentActivity() {

    private val requestLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // Puedes manejar la respuesta del permiso aquí si lo deseas
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Solicitar permiso de ubicación si no está concedido
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

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

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            imageBitmap?.let { bitmap ->
                // Verificar si el permiso de ubicación está concedido
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        val locationText = if (location != null) {
                            "Lat: ${location.latitude}, Lon: ${location.longitude}"
                        } else {
                            "Ubicación no disponible"
                        }
                        // Agregar imagen y ubicación a la lista
                        imageList.add(ImageWithLocation(bitmap, locationText))
                    }
                } else {
                    // Permiso no concedido, agregar imagen sin ubicación
                    imageList.add(ImageWithLocation(bitmap, "Permiso de ubicación no concedido"))
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraLauncher.launch(cameraIntent)
        } else {
            // Manejar el caso en que el permiso no fue concedido
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
                when (PackageManager.PERMISSION_GRANTED) {
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) -> {
                        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        cameraLauncher.launch(cameraIntent)
                    }
                    else -> {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
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
