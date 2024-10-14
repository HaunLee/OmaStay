package com.omakase.omastay.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.omakase.omastay.entity.Grade;
import com.omakase.omastay.entity.enumurate.BooleanStatus;
import com.omakase.omastay.entity.enumurate.Social;
import com.omakase.omastay.mapper.MemberMapper;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.io.UnsupportedEncodingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.omakase.omastay.dto.MemberDTO;
import com.omakase.omastay.dto.custom.MemberInfoDTO;
import com.omakase.omastay.service.EmailService;
import com.omakase.omastay.service.GradeService;
import com.omakase.omastay.service.MemberService;
import com.omakase.omastay.session.UserSession;
import com.omakase.omastay.vo.AddressVo;
import com.omakase.omastay.vo.UserProfileVo;


@Controller
@RequestMapping("/login")
public class LoginController {

    @Autowired
    private GradeService gradeService;

    @Autowired
    private UserSession userSession;  // 세션 관리

    @Autowired
    private EmailService emailService;

    @Autowired
    private MemberService memberService;

    @Autowired
    private HttpSession httpSession;

    @GetMapping()
    public String login() {
        return "login/login";
    }

    @RequestMapping("/user")
    public String signup() {
        return "login/user_login";
    }

    @RequestMapping("/user/register")
    public String register() {
        return "login/user_register";
    }

    @RequestMapping("/admin")
    public String admin() {
        return "login/admin_login";
    }

    @RequestMapping("/userfindpw")
    public String findpw() {
        return "login/userfindpw";
    }

    @RequestMapping("/userchangepw")
    public String userchangepw() {
        return "login/userchangepw";
    }

    //html반환용(토큰 전달 원활 목적)
    @GetMapping("/main")
    public String getUserInfo(Model model, HttpSession session) {
        String email = (String) session.getAttribute("mem_email");
        if (email == null) {
            throw new IllegalArgumentException("로그인되지 않았습니다.");
        }
    
        MemberDTO member = memberService.getMemberByEmail(email);
    
        // 사용자 정보를 모델에 추가
        model.addAttribute("email", member.getMemberProfile().getEmail());
        model.addAttribute("access_token", member.getAccessToken());
        model.addAttribute("refresh_token", member.getRefreshToken());
        model.addAttribute("phone", member.getMemPhone());
        return "main";  // main.html 파일로 이동
    }
    
    @GetMapping("/social/kakao")
    public void kakaoLogin(HttpServletResponse response) throws IOException {
        String kakaoLoginUrl = memberService.getKakaoLoginUrl();  // 카카오 로그인 URL을 받아옴
        response.sendRedirect(kakaoLoginUrl);  // 카카오 로그인 페이지로 리다이렉트
    }
    
    @GetMapping("/social/google")
    public void googleLogin(HttpServletResponse response) throws IOException {
        String googleLoginUrl = memberService.getGoogleLoginUrl();
        response.sendRedirect(googleLoginUrl);  // 구글 로그인 페이지로 리다이렉트
    }

    @GetMapping("/social/naver")
    public void naverLogin(HttpServletResponse response) throws IOException {
        String naverLoginUrl = memberService.getNaverLoginUrl();  // 서비스에서 네이버 로그인 URL을 받아옴
        response.sendRedirect(naverLoginUrl);  // 네이버 로그인 페이지로 리다이렉트
    }

