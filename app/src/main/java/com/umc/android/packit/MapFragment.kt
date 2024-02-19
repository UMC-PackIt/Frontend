

package com.umc.android.packit

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.umc.android.packit.databinding.FragmentMapBinding
import com.umc.android.packit.databinding.FragmentStoreListBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"


class MapFragment : Fragment(), OnMapReadyCallback, StoreListRVAdapter.MyItemClickListener  {

    private lateinit var binding: FragmentMapBinding
    private lateinit var autocompleteFragment:AutocompleteSupportFragment
    private lateinit var mapView: MapView //지도용 변수
    private lateinit var googleMap: GoogleMap
    private var currentMarker: Marker? = null

    // TODO: 지울 것
    // 가게 데이터 리스트
    var StoreDataFromAPI: List<StoreResponse> = ArrayList()
    var storeList : ArrayList<StoreResponse> = ArrayList()

    private lateinit var dialog: BottomSheetDialog
    private lateinit var bottomSheet: FragmentStoreListBinding
    companion object {
        fun newInstance(): MapFragment {
            return MapFragment()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Places.initialize(requireContext(),"AIzaSyAIiKuJ8RTp54prSp0lyIUw15lUVftR-6s")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMapBinding.inflate(inflater, container, false)

        //배경에 지도 띄우기
        this.mapView = binding.mapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this@MapFragment)

        //지도검색 칸 초기화
        autocompleteFragment = (childFragmentManager.findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment?)!!

        autocompleteFragment.setPlaceFields(listOf(Place.Field.ID,Place.Field.ADDRESS, Place.Field.LAT_LNG,Place.Field.NAME ))
        autocompleteFragment.setOnPlaceSelectedListener(object: PlaceSelectionListener{
            //장소 검색 시 오류 처리
            override fun onError(p0: Status) {
                Toast.makeText(requireContext(), "잠시 후 다시 검색해주세요.", Toast.LENGTH_SHORT).show()
            }

            //에러 없다면 검색된 장소 보여주기
            override fun onPlaceSelected(place: Place) {
                val latLng= place.latLng!! //위치 찾아서
                val placeName=place.name!!
                val placeAdd=place.address!!
                zoonOnMap(latLng) //지도에서 줌 인
                autocompleteFragment.setText("") //장소가 선택되면 입력창의 input은 초기화
                addMarker(latLng, placeName, placeAdd)
                //addMarker(latLng)
            }

        })

        // 가게 목로 조회 api 호출
        val apiService = ApiClient.retrofitInterface
        // CoroutineScope를 생성하고 비동기 호출을 수행
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // API 호출 결과를 LiveData에 저장
                StoreDataFromAPI = apiService.getNearbyStores()
                storeList = ArrayList(StoreDataFromAPI) ?: ArrayList()


            } catch (e: Exception) {
                // 예외 처리 로직
                storeList =  ArrayList()
                Toast.makeText(requireContext(), "API 호출에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        // LiveData는 비동기적으로 값을 가져오기 때문에,
        // null 데이터 emptyList()로 처리
        binding.storeListBtn.setOnClickListener {
            // LiveData 값을 가져와서 BottomSheet에 표시
            showBottomSheet(storeList)
        }

        return binding.root

    }

    private fun showBottomSheet(storeDatas: ArrayList<StoreResponse>) {
        val dialogView = layoutInflater.inflate(R.layout.fragment_store_list, null)
        dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(dialogView)
        bottomSheet = FragmentStoreListBinding.bind(dialogView)

        // 리사이클러 뷰 데이터 연결
        val storeListRVAdapter = StoreListRVAdapter(storeDatas)
        storeListRVAdapter.setMyItemClickListener(this)
        bottomSheet.storeListRecyclerView.adapter = storeListRVAdapter

        dialog.show()
    }

    override fun onItemClick(store: StoreResponse) {
        // 아이템 클릭 시 StoreActivity를 시작
        val intent = Intent(requireContext(), StoreActivity::class.java)
        intent.putExtra("storeImg", store.image ?: "") // storeImg가 null이 아니면 해당 값, null이면 -1을 전달
        // TODO: 북마크
        intent.putExtra("star", store.is_bookmarked ?: 0) // storeImg가 null이 아니면 해당 값, null이면 -1을 전달
        intent.putExtra("storeId", store.store_id ?: -1)

        startActivity(intent)
        dialog.dismiss()
    }

    // TODO: 즐겨찾기 on/off 1
    override fun onStarClick(store: StoreResponse) {
        if (store.is_bookmarked == 1) store.is_bookmarked = 0 else store.is_bookmarked = 1

        val userId = 1 // 사용자 ID를 여기에 설정하세요
        val apiService = ApiClient.retrofitInterface

        apiService.changeBookmarkStatus(store.store_id!!, userId).enqueue(object :
            Callback<BookmarkResponse> {
            override fun onResponse(call: Call<BookmarkResponse>, response: Response<BookmarkResponse>) {
                if (response.isSuccessful) {
                    // 북마크 상태 변경 성공
                    updateStoreStarImage(store) // RecyclerView 업데이트

                } else {
                    // 북마크 상태 변경 실패
                    Toast.makeText(requireContext(), "북마크 상태 변경 실패", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<BookmarkResponse>, t: Throwable) {
                // 네트워크 오류 등으로 인한 실패
                Toast.makeText(requireContext(), "네트워크 오류: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
//        if (store.is_bookmarked == 1) store.is_bookmarked = 0 else store.is_bookmarked = 1
//        updateStoreStarImage(store)
    }

    // TODO: 즐겨찾기 on/off 2
    private fun updateStoreStarImage(store: StoreResponse) {
        val adapter = bottomSheet.storeListRecyclerView.adapter as? StoreListRVAdapter
        adapter?.updateStoreStarImage(store)
    }


    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap

        currentMarker = setupMarker(MapFragment.LatLngEntity(37.5562, 126.9724))  // default 서울역
        currentMarker?.showInfoWindow()
    }

    //검색한 장소를 찾은 후에 지도에서 확대해서 보여주기
    private fun zoonOnMap(latLng: LatLng){
        val newLatLngZoom=CameraUpdateFactory.newLatLngZoom(latLng, 16f)
        googleMap?.animateCamera(newLatLngZoom)
    }

    private fun setupMarker(locationLatLngEntity: MapFragment.LatLngEntity): Marker? {

        val positionLatLng =
            LatLng(locationLatLngEntity.latitude!!, locationLatLngEntity.longitude!!)
        val markerOption = MarkerOptions().apply {
            position(positionLatLng)
            title("위치")
            snippet("서울역 위치")
        }

        googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL  // 지도 유형 설정
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(positionLatLng, 15f))  // 카메라 이동
        googleMap.animateCamera(CameraUpdateFactory.zoomTo(15f))  // 줌의 정도 - 1 일 경우 세계지도 수준, 숫자가 커질 수록 상세지도가 표시됨
        return googleMap.addMarker(markerOption)

    }

    // addMarker 메서드 추가
    private fun addMarker(location: LatLng, storeName: String, placeAdd: String) {
        // 기존 마커 삭제
        currentMarker?.remove()

        // 새로운 마커 추가
        currentMarker = googleMap.addMarker(
            MarkerOptions()
                .position(location)
                .title(storeName)
                .snippet(placeAdd)
        )

        // 지도 이동 및 줌 조절
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
        currentMarker?.showInfoWindow()
    }


    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        mapView.onDestroy()
        super.onDestroy()
    }



    data class LatLngEntity(
        var latitude: Double?,
        var longitude: Double?
    )

}