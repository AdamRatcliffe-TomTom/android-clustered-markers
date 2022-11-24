package com.example.randommarkers

import android.graphics.PointF
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tomtom.sdk.common.location.GeoBoundingBox
import com.tomtom.sdk.common.location.GeoPoint
import com.tomtom.sdk.map.display.MapOptions
import com.tomtom.sdk.map.display.camera.CameraOptions
import com.tomtom.sdk.map.display.common.Color
import com.tomtom.sdk.map.display.common.screen.Padding
import com.tomtom.sdk.map.display.image.ImageFactory
import com.tomtom.sdk.map.display.marker.Label
import com.tomtom.sdk.map.display.marker.MarkerOptions
import com.tomtom.sdk.map.display.ui.MapFragment
import com.yeo.javasupercluster.SuperCluster
import org.wololo.geojson.Feature
import org.wololo.geojson.Point
import java.util.*
import kotlin.math.roundToInt
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private lateinit var tomTomMap: com.tomtom.sdk.map.display.TomTomMap
    private lateinit var cluster: SuperCluster
    private val bounds: GeoBoundingBox =
        GeoBoundingBox(GeoPoint(38.2033, -122.6445), GeoPoint(37.1897, -121.5871))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initMap()
    }

    private fun initMap() {
        val mapOptions = MapOptions(
            mapKey = resources.getString(R.string.API_KEY),
            padding = Padding(48, 48, 48, 48),
            cameraOptions = CameraOptions(
                position = getBoundingBoxCenter(bounds),
                zoom = 5.0
            )
        )
        val mapFragment = MapFragment.newInstance(mapOptions)
        supportFragmentManager.beginTransaction()
            .replace(R.id.map_container, mapFragment)
            .commit()

        mapFragment.getMapAsync { map ->
            tomTomMap = map
            tomTomMap.isMarkersFadingEnabled = false
            tomTomMap.isMarkersShrinkingEnabled = false

            initCluster()
            updateMarkers()
            addMapListeners()
        }

    }

    private fun initCluster() {
        val features = mutableListOf<Feature>()

        for (i in 0..NUM_MARKERS) {
            val lat =
                Random.nextFloat() * (bounds.topLeft.latitude - bounds.bottomRight.latitude) + bounds.bottomRight.latitude
            val lng =
                Random.nextFloat() * (bounds.bottomRight.longitude - bounds.topLeft.longitude) + bounds.topLeft.longitude
            val point = Point(doubleArrayOf(lng, lat))
            val feature = Feature(point, mapOf("id" to randomUUID()))
            features.add(feature)
        }

        cluster = SuperCluster(40, 256, 0, 14, 64, features.toTypedArray())
    }

    private fun updateMarkers() {
        val bbox = boundsToBBox(tomTomMap.visibleRegion.value().bounds)
        val zoom = tomTomMap.cameraPosition().zoom.toInt()
        val clusters = cluster.getClusters(
            bbox,
            zoom
        )
        val clusterCoords = clusters.map { cluster: Feature ->
            val (lng, lat) = (cluster.geometry as Point).coordinates
            GeoPoint(lat, lng)
        }

        // Remove any markers for which we don't have a cluster at the marker location
        for (marker in tomTomMap.markers.toList()) {
            if (clusterCoords.all { marker.coordinate != it }) {
                marker.remove()
            }
        }

        // Remove any clusters for which we've already created a marker at the cluster location
        // with the cluster point count
        val clustersToRemove = mutableListOf<Feature>()

        for (i in clusterCoords.indices) {
            val clusterCoord = clusterCoords[i]
            val cluster = clusters[i]
            for (marker in tomTomMap.markers) {
                if (marker.coordinate == clusterCoord && cluster.properties["point_count"] == marker.tag) {
                    clustersToRemove.add(clusters[i])
                }
            }
        }

        clusters.removeAll(clustersToRemove)

        // Add markers for the remaining clusters
        for (cluster in clusters) {
            addMarker(cluster as Feature)
        }
    }

    private fun addMapListeners() {
        tomTomMap.addOnCameraSteadyListener { updateMarkers() }
    }

    private fun addMarker(feature: Feature) {
        val (lng, lat) = (feature.geometry as Point).coordinates
        val pointCount = feature.properties["point_count"]
        val isCluster = pointCount != null
        val point = GeoPoint(lat, lng)
        val markerOptions = when {
            isCluster -> MarkerOptions(
                coordinate = point,
                pinImage = ImageFactory.fromResource(R.drawable.ic_ellipse),
                label = Label(
                    text = formatNumber(pointCount as Int),
                    textColor = Color.WHITE,
                    offset = PointF(0F, 16F)
                ),
                tag = pointCount
            )
            else -> MarkerOptions(
                coordinate = point,
                pinImage = ImageFactory.fromResource(R.drawable.ic_marker_pin),
            )
        }

        tomTomMap.addMarker(markerOptions)
    }

    private fun getBoundingBoxCenter(bounds: GeoBoundingBox): GeoPoint {
        val lat =
            bounds.bottomRight.latitude + (bounds.topLeft.latitude - bounds.bottomRight.latitude) / 2
        val lng =
            bounds.topLeft.longitude + (bounds.bottomRight.longitude - bounds.topLeft.longitude) / 2
        return GeoPoint(lat, lng)
    }

    private fun boundsToBBox(bounds: GeoBoundingBox): DoubleArray {
        return doubleArrayOf(
            bounds.topLeft.longitude,
            bounds.bottomRight.latitude,
            bounds.bottomRight.longitude,
            bounds.topLeft.latitude
        )
    }

    private fun formatNumber(num: Int): String {
        return if (num >= 1000) {
            "${(num.toDouble() / 1000).roundToInt()}K"
        } else {
            num.toString()
        }
    }

    private fun randomUUID() = UUID.randomUUID().toString()

    companion object {
        private const val NUM_MARKERS = 100000
    }
}