        // 카카오 로그인 응답
        @GetMapping("/kakao/callback")
        public String kakaoCallback(HttpServletRequest request, HttpServletResponse response,
                                    @RequestParam(value = "code", required = false) String code,
                                    @RequestParam(value = "state", required = false) String state,
                                    @RequestParam(value = "error", required = false) String error, Model model
                                    ) {
            try {
                if (error != null || code == null) {
                    model.addAttribute("errorMessage", "카카오 로그인 처리 중 오류가 발생했습니다.");
                    return "redirect:/";  // 메인 페이지로 리다이렉트
                }

                // 카카오 로그인 처리 후 회원 정보를 세션에 저장
                MemberDTO memberDTO = memberService.handleKakaoCallbackAndSaveMember(response, request, code, state);

                // 소셜 회원 정보 로그로 출력
                System.out.println("카카오 소셜 회원 정보: " + memberDTO);

                    // 이미 일반 회원으로 가입된 이메일 처리
                    if (memberDTO == null) {
                        System.out.println("일반 회원으로 가입된 이메일입니다. 로그인 대기 페이지로 리다이렉트합니다.");
                        String encodedErrorMessage = URLEncoder.encode("이미 일반 회원으로 가입된 이메일입니다.", StandardCharsets.UTF_8);
    
                        return "redirect:/login/wait?errorMessage=" + encodedErrorMessage;
                    }

                // 이미 소셜 회원으로 등록된 경우 처리
                if (memberDTO != null && memberService.isMemberRegistered(memberDTO.getMemberProfile().getEmail())) {
                    return "redirect:/";  // 메인 페이지로 리다이렉트
                }

                // 신규 회원이거나 소셜 회원인 경우 회원가입 페이지로 리다이렉트
                request.getSession().setAttribute("socialMember", memberDTO);
                return "redirect:/login/user/register";  // 회원가입 페이지로 리다이렉트

            } catch (IllegalArgumentException e) {
                model.addAttribute("errorMessage", e.getMessage());
                return "login/error";  // 에러 처리 페이지로 리다이렉트
            }
        }


    // 구글 로그인 응답
    @GetMapping("/google/callback")
    public String googleCallback(HttpServletRequest request, HttpServletResponse response,
                                @RequestParam(value = "code", required = false) String code,
                                @RequestParam(value = "state", required = false) String state,
                                @RequestParam(value = "error", required = false) String error, Model model) {
        try {
            if (error != null || code == null) {    // 사용자가 소셜 로그인 취소를 할 경우
                model.addAttribute("errorMessage", "구글 로그인 처리 중 오류가 발생했습니다.");
                return "redirect:/";  // 메인 페이지로 리다이렉트
            }

            // 구글 로그인 처리 후 회원 정보를 세션에 저장
            MemberDTO memberDTO = memberService.handleGoogleCallback(response, request, code, state);

            // 소셜 회원 정보 로그로 출력
            System.out.println("구글 소셜 회원 정보: " + memberDTO);

            // 이미 일반 회원으로 가입된 이메일 처리
            if (memberDTO == null) {
                String encodedErrorMessage = URLEncoder.encode("이미 일반 회원으로 가입된 이메일입니다.", StandardCharsets.UTF_8);
                return "redirect:/login/wait?errorMessage=" + encodedErrorMessage;
            }

            // 이미 소셜 회원으로 등록된 경우 처리
            if (memberDTO != null && memberService.isMemberRegistered(memberDTO.getMemberProfile().getEmail())) {
                return "redirect:/";  // 메인 페이지로 리다이렉트
            }

            // 신규 회원이거나 소셜 회원인 경우 회원가입 페이지로 리다이렉트
            request.getSession().setAttribute("socialMember", memberDTO);
            return "redirect:/login/user/register";  // 회원가입 페이지로 리다이렉트

        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "login/error";  // 에러 처리 페이지로 리다이렉트
        }
    }

    // 네이버 로그인 응답
    @GetMapping("/naver/callback")
    public String naverCallback(HttpServletRequest request, HttpServletResponse response,
                                @RequestParam(value = "code", required = false) String code,
                                @RequestParam("state") String state,
                                @RequestParam(value = "error", required = false) String error, Model model) {
        try {
            if (error != null || code == null) {  // 사용자가 소셜 로그인 취소를 할 경우
                model.addAttribute("errorMessage", "네이버 로그인 처리 중 오류가 발생했습니다.");
                return "redirect:/";  // 메인 페이지로 리다이렉트
            }
            
            // 네이버 로그인 처리 후 회원 정보를 세션에 저장
            MemberDTO memberDTO = memberService.handleNaverCallback(response, request, code, state);
    
            // 소셜 회원 정보 로그로 출력
            System.out.println("소셜 회원 정보: " + memberDTO);
    
            // **일반 회원 가입된 네이버 이메일 사용 시 처리**
            if (memberDTO == null) {
                String encodedErrorMessage = URLEncoder.encode("이미 일반 회원으로 가입된 이메일입니다.", StandardCharsets.UTF_8);
                return "redirect:/login/wait?errorMessage=" + encodedErrorMessage;
            }

            if (error != null || code == null) {  // 사용자가 소셜 로그인 취소를 할 경우
                String encodedErrorMessage = URLEncoder.encode("네이버 로그인 처리 중 오류가 발생했습니다.", StandardCharsets.UTF_8);
                return "redirect:/login/wait?errorMessage=" + encodedErrorMessage;
            }
    
            if (memberDTO != null && memberService.isMemberRegistered(memberDTO.getMemberProfile().getEmail())) {
                // 이미 소셜 회원으로 등록된 경우 메인 페이지로 리다이렉트
                return "redirect:/";
            }
    
            // 신규 회원이거나 소셜 회원인 경우 회원가입 페이지로 리다이렉트
            request.getSession().setAttribute("socialMember", memberDTO);
            return "redirect:/login/user/register";  // 회원가입 페이지로 리다이렉트

        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMessage", e.getMessage());  // 에러 메시지를 모델에 담음
            return "login/error";  // 에러 처리 페이지로 리다이렉트
        }
    }
    
    //비밀번호 변경
    @PostMapping("/changePassword")
    public ResponseEntity<Map<String, Boolean>> changePassword(HttpSession session, @RequestBody Map<String, String> requestData) {
        String email = (String) session.getAttribute("email");  // 세션에서 이메일 가져오기
        String newPassword = requestData.get("password");
        System.out.println("세션에 저장된 이메일: " + session.getAttribute("email"));
        Map<String, Boolean> response = new HashMap<>();
        
        if (email == null || email.isEmpty()) {
            System.out.println("세션에 저장된 이메일이 없습니다.");
            response.put("success", false);
            return ResponseEntity.ok(response);
        }
    
        System.out.println("비밀번호 변경할 이메일: " + email);
    
        // 비밀번호 변경 처리 로직
        boolean isPasswordChanged = memberService.changePassword(email, newPassword);
    
        // 응답 처리
        response.put("success", isPasswordChanged);
    
        System.out.println("Response: " + response);  // 응답을 로그로 출력
        return ResponseEntity.ok(response);
    }
    

    // userchangepw에서 이메일을 세션에 저장

    @PostMapping("/userchangepw")
    public String changePasswordPost(@RequestBody Map<String, String> requestData, HttpSession session) {
        String email = requestData.get("email");
        System.out.println("테슽"+requestData);
        System.out.println("테스트 ㅇㅁ"+email);
        if (email != null && !email.isEmpty()) {
            session.setAttribute("email", email); // 세션에 이메일 저장
            System.out.println("세션에 저장된 이메일: " + email);
        } else {
            System.out.println("이메일이 null이거나 비어 있습니다.");
        }

        return "redirect:/login/userchangepw";  // 비밀번호 변경 페이지로 리다이렉트
    }


