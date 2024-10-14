package com.omakase.omastay.controller;

import io.jsonwebtoken.lang.Collections;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.omakase.omastay.dto.HostInfoDTO;
import com.omakase.omastay.dto.ImageDTO;
import com.omakase.omastay.dto.MemberDTO;
import com.omakase.omastay.dto.PaymentDTO;
import com.omakase.omastay.dto.ReservationDTO;
import com.omakase.omastay.dto.RoomInfoDTO;
import com.omakase.omastay.dto.custom.MemberCouponDTO;
import com.omakase.omastay.dto.custom.MemberInfoDTO;
import com.omakase.omastay.dto.custom.MemberPointDTO;
import com.omakase.omastay.dto.custom.MemberReservationDTO;
import com.omakase.omastay.dto.custom.ReservationWithImage;
import com.omakase.omastay.dto.custom.ReviewMemberDTO;
import com.omakase.omastay.service.MyPageService;
import com.omakase.omastay.service.RoomInfoService;

import jakarta.servlet.http.Cookie;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.ui.Model;


@Controller
@RequestMapping("/mypage")
public class MypageController {

    @Autowired
    private MyPageService myPageService;

    @Autowired
    private RoomInfoService roomInfoService;


    @Value("${upload}")
    private String realPath;

    @RequestMapping("/member-ship")
    public String userMemberShip() {
        return "mypage/user-member-ship";
    }

    @RequestMapping("/coupon")
    public String userMypageCoupon() {
        return "mypage/user-mypage-coupon";
    }
    @RequestMapping("/point")
    public String userMypagePoint() {
        return "mypage/user-mypage-point";
    }
    @RequestMapping("/review")
    public String userMyPageReview() {
        return "mypage/user-my-page-review";
    }
    @RequestMapping("/info")
    public String userMyPageInfo() {
        return "mypage/user-mypage-info";
    }

