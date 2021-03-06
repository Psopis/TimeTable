package com.example.timetable.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.coroutineScope
import androidx.navigation.fragment.navArgs
import com.example.timetable.*
import com.example.timetable.data.Route
import com.example.timetable.worker.BusStopsBottomSheet
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.flow.collect


@RequiresApi(Build.VERSION_CODES.M)
class MapsFragment : Fragment() {
    private var busMarker: Marker? = null

    private val args: MapsFragmentArgs by navArgs()

    private val viewModel: MapViewModel by viewModels {
        MapViewModelFactory(
            (activity?.application as App).database
                .routeDao()
        )
    }

    lateinit var googleMap: GoogleMap

    var flight/*: Flight*/: Route? = null

    lateinit var findBusButton: Button

    private val callback = OnMapReadyCallback { google_map ->
        googleMap = google_map // ассинхронный вызов - в другом потоке

        lifecycle.coroutineScope.launchWhenStarted {
            viewModel.getFlight(args.id).also {
                if (it != null) {
                    flight = it
                    (requireActivity() as MainActivity).setActionBarTitle(flight?.name)
                    mapReady()
                    Log.d("response_server", "data (flight) Ready")
                } else
                    Log.d("response_server", "data (flight) is null")
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        var root = inflater.inflate(R.layout.fragment_maps, container, false)

        findBusButton = root.findViewById(R.id.findBus_fragment_map)
        findBusButton.setOnClickListener {
            moveCamera(busMarker?.position)
//            viewModel.addRoute(flight)
            Log.d("findbusbtn", busMarker.toString())
        }
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.map_fragment_map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun mapReady() // это вызывается когда данные карт получены и можно работать (аналог onCreate)
    {
        val route = flight/*.route*/

        if (!route?.id.isNullOrEmpty())
            startListeningTracker(route!!.id) // включаю вебсокет

        val polylineOptions = PolylineOptions() // это будет маршрут (ломаная линия)
        polylineOptions.color(requireContext().getColor(R.color.polyline))
        moveCamera(route!!.points[0].toLatLng())// перемещаем камеру на первую остановку

        route.points.forEach { polylineOptions.add(it.toLatLng()) } // добавляем точки для линии
        val polyline = googleMap.addPolyline(polylineOptions) // добавляем линию (маршрут) на карту

        val busStops = route.busStopsWithTime
        for (i in busStops.indices) // добавляем маркеры на карту
        {
            var marker = googleMap.addMarker(
                MarkerOptions()
                    .position(
                        LatLng(
                            busStops[i].busStop?.point!!.latitude,
                            busStops[i].busStop?.point!!.longitude
                        )
                    )
                    .title(busStops[i].busStop?.name)
            )
                ?.setTag(i) // в тэг сохраняем индекс данных, потом по этому индексу будем находить даннные в массиве (ти-па привязки данных к маркеру)
        }

        googleMap.setOnMarkerClickListener { marker -> // при нажатии на маркер
            if (marker.tag != null)
                BusStopsBottomSheet(marker.tag as Int, busStops)
                    .show(requireFragmentManager(), "BottomSheetDialog")
            true
        }


    }

    private fun startListeningTracker(trackerId: String)
    {
        Log.d("startListeningTracker", "id = $trackerId")
        val busMarkerIcon: BitmapDescriptor = getMarkerIconFromDrawable(
            ResourcesCompat.getDrawable(
                resources,
                R.drawable.bus_marker,
                null
            )!! // создаем и конвертируем Drawable к BitmapDescriptor
        )

        lifecycle.coroutineScope.launchWhenStarted {
            viewModel.startWebSocket(trackerId).collect {
                Log.d("tracker new pos", it.toString())


                val busPosition = it.toLatLng()
                if (busMarker == null)
                    busMarker = googleMap.addMarker(
                        MarkerOptions()
                            .position(busPosition)
                            .title(flight?.name)
                            .icon(busMarkerIcon)
                    )
                else
                    busMarker?.position = busPosition

            }
        }
    }
    private fun moveCamera(point: LatLng?) {
        if (point != null)
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 13.5f), 1500, null)
    }

    override fun onStop() {
        viewModel.stopWebSocket()
        super.onStop()
    }

    override fun onResume() {
        if (flight != null && flight!!.id != null) startListeningTracker(flight!!.id)
        super.onResume()
    }

    private fun getMarkerIconFromDrawable(drawable: Drawable): BitmapDescriptor {
        val canvas = Canvas()
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        canvas.setBitmap(bitmap)
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}