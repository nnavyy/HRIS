package com.ptniger.hris.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun OsmMapView(
    modifier: Modifier = Modifier,
    initialLat: Double = -6.2088,
    initialLng: Double = 106.8456,
    onLocationSelected: (Double, Double) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var mapView by remember { mutableStateOf<org.osmdroid.views.MapView?>(null) }
    
    LaunchedEffect(Unit) {
        org.osmdroid.config.Configuration.getInstance().userAgentValue = context.packageName
    }

    AndroidView(
        factory = { ctx ->
            org.osmdroid.views.MapView(ctx).apply {
                setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                controller.setZoom(15.0)
                controller.setCenter(org.osmdroid.util.GeoPoint(initialLat, initialLng))
                
                val marker = org.osmdroid.views.overlay.Marker(this)
                marker.position = org.osmdroid.util.GeoPoint(initialLat, initialLng)
                marker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM)
                overlays.add(marker)

                val overlayEvents = org.osmdroid.views.overlay.MapEventsOverlay(object : org.osmdroid.events.MapEventsReceiver {
                    override fun singleTapConfirmedHelper(p: org.osmdroid.util.GeoPoint): Boolean {
                        marker.position = p
                        invalidate()
                        onLocationSelected(p.latitude, p.longitude)
                        return true
                    }
                    override fun longPressHelper(p: org.osmdroid.util.GeoPoint): Boolean {
                        marker.position = p
                        invalidate()
                        onLocationSelected(p.latitude, p.longitude)
                        return true
                    }
                })
                overlays.add(overlayEvents)
                mapView = this
            }
        },
        modifier = modifier
    ) { view ->
        // view updates
    }
    
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView?.onDetach()
        }
    }
}