    @PostMapping("/checkEmail")
    public ResponseEntity<Map<String, Object>> checkSessionEmail(
    @RequestBody Map<String, String> requestData) {
    
    // 1. 요청 데이터에서 이메일 가져오기
    String email = requestData.get("email");

    // 2. 이메일 유효성 확인 (email이 비어있거나 null이면 오류 처리)
    if (email == null || email.isEmpty()) {
        Map<String, Object> response = new HashMap<>();
        response.put("exists", false);
        response.put("message", "이메일을 찾을 수 없습니다.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // 3. 이메일이 존재할 경우, DB에서 중복 여부 확인
    boolean exists = memberService.checkEmailDuplicate(email);

    // 4. 응답 처리
    Map<String, Object> response = new HashMap<>();
    response.put("exists", exists); // 중복이면 true, 중복이 아니면 false
    response.put("email", email);

    return ResponseEntity.ok(response);
}


    @PostMapping("/sendVerificationCode")
    public ResponseEntity<Map<String, Object>> sendVerificationCode(@RequestBody Map<String, String> requestData) {
        String email = requestData.get("email");
        Map<String, Object> response = new HashMap<>();
        try {
            // 이메일로 인증 코드 전송
            emailService.sendEmail(email);
            response.put("message", "인증 코드가 이메일로 전송되었습니다.");
            response.put("success", true); // 프론트엔드에서 success를 체크할 수 있도록 추가
        } catch (MessagingException | IOException e) {
            response.put("message", "인증 코드 전송 실패: " + e.getMessage());
            response.put("success", false);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
        return ResponseEntity.ok(response);
    }
    @GetMapping("/wait")
    public String showWaitPage(Model model) {
        System.out.println("에러 Message in Model: " + model.getAttribute("errorMessage"));
        return "login/wait";  // wait.html 파일이 resources/templates/login/에 있어야 함
    }


    // **이메일 인증 코드 확인**
    @PostMapping("/verifyCode")
    public ResponseEntity<Map<String, Boolean>> verifyCode(@RequestBody Map<String, String> requestData) {
        String email = requestData.get("email");
        String code = requestData.get("code");

        boolean isVerified = emailService.verifyEmailCode(email, code);
        Map<String, Boolean> response = new HashMap<>();
        response.put("isVerified", isVerified);
        return ResponseEntity.ok(response);
    }

    // user_register.html 로 이동할 때 데이터 전달
    @GetMapping("user/register")
    public String registerMemberSocial(Model model, HttpSession session) {
        MemberDTO socialMember = (MemberDTO) session.getAttribute("socialMember");
        
        if (socialMember == null) {
            // socialMember가 null일 경우 NONE으로 설정
            model.addAttribute("memSocial", "NONE");
            System.out.println("소셜 회원 정보가 세션에 없습니다.");
        } else {
            System.out.println("소셜 회원 정보 확인: " + socialMember);

            model.addAttribute("memEmail", socialMember.getMemberProfile().getEmail());
            model.addAttribute("memName", socialMember.getMemName());
            model.addAttribute("memPhone", socialMember.getMemPhone());

            // 생년월일 처리
            String birth = socialMember.getMemBirth();
            if (birth != null) {
                String[] birthParts = birth.split("-");
                model.addAttribute("birthYear", birthParts[0]);
                model.addAttribute("birthMonth", birthParts.length > 1 ? birthParts[1] : "");
                model.addAttribute("birthDay", birthParts.length > 2 ? birthParts[2] : "");
            }

            // memSocial을 안전하게 설정 (Social -> String으로 변환)
            String memSocialValue = (socialMember.getMemSocial() != null) ? socialMember.getMemSocial().name() : "NONE";
            model.addAttribute("memSocial", memSocialValue);
            System.out.println("소셜 여부 테스트: " + memSocialValue);

            if (socialMember.getMemGender() != null) {
                model.addAttribute("memGender", socialMember.getMemGender().name());
            } else {
                model.addAttribute("memGender", "MAN"); // 기본값 설정
            }

            model.addAttribute("memEmailCheck", socialMember.getMemEmailCheck().toString());
        }

        return "login/user_register";
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String,Object>> registerMember(@RequestBody Map<String, String> requestData, HttpSession session) {
        String name= requestData.get("memName");
        System.out.println("Name받기용: " + name);
        String email = requestData.get("memEmail");  // memEmail으로 수정
        String password = requestData.get("memPw"); 
        String year = requestData.get("memYear");
        String month = requestData.get("memMonth");
        String day = requestData.get("memDay");
        String birth = year + "-" + month + "-" + day;
        String gender = requestData.get("gender");
        String emailSubscription = requestData.get("emailSubscription");
        String phone = requestData.get("phone");
        String postCode = requestData.get("postCode");
        String street = requestData.get("street");
        String detail = requestData.get("detail");

        // MemberDTO 생성 및 서비스 호출
        MemberDTO memberDTO = new MemberDTO();

        // **memberProfile null 체크 후 설정**
        if (memberDTO.getMemberProfile() == null) {
            memberDTO.setMemberProfile(new UserProfileVo());
        }
        memberDTO.setMemName(name);
        memberDTO.getMemberProfile().setEmail(email);
        memberDTO.getMemberProfile().setPw(password);
        memberDTO.setMemPhone(phone);
        memberDTO.setMemBirth(birth);

        // **addressVo null 체크 후 설정**
        if (memberDTO.getAddressVo() == null) {
            memberDTO.setAddressVo(new AddressVo());
        }

        // 주소 정보 설정
        memberDTO.getAddressVo().setPostCode(postCode);
        memberDTO.getAddressVo().setStreet(street);
        memberDTO.getAddressVo().setDetail(detail);

        // 서비스 호출하여 데이터 저장
        memberService.registerMember(memberDTO, gender, emailSubscription, session);

       // 성공 응답을 JSON 형식으로 반환
       Map<String, Object> response = new HashMap<>();
       response.put("message", "회원가입 성공");
        System.out.println("년도 "+year);
        System.out.println("월 "+month);
        System.out.println("일 "+day);
       return ResponseEntity.ok(response); // JSON 응답
    }


    //유저 로그인 구역
    @PostMapping("/user")
    public String login(MemberDTO memberDTO, HttpServletResponse response, HttpServletRequest request, Model model) {
        try {
            // 이메일과 비밀번호로 로그인 및 토큰 생성
            MemberDTO responseDTO = memberService.loginAndGenerateToken(memberDTO);
    
            // 세션에 토큰 저장
            httpSession.setAttribute("accessToken", responseDTO.getAccessToken());
            Cookie accessTokenCookie = new Cookie("accessToken", responseDTO.getAccessToken());
            accessTokenCookie.setHttpOnly(false);
            accessTokenCookie.setPath("/");
            response.addCookie(accessTokenCookie);
    
            httpSession.setAttribute("refreshToken", responseDTO.getRefreshToken());
            Cookie refreshTokenCookie = new Cookie("refreshToken", responseDTO.getRefreshToken());
            refreshTokenCookie.setHttpOnly(false);
            refreshTokenCookie.setPath("/");
            response.addCookie(refreshTokenCookie);
    
            // 로그인 성공 시 메인 페이지로 리다이렉트
            return "redirect:/";
    
        } catch (IOException e) {
            // 예외 발생 시 에러 메시지를 모델에 추가하여 뷰에 전달
            model.addAttribute("errorMessage", e.getMessage());  // 예외 메시지를 전달
            return "login/user_login";  // 로그인 페이지로 다시 이동
        }
    }
    

    //로그아웃시 토큰 삭제

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response, MemberDTO memberDTO) {
        // 세션 무효화
        try{
        MemberDTO responseDTO = memberService.loginAndGenerateToken(memberDTO);
        
        if(responseDTO !=null){
            httpSession.setAttribute("login", false );
        }

    }
    catch(Exception e){
        e.printStackTrace();
    }
        request.getSession().invalidate();

        // 토큰 쿠키 삭제
        Cookie accessTokenCookie = new Cookie("accessToken", null);
        accessTokenCookie.setMaxAge(0);  // 쿠키 만료
        accessTokenCookie.setPath("/");
        response.addCookie(accessTokenCookie);

        Cookie refreshTokenCookie = new Cookie("refreshToken", null);
        refreshTokenCookie.setMaxAge(0);
        refreshTokenCookie.setPath("/");
        response.addCookie(refreshTokenCookie);

        // 로그아웃 후 메인 페이지로 리다이렉트
        return "redirect:/";  // 메인 페이지로 리다이렉트
    }

        @RequestMapping("/userInfo")
        @ResponseBody
        public Map<String, Object> requestMethodName(MemberInfoDTO member) {
            System.out.println(member);
            Map<String, Object> map = new HashMap<>();
            MemberDTO memberDTO = memberService.getMemberInfo(member.getId());
            System.out.println("회원등급" + memberDTO);
            Grade grade = gradeService.getGrade(memberDTO.getGIdx());
            System.out.println("회원등급" + grade);
            map.put("grade", grade);
            return map;
        }
}

