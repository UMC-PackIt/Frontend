package com.umc.android.packit

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.animation.doOnEnd
import androidx.recyclerview.widget.LinearLayoutManager
import com.umc.android.packit.databinding.FragmentOrderBinding


class OrderFragment() : AppCompatActivity() {

    // 변수 선언
    private lateinit var binding: FragmentOrderBinding
    private lateinit var couponBackground: CardView // 쿠폰 배경
    private var isClicked = false // 쿠폰 버튼 상태
    private var isChecked = false // 체크 버튼 상태

    private val couponList = listOf(
        OrderCoupon("[첫 주문] 5% 할인"),
        OrderCoupon("[누적 주문 10회] 5% 할인"),
        OrderCoupon("[누적 주문 10회] 15% 할인"),
        OrderCoupon("[누적 주문 10회] 20% 할인"),
        OrderCoupon("쿠폰 미사용")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = FragmentOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        init()  // 화면 초기화

        // 리사이클러뷰 연결
        val adapter = OrderCouponRVAdapter(couponList)
        binding.orderCouponRecyclerView.adapter = adapter
        binding.orderCouponRecyclerView.layoutManager= LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        // 아이템 클릭 이벤트 설정
        adapter.itemClick  = object : OrderCouponRVAdapter.ItemClick {  //클릭이벤트추가부분
            override fun onClick(position: Int) {
                // 클릭한 쿠폰으로 텍스트 변경
                binding.orderCouponSelectedBtn.text = couponList[position].name
                isClicked=false  // 클릭 버튼 상태 바꿔 주면서 쿠폰 리스트 숨기기
                hideCouponList()
            }
        }

        // 쿠폰 선택 버튼
        binding.orderCouponSelectedBtn.setOnClickListener {
            clickCouponSelection()
        }

        // 하단 체크 버튼
        binding.orderCheckOffBtnIv.setOnClickListener {
            checkButtonState()
        }
        binding.orderCheckOnBtnIv.setOnClickListener {
            checkButtonState()
        }
    }

    // 화면 초기화 함수
    private fun init() {
        // 쿠폰 리스트 숨김 상태
        couponBackground = binding.orderCouponBackgroundView
        couponBackground.visibility = View.INVISIBLE

        // 체크 버튼 해제 상태
        binding.orderCheckOffBtnIv.visibility = View.VISIBLE
        binding.orderCheckOnBtnIv.visibility = View.GONE
    }

    // 쿠폰 클릭 시, 쿠폰 리스트 띄우기
    private fun clickCouponSelection() {
        isClicked = !isClicked // 상태 전환
        if (isClicked) showCouponList() else hideCouponList()
    }

    // 쿠폰 리스트 보이기
    private fun showCouponList() {
        couponBackground.visibility = View.VISIBLE
        // 아래로 슬라이드
        val slideDown = ObjectAnimator.ofFloat(couponBackground, "translationY", 0f, 55f)
        slideDown.duration = 300
        slideDown.start()
    }

    // 쿠폰 리스트 숨기기
    private fun hideCouponList() {
        // 위로 슬라이드
        val slideUp = ObjectAnimator.ofFloat(couponBackground, "translationY", 55f, 0f)
        slideUp.duration = 300
        slideUp.start()
        slideUp.doOnEnd {
            couponBackground.visibility = View.GONE
        }
    }

    // 체크 버튼 on/off 함수
    private fun checkButtonState() {
        isChecked = !isChecked // 상태 전환

        if (isChecked) {    // 체크된 상태로 변경
            binding.orderCheckOffBtnIv.visibility = View.GONE
            binding.orderCheckOnBtnIv.visibility = View.VISIBLE
        } else {            // 체크되지 않은 상태로 변경
            binding.orderCheckOffBtnIv.visibility = View.VISIBLE
            binding.orderCheckOnBtnIv.visibility = View.GONE
        }
    }
}
