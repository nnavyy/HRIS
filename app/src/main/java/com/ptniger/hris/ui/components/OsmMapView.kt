package com.ptniger.hris.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun OsmMapView(
    modifier: Modifier = Modifier,
    targetLat: Double = -6.2088,
    targetLng: Double = 106.8456,
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
                controller.setCenter(org.osmdroid.util.GeoPoint(targetLat, targetLng))
                
                val marker = org.osmdroid.views.overlay.Marker(this)
                marker.id = "main_marker"
                marker.position = org.osmdroid.util.GeoPoint(targetLat, targetLng)
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
        val geoPoint = org.osmdroid.util.GeoPoint(targetLat, targetLng)
        view.controller.animateTo(geoPoint)
        
        val marker = view.overlays.filterIsInstance<org.osmdroid.views.overlay.Marker>().firstOrNull { it.id == "main_marker" }
        if (marker != null) {
            marker.position = geoPoint
            view.invalidate()
        }
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