     @PostMapping("/member-ship")
    @ResponseBody
    public ResponseEntity<MemberReservationDTO> getMemberReservationInfo(@RequestBody Map<String, String> requestData) {
        String memberId = requestData.get("userId");

        try {
            Integer memberIdInt = Integer.parseInt(memberId);
            // userId로 DB에서 사용자 정보 조회
            MemberReservationDTO memberInfo = myPageService.getMemberReservationInfo(memberIdInt);
            if (memberInfo == null) {

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); // 사용자 정보가 없을 경우 404 응답
            }
            // 사용자 정보 반환
            return ResponseEntity.ok(memberInfo);
        } catch (RuntimeException e) {
            System.out.println("사용자 정보 조회 중 오류 발생: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    @GetMapping("/member-ship")
    public String showMemberShipPage(Model model) {
        // 기존의 불필요한 필드 제거하고, 새로운 생성자에 맞게 호출
        model.addAttribute("memberInfo", new MemberReservationDTO(null, "", "", "방랑자 등급", "/image/user/normal.svg"));
        return "mypage/user-member-ship"; // Thymeleaf 템플릿 반환
    }

    @GetMapping("/info")
    public String userMyPageInfo(Model model) {
        // MemberInfoDTO로 데이터 생성
        model.addAttribute("memberInfo", new MemberInfoDTO(
                1,                    // ID
                "test@example.com",    // 이메일
                1000L,                // exp
                "John Doe",           // 이름
                "some-sub-value"      // sub
        ));
        return "mypage/user-mypage-info"; // Thymeleaf 템플릿 반환
    }
    

    @PostMapping("/info/userData")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUserData(@RequestBody Map<String, String> requestData) {
        String email = requestData.get("email");
        try {
            // 기존 회원 정보, 등급, 이미지 관련 데이터 가져오기
            Map<String, Object> responseData = myPageService.getMemberDataWithGradeAndImage(email);
            Map<String, String> emailReceiveStatusData = myPageService.getEmailReceiveStatus(email);
            responseData.put("emailReceiveStatus", emailReceiveStatusData.get("emailReceiveStatus"));
            return ResponseEntity.ok(responseData);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/member-info")
    @ResponseBody
    public ResponseEntity<MemberDTO> getMemberInfo(@RequestBody Map<String, String> requestData) {
        String memberId = requestData.get("userId");
        try {
            Integer memberIdInt = Integer.parseInt(memberId);
            MemberDTO memberInfo = myPageService.getMemberInfo(memberIdInt);
            return ResponseEntity.ok(memberInfo);
        } catch (RuntimeException e) {
            System.out.println("사용자 정보 조회 중 오류 발생: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/check-password")
    @ResponseBody
    public ResponseEntity<Boolean> checkPassword(@RequestBody Map<String, String> requestData) {
        String email = requestData.get("email");
        String inputPassword = requestData.get("password");

        try {
            boolean isMatch = myPageService.checkPassword(email, inputPassword);
            return ResponseEntity.ok(isMatch);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(false);
        }
    }

    // 회원 정보 수정 
    @PostMapping("/update-member-info")
    @ResponseBody
    public ResponseEntity<String> updateMemberInfo(@RequestBody MemberDTO memberDTO) {
        String email = memberDTO.getMemberProfile().getEmail();  // MemberDTO에서 이메일을 추출

        try {
            // 이메일을 통해 회원 정보를 수정
            myPageService.updateMemberInfoByEmail(email, memberDTO);
            return ResponseEntity.ok("수정되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("수정에 실패했습니다.");
        }
    }
    //회원 탈퇴 mem_status==False =1로 하기
    @PostMapping("/quit-member-info")
    @ResponseBody
    public ResponseEntity<String> quitMember(@RequestBody Map<String, String> requestData, HttpServletRequest request, HttpServletResponse response) {
        String email = requestData.get("email");

        try {
            // 서비스 호출하여 회원 탈퇴 처리
            myPageService.quitMember(email);
            HttpSession session = request.getSession();
            session.invalidate(); // 세션 무효화

            // 쿠키에서 accessToken 및 refreshToken 제거
            Cookie accessTokenCookie = new Cookie("accessToken", null);
            accessTokenCookie.setMaxAge(0);
            accessTokenCookie.setPath("/");
            response.addCookie(accessTokenCookie);

            Cookie refreshTokenCookie = new Cookie("refreshToken", null);
            refreshTokenCookie.setMaxAge(0);
            refreshTokenCookie.setPath("/");
            response.addCookie(refreshTokenCookie);
            return ResponseEntity.ok("회원 탈퇴가 성공적으로 처리되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("회원 탈퇴 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @PostMapping("/point")
    @ResponseBody
    public ResponseEntity<MemberPointDTO> getUserPoints(@RequestBody Map<String, Integer> requestData) {
        Integer memberId = requestData.get("memberId");

        MemberPointDTO memberPointDTO = myPageService.getMemberPoints(memberId);

        if (memberPointDTO == null) {
            memberPointDTO = new MemberPointDTO();
        }

        if (memberPointDTO.getFormattedPoints() == null) {
            memberPointDTO.setFormattedPoints(Collections.emptyList());
        }

        // JSON 형식으로 응답
        return ResponseEntity.ok(memberPointDTO);
    }


    @PostMapping("/coupon")
    public ResponseEntity<MemberCouponDTO> getUserCoupons(@RequestBody Map<String, Integer> requestData) {
        Integer memberId = requestData.get("memberId");
        // memberId가 없을 경우 잘못된 요청 처리
        if (memberId == null) {
            return ResponseEntity.badRequest().build();
        }

        // 서비스에서 쿠폰 정보를 가져옴
        MemberCouponDTO memberCouponDTO = myPageService.getCouponsForMember(memberId);
        // 쿠폰 정보를 반환
        return ResponseEntity.ok(memberCouponDTO);
    }

    @PostMapping("/register-coupon")
    public ResponseEntity<String> registerCoupon(@RequestBody Map<String, String> requestData) {
        String icCode = requestData.get("couponCode");
        Integer memberId = Integer.parseInt(requestData.get("memberId"));

        // 쿠폰 등록 서비스 호출
        String resultMessage = myPageService.registerCoupon(icCode, memberId);

        return ResponseEntity.ok(resultMessage);
    }

    @PostMapping("/review")
    @ResponseBody
    public List<ReviewMemberDTO> reviewMember(@RequestBody Map<String, Integer> requestData) {
        int memIdx = requestData.get("memIdx");
        // 서비스 호출 후 데이터 확인
        List<ReviewMemberDTO> reviewList = myPageService.reviewMember(memIdx);

        return reviewList;
    }
    
    @PostMapping("/delete-reviews")
    public Map<String, String> deleteReviews(@RequestBody Map<String, List<Integer>> requestData) {
        List<Integer> reviewIds = requestData.get("reviewIds");
        boolean success = myPageService.deleteReviews(reviewIds);
        
        Map<String, String> response = new HashMap<>();
        if (success) {
            response.put("message", "리뷰가 성공적으로 삭제되었습니다.");
        } else {
            response.put("message", "리뷰 삭제 중 오류가 발생했습니다.");
        }
        return response;
    }

    @GetMapping("/reservation")
    public ModelAndView reservation(
        @RequestParam("id") Integer id,
        @PageableDefault(size = 3, page = 0, sort = "startEndVo.end", direction = Sort.Direction.DESC) Pageable pageable) {
        System.out.println("회원번호 " + id);
        ModelAndView mv = new ModelAndView();
        boolean newTrip = false;

        Page<ReservationDTO> reservationPage = myPageService.getNewReservationInfo(id, pageable);

        int nowPage = reservationPage.getPageable().getPageNumber() + 1;
        int startPage = Math.max(nowPage - 4, 1);
        int endPage = Math.min(startPage + 4, reservationPage.getTotalPages());

        List<ReservationDTO> reservation = reservationPage.getContent();
        System.out.println("리스트사이즈" +reservation.size());
        if( reservation != null && reservation.size() > 0){
            newTrip = true;
        }
        List<ReservationWithImage> reservationsWithImages = new ArrayList<>();

        for (ReservationDTO res : reservation) {
            RoomInfoDTO room = roomInfoService.getRoomInfo(res.getRoomIdx());
            System.out.println("호텔정보" + room.getHIdx());
            ImageDTO image = roomInfoService.getImage(room.getHIdx());
            System.out.println("이미지" + image);
            String img = realPath + "host/" + image.getImgName().getFName();

            HostInfoDTO host = roomInfoService.getHostInfo(room.getHIdx());
            LocalDateTime start = res.getStartEndVo().getStart();
            LocalDateTime end = res.getStartEndVo().getEnd();

            LocalDate startDate = start.toLocalDate();
            LocalDate endDate = end.toLocalDate();
            
            // 날짜 차이 계산 (며칠 차이인지 계산)
            long totalNights = ChronoUnit.DAYS.between(startDate, endDate);

            ReservationWithImage reservationWithImage = new ReservationWithImage();
            reservationWithImage.setReservation(res);
            reservationWithImage.setImage(img);
            reservationWithImage.setHostName(host.getHname());
            reservationWithImage.setHIdx(host.getId());
            reservationWithImage.setStart(startDate);
            reservationWithImage.setEnd(endDate);
            reservationWithImage.setDate(totalNights);
            reservationsWithImages.add(reservationWithImage);
        }
        mv.addObject("reservation", reservationsWithImages);
        mv.addObject("newTrip", newTrip);
        mv.addObject("reservationPage", reservationPage);
        mv.addObject("nowPage", nowPage);
        mv.addObject("startPage", startPage);
        mv.addObject("endPage", endPage);
        mv.addObject("id", id);

        mv.setViewName("mypage/user-reservation");
        return mv;
    }

    @PostMapping("/reservation")
    @ResponseBody
    public Map<String, Object> reservationProc(
        @RequestBody MemberInfoDTO memberInfo,
        @PageableDefault(size = 3, page = 0, sort = "startEndVo.end", direction = Sort.Direction.DESC) Pageable pageable) {
        System.out.println("멤버정보" + memberInfo);
        System.out.println("페이지정보" + pageable.getPageNumber());
        System.out.println("페이지정보null?" + pageable);
        Map<String, Object> map = new HashMap<>();
        Page<ReservationDTO> reservation = myPageService.getReservationInfo(memberInfo.getId(), pageable);

        int nowPage = reservation.getPageable().getPageNumber() + 1;

        int startPage = Math.max(nowPage - 4, 1);
        int endPage = Math.min(startPage + 4, reservation.getTotalPages());

        List<ReservationWithImage> reservationsWithImages = new ArrayList<>();

        for (ReservationDTO res : reservation) {
            RoomInfoDTO room = roomInfoService.getRoomInfo(res.getRoomIdx());
            System.out.println("호텔정보" + room.getHIdx());    
            ImageDTO image = roomInfoService.getImage(room.getHIdx());
            System.out.println("이미지" + image);
            String img = realPath + "host/" + image.getImgName().getFName();

            HostInfoDTO host = roomInfoService.getHostInfo(room.getHIdx());

            ReservationWithImage reservationWithImage = new ReservationWithImage();
            reservationWithImage.setReservation(res);
            reservationWithImage.setImage(img);
            reservationWithImage.setHIdx(room.getHIdx());
            reservationWithImage.setHostName(host.getHname());
            reservationsWithImages.add(reservationWithImage);
        }
        System.out.println("넘어갈 현재페이지" +nowPage);
        map.put("reservation", reservationsWithImages);
        map.put("nowPage", nowPage);
        map.put("startPage", startPage);
        map.put("endPage", endPage);
        map.put("id", memberInfo.getId());

        return map;
    }

    @GetMapping("/reservation_check")
    public ModelAndView reservation_check() {
        ModelAndView mv = new ModelAndView();
        mv.setViewName("mypage/user-reservation-detail");
        return mv;
    }

    @PostMapping("/reservation_check")
    @ResponseBody
    public Map<String, Object> reservationCheckProc(@RequestBody ReservationDTO reservation) {
        System.out.println("하이");
        Map<String, Object> map = new HashMap<String, Object>();
        ReservationDTO res = myPageService.getReservation(reservation.getId());
        System.out.println("예약정보" + res);
        PaymentDTO payment = myPageService.getPayment(res.getPayIdx());
        System.out.println("결제정보" + payment);
        RoomInfoDTO room = roomInfoService.getRoomInfo(res.getRoomIdx());
        ImageDTO image = roomInfoService.getImage(room.getHIdx());
        String img = realPath + "host/" + image.getImgName().getFName();
        HostInfoDTO host = roomInfoService.getHostInfo(room.getHIdx());
        map.put("room", room);
        map.put("image", img);
        map.put("host", host);
        map.put("reservation", res);
        map.put("payment", payment);
        return map;
    }

    @GetMapping("/cancel_content")
    public String checkDetail() {
        return "mypage/modal/cancel_content";
    }


    @PostMapping("/cancel_content")
    @ResponseBody
    public Map<String, Object> checkDetail(@RequestParam("id") Integer id) {
        Map<String, Object> map = new HashMap<String, Object>();
        ReservationDTO res = myPageService.getReservation(id);
        PaymentDTO payment = myPageService.getPayment(res.getPayIdx());
        map.put("payment", payment);
        return map;
    }


    @GetMapping("/noCancel_content")
    public String noCancel() {
        return "reservation/modal/noCancel_content";
    }


    @PostMapping("/noCancel_content")
    @ResponseBody
    public Map<String, Object> noCancel(@RequestParam("id") Integer id) {
        System.out.println("들어와야해요" + id);
        Map<String, Object> map = new HashMap<String, Object>();
        PaymentDTO payment = myPageService.getPayment(id);
        map.put("payment", payment);
        return map;
    }


    //이거 3개는 모달창
    @RequestMapping("/room_info")
    public String roomInfo() {
        return "mypage/modal/room_info";
    }
}
