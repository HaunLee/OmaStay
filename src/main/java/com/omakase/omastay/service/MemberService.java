package com.omakase.omastay.service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.omakase.omastay.dto.MemberDTO;
import com.omakase.omastay.entity.Grade;
import com.omakase.omastay.entity.Member;
import com.omakase.omastay.entity.Reservation;
import com.omakase.omastay.entity.enumurate.BooleanStatus;
import com.omakase.omastay.entity.enumurate.Gender;
import com.omakase.omastay.entity.enumurate.Social;
import com.omakase.omastay.jwt.JwtProvider;
import com.omakase.omastay.mapper.MemberMapper;
import com.omakase.omastay.repository.GradeRepository;
import com.omakase.omastay.repository.MemberRepository;
import com.omakase.omastay.repository.ReservationRepository;
import com.omakase.omastay.vo.UserProfileVo;

import java.util.Base64;
import javax.crypto.SecretKey;
import java.util.Date;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;


@Service
public class MemberService {

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    //구글 로그인
    private final String googleclientId = "590469886146-me4vl0oobgapgs954olc073nj4d771hk.apps.googleusercontent.com" ;  //구글 클라이언트 ID
    private final String googleclientSecret = "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";    //구글 클라이언트 시크릿
    //private final String googleredirectUri= "http://localhost:9090/login/google/callback";  //구글 콜백 URL
    private final String googleredirectUri= "http://omastay.duckdns.org/login/google/callback";  //구글 콜백 URL
    //구글은 http가 안되서 https만 됩니다.......

    private final String clientId = "GJiDqqCVffs4XRqv94HT";  // 네이버 클라이언트 ID
    private final String clientSecret = "LvLkyp2ryM";  // 네이버 클라이언트 시크릿
    private final String redirectUri = "http://omastay.duckdns.org/login/naver/callback";  // 네이버 콜백 URL

    
    private final String kakaoclientId ="b37b15fa5576e0a1fcdde58b551288f2";
    private final String kakaoclientSecret ="fPofDsf9V7MwD2ue5gO7ZcWiEvR0D5fv";
    private final String kakaoredirectUri ="http://omastay.duckdns.org/login/kakao/callback";

    //private final String kakaoclientId ="56a031709958ad05dfa47275d428993b";
    //private final String kakaoclientSecret ="6x6DELwdDqGc06LpRceZWNPL6LJ73Db8";
    //private final String kakaoredirectUri ="http://localhost:9090/login/kakao/callback";
    //테스트용

    //카카오 로그인 시작
    public String getKakaoLoginUrl() throws UnsupportedEncodingException{
        String state = "random_state";  // 보안을 위해 랜덤 상태 값 생성
        String encodedRedirectUri = URLEncoder.encode(kakaoredirectUri, "UTF-8");
        String apiUrl = "https://kauth.kakao.com/oauth/authorize?response_type=code&client_id="
            + kakaoclientId + "&redirect_uri=" + encodedRedirectUri
            + "&state=" + state;

        return apiUrl;
    }
    
    public MemberDTO handleKakaoCallbackAndSaveMember(HttpServletResponse response, HttpServletRequest request, String code, String state) {
        String tokenUrl = "https://kauth.kakao.com/oauth/token?grant_type=authorization_code"
                        + "&client_id=" + kakaoclientId
                        + "&redirect_uri=" + kakaoredirectUri
                        + "&code=" + code
                        + "&client_secret=" + kakaoclientSecret;
    
        RestTemplate restTemplate = new RestTemplate();
        try {
            String tokenResponseStr = restTemplate.getForObject(tokenUrl, String.class);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(tokenResponseStr);
    
            String accessToken = jsonNode.get("access_token").asText();
            String refreshToken = jsonNode.has("refresh_token") ? jsonNode.get("refresh_token").asText() : null;
    
            // 사용자 정보 요청
            String userInfoUrl = "https://kapi.kakao.com/v2/user/me";
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Bearer " + accessToken);
            HttpEntity<String> userInfoRequest = new HttpEntity<>(headers);
    
            ResponseEntity<String> userInfoResponse = restTemplate.exchange(userInfoUrl, HttpMethod.GET, userInfoRequest, String.class);
            JsonNode userInfoNode = objectMapper.readTree(userInfoResponse.getBody());
            String email = userInfoNode.path("kakao_account").path("email").asText();
            String nickname = userInfoNode.path("properties").path("nickname").asText();
            Member existingMember = memberRepository.findByEmail(email);
            MemberDTO memberDTO;
    
            // 이미 존재하는 회원일 경우 처리
            if (existingMember != null) {
                if (existingMember.getMemberProfile().getStatus() == BooleanStatus.FALSE) {
                    throw new IllegalArgumentException("계정이 비활성화되었습니다. 고객센터에 문의하세요.");
                }
    
                // 일반 회원일 경우 토큰 삭제하고 리다이렉트 처리 (예외 던지지 않음)
                if (existingMember.getMemSocial() == Social.NONE) {
                    System.out.println("소셜 상태 확인: " + existingMember.getMemSocial());
                    
                    // 토큰 삭제 (쿠키와 세션에서 제거)
                    invalidateTokens(response, request);
    
                    // 일반 회원이면 에러 메시지 반환 후 null 반환
                    return null;
                }
    
                // 기존 소셜 회원의 토큰 갱신
                if (refreshToken == null) {
                    refreshToken = existingMember.getRefreshToken();
                }
    
                // JWT 토큰 생성
                Map<String, Object> claims = new HashMap<>();
                claims.put("email", email);
                claims.put("name", nickname);
                claims.put("id", existingMember.getId());
    
                String jwtAccessToken = jwtProvider.getAccesToken(claims);
                String jwtRefreshToken = jwtProvider.getRefreshToken(claims);
    
                // 회원 정보 업데이트
                existingMember.setAccessToken(jwtAccessToken);
                existingMember.setRefreshToken(jwtRefreshToken);
                memberRepository.save(existingMember);
    
                // DTO 생성
                memberDTO = new MemberDTO(existingMember);
    
                // 세션 및 쿠키 설정
                HttpSession session = request.getSession();
                session.setAttribute("accessToken", jwtAccessToken);
                session.setAttribute("refreshToken", jwtRefreshToken);
    
                // 쿠키로 저장
                Cookie accessTokenCookie = new Cookie("accessToken", jwtAccessToken);
                accessTokenCookie.setHttpOnly(false);
                accessTokenCookie.setPath("/");
                response.addCookie(accessTokenCookie);
    
                Cookie refreshTokenCookie = new Cookie("refreshToken", jwtRefreshToken);
                refreshTokenCookie.setHttpOnly(false);
                refreshTokenCookie.setPath("/");
                response.addCookie(refreshTokenCookie);
    
                System.out.println("카카오 로그인 성공: " + memberDTO);
            } else {
                // 신규 회원 처리
                memberDTO = new MemberDTO();
                memberDTO.getMemberProfile().setEmail(email);
                memberDTO.getMemberProfile().setPw("NONE");
                memberDTO.setMemName(nickname);
                memberDTO.setMemEmailCheck(BooleanStatus.TRUE);
                memberDTO.setMemSocial(Social.KAKAO);  // 카카오 로그인 설정
                System.out.println("카카오 소셜 회원 정보: " + memberDTO);
    
                // 세션에 저장
                request.getSession().setAttribute("socialMember", memberDTO);
            }
    
            return memberDTO;
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("카카오 로그인 처리 중 오류가 발생했습니다.");
        }
    }
    
    //카카오 로그인 종료

    //구글 로그인 시작
    public String getGoogleLoginUrl() throws UnsupportedEncodingException {
        String encodedRedirectUri = URLEncoder.encode(googleredirectUri, "UTF-8");
        String state = "random_state";  // 보안을 위해 랜덤 상태 값 생성
        String scope = URLEncoder.encode("profile email", "UTF-8");  // 사용자 정보 요청 범위
        
        String apiUrl = "https://accounts.google.com/o/oauth2/v2/auth?response_type=code&client_id="
                + googleclientId + "&redirect_uri=" + encodedRedirectUri 
                + "&scope=" + scope + "&state=" + state
                + "&access_type=offline&prompt=consent"; 
        return apiUrl;
    }

    // 구글 콜백 처리 + token을 추출
    public MemberDTO handleGoogleCallback(HttpServletResponse response, HttpServletRequest request, String code, String state) {
        String tokenUrl = "https://oauth2.googleapis.com/token?grant_type=authorization_code&client_id="
                        + googleclientId + "&client_secret=" + googleclientSecret
                        + "&code=" + code + "&redirect_uri=" + googleredirectUri;
    
        RestTemplate restTemplate = new RestTemplate();
        String responser = restTemplate.postForObject(tokenUrl, null, String.class);
    
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(responser);
            String accessTokenFromGoogle = jsonNode.get("access_token").asText();
    
            // 사용자 정보 가져오기
            String userInfoUrl = "https://people.googleapis.com/v1/people/me?personFields=phoneNumbers,emailAddresses,names,genders";
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Bearer " + accessTokenFromGoogle);
            HttpEntity<String> tokenRequest = new HttpEntity<>(headers);
            ResponseEntity<String> userInfoResponse = restTemplate.exchange(userInfoUrl, HttpMethod.GET, tokenRequest, String.class);
            JsonNode userInfoNode = objectMapper.readTree(userInfoResponse.getBody());
    
            // 사용자 정보 추출
            String email = userInfoNode.get("emailAddresses").get(0).get("value").asText();
            String name = userInfoNode.get("names").get(0).get("displayName").asText();
    
            Member existingMember = memberRepository.findByEmail(email);
            MemberDTO memberDTO;
    
            // 기존 회원이 있을 때
            if (existingMember != null) {
                if (existingMember.getMemberProfile().getStatus() == BooleanStatus.FALSE) {
                    throw new IllegalArgumentException("계정이 비활성화되었습니다. 고객센터에 문의하세요.");
                }
                if (existingMember.getMemSocial() == Social.NONE) {
                    invalidateTokens(response, request);
                    return null;  // 일반 회원임을 표시
                }
                // JWT 토큰 생성
                Map<String, Object> claims = new HashMap<>();
                claims.put("email", email);
                claims.put("name", name);
                claims.put("id", existingMember.getId());
    
                String accessToken = jwtProvider.getAccesToken(claims);
                String refreshToken = jwtProvider.getRefreshToken(claims);
    
                existingMember.setAccessToken(accessToken);
                existingMember.setRefreshToken(refreshToken);
                memberRepository.save(existingMember);
    
                memberDTO = new MemberDTO(existingMember);
                HttpSession session = request.getSession();
                session.setAttribute("accessToken", accessToken);
                session.setAttribute("refreshToken", refreshToken);
    
                Cookie accessTokenCookie = new Cookie("accessToken", accessToken);
                accessTokenCookie.setHttpOnly(false);
                accessTokenCookie.setPath("/");
                response.addCookie(accessTokenCookie);
    
                Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
                refreshTokenCookie.setHttpOnly(false);
                refreshTokenCookie.setPath("/");
                response.addCookie(refreshTokenCookie);
            } else {
                // 신규 회원 처리
                memberDTO = new MemberDTO();
                memberDTO.getMemberProfile().setEmail(email);
                memberDTO.getMemberProfile().setPw("NONE");
                memberDTO.setMemName(name);
                memberDTO.setMemSocial(Social.GOOGLE);
                memberDTO.setMemEmailCheck(BooleanStatus.TRUE);
    
                // JWT 토큰 생성
                Map<String, Object> claims = new HashMap<>();
                claims.put("email", email);
                claims.put("name", name);


                // 세션에 저장
                request.getSession().setAttribute("socialMember", memberDTO);
    
            }
    
            return memberDTO;
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("구글 로그인 처리 중 오류가 발생했습니다.");
        }
    }
    //구글 로그인 구간 종료
     
    // 네이버 로그인 구현 범위 시작
    public String getNaverLoginUrl() throws UnsupportedEncodingException {
        String encodedRedirectUri = URLEncoder.encode(redirectUri, "UTF-8");
        String state = "random_state";  // 보안을 위해 랜덤 상태 값 생성
        String apiUrl = "https://nid.naver.com/oauth2.0/authorize?response_type=code&client_id=" 
                        + clientId + "&redirect_uri=" + encodedRedirectUri + "&state=" + state;
        return apiUrl;
    }

    // 네이버 콜백 처리 + 정보 추출
    public MemberDTO handleNaverCallback(HttpServletResponse response, HttpServletRequest request, String code, String state) {
        String tokenUrl = "https://nid.naver.com/oauth2.0/token?grant_type=authorization_code&client_id="
                        + clientId + "&client_secret=" + clientSecret + "&code=" + code + "&state=" + state;
    
        RestTemplate restTemplate = new RestTemplate();
        String responseStr = restTemplate.getForObject(tokenUrl, String.class);
    
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(responseStr);
    
            // 사용자 정보 가져오기
            String userInfoUrl = "https://openapi.naver.com/v1/nid/me";
            HttpHeaders headers = new HttpHeaders();
            headers.add("Authorization", "Bearer " + jsonNode.get("access_token").asText());
            HttpEntity<String> tokenRequest = new HttpEntity<>(headers);
            ResponseEntity<String> userInfoResponse = restTemplate.exchange(userInfoUrl, HttpMethod.GET, tokenRequest, String.class);
    
            JsonNode userInfoNode = objectMapper.readTree(userInfoResponse.getBody()).get("response");
    
            // 사용자 정보 추출
            String email = userInfoNode.has("email") ? userInfoNode.get("email").asText() : null;
            String name = userInfoNode.has("name") ? userInfoNode.get("name").asText() : null;
    
            Member existingMember = memberRepository.findByEmail(email);
            MemberDTO memberDTO;
    
            if (existingMember != null) {
                if (existingMember.getMemberProfile().getStatus() == BooleanStatus.FALSE) {
                    throw new IllegalArgumentException("계정이 비활성화되었습니다. 고객센터에 문의하세요.");
                }
                if (existingMember.getMemSocial() == Social.NONE) {
                    invalidateTokens(response, request);
                    return null;  // 일반 회원임을 표시
                }
                // JWT 토큰 생성
                Map<String, Object> claims = new HashMap<>();
                claims.put("email", email);
                claims.put("name", name);
                claims.put("id", existingMember.getId());
    
                String accessToken = jwtProvider.getAccesToken(claims);
                String refreshToken = jwtProvider.getRefreshToken(claims);
    
                existingMember.setAccessToken(accessToken);
                existingMember.setRefreshToken(refreshToken);
                memberRepository.save(existingMember);
    
                memberDTO = new MemberDTO(existingMember);
                HttpSession session = request.getSession();
                session.setAttribute("accessToken", accessToken);
                session.setAttribute("refreshToken", refreshToken);
    
                Cookie accessTokenCookie = new Cookie("accessToken", accessToken);
                accessTokenCookie.setHttpOnly(false);
                accessTokenCookie.setPath("/");
                response.addCookie(accessTokenCookie);
    
                Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
                refreshTokenCookie.setHttpOnly(false);
                refreshTokenCookie.setPath("/");
                response.addCookie(refreshTokenCookie);
            } else {
                // 신규 회원 처리
                memberDTO = new MemberDTO();
                memberDTO.getMemberProfile().setEmail(email);
                memberDTO.getMemberProfile().setPw("NONE");
                memberDTO.setMemName(name);
                memberDTO.setMemSocial(Social.NAVER);
                memberDTO.setMemEmailCheck(BooleanStatus.TRUE);
    
                // JWT 토큰 생성
                Map<String, Object> claims = new HashMap<>();
                claims.put("email", email);
                claims.put("name", name);
    
                // 세션에 저장
                request.getSession().setAttribute("socialMember", memberDTO);
    
            }
    
            return memberDTO;
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("네이버 로그인 처리 중 오류가 발생했습니다.");
        }
    }

    
    @Transactional
    public void updateTokens(Member member, String accessToken, String refreshToken) {
        member.setAccessToken(accessToken);
        member.setRefreshToken(refreshToken);
        memberRepository.save(member);
    }

    public boolean isMemberRegistered(String email) {
        Member member = memberRepository.findByEmail(email);
        return member != null;
    }

    //네이버 로그인 구현 범위 끝
    public boolean checkEmailDuplicate(String email) {
        // 이메일 중복 여부 확인 (null이면 중복 아님)
        Member member = memberRepository.findByEmail(email);
        return member != null;
    }

    @Transactional
    public void registerMember(MemberDTO memberDTO, String genderString, String emailSubscription, HttpSession session) {
        // 회원 가입 시 이메일 중복 체크
        // 회원 가입 시 기본 등급을 가져옵니다.
        MemberDTO socialMember = (MemberDTO) session.getAttribute("socialMember");
        System.out.println("세션에서 불러온 소셜 타입: " + (socialMember != null ? socialMember.getMemSocial() : "세션에 저장된 값이 없습니다."));
        System.out.println("registerMember 호출됨. 소셜 타입: " + memberDTO.getMemSocial());
        System.out.println("번호입니다 "+memberDTO.getMemSocial());
        if (socialMember != null) {
            memberDTO.setMemSocial(socialMember.getMemSocial());
            System.out.println("세션에서 불러온 소셜 타입: " + socialMember.getMemSocial());
        } else {
            System.out.println("세션에 저장된 값이 없습니다.");
        }
        Grade grade = gradeRepository.findById(1)
            .orElseThrow(() -> new IllegalArgumentException("기본 등급을 찾을 수 없습니다."));

        // Gender 처리: HTML에서 선택한 값을 enum으로 변환하여 저장
        Gender gender;
        if ("male".equalsIgnoreCase(genderString)) {
            gender = Gender.MAN;
        } else if ("female".equalsIgnoreCase(genderString)) {
            gender = Gender.WOMAN;

        } else {
            throw new IllegalArgumentException("Invalid gender value: " + genderString);
        }

        // 이메일 수신 여부:참이면 TRUE,아니면 FALSE
        BooleanStatus emailCheck = "yes".equalsIgnoreCase(emailSubscription) ? BooleanStatus.TRUE : BooleanStatus.FALSE;

        String rawPassword = memberDTO.getMemberProfile().getPw();
        String encryptedPassword = passwordEncoder.encode(rawPassword); // 비밀번호 암호화
        memberDTO.getMemberProfile().setPw(encryptedPassword);  

        // Member 엔티티를 생성하여 입력 데이터 세팅
        Member member = new Member();
        
        member.setGrade(grade);  // 기본 등급
        member.setMemberProfile(memberDTO.getMemberProfile());  // 이메일, 비밀번호 등
        
        member.setMemName(memberDTO.getMemName());
        member.setMemPhone(memberDTO.getMemPhone());
        member.setMemEmailCheck(emailCheck);
        member.setMemName(memberDTO.getMemName());
        member.setMemBirth(memberDTO.getMemBirth());
        member.setMemJoinDate(LocalDateTime.now());
        // 소셜 로그인 여부 확인 및 설정
        member.setMemSocial(memberDTO.getMemSocial() != null ? memberDTO.getMemSocial() : Social.NONE);
        member.setAddressVo(memberDTO.getAddressVo());  // 주소 정보
        member.setMemGender(gender);  // 성별 저장

        member.setMemberProfile(memberDTO.getMemberProfile());
        member.getMemberProfile().setStatus(BooleanStatus.TRUE);  // 가입 상태로 설정

        // 회원 정보를 DB에 저장
        memberRepository.save(member);
    }


    //로그인 기능
    @Transactional
    public MemberDTO loginAndGenerateToken(MemberDTO memberDTO) throws IOException {
        //암호화 포함
        if (memberDTO.getMemberProfile() == null) {
            System.out.println("테스트1");
            throw new IOException("잘못된 요청입니다: 사용자 프로필 정보가 없습니다.");
        }
    // 1. 이메일 조회
    Member member = memberRepository.findByEmail(memberDTO.getMemberProfile().getEmail());
    if (member == null) {
        System.out.println("해당 이메일로 등록된 사용자가 없습니다.");
        throw new IOException("해당 이메일로 등록된 사용자가 없습니다.");
    }
    // 2. 회원 탈퇴 여부
    if (member.getMemberProfile().getStatus() == BooleanStatus.FALSE) {  // Assuming mem_status == 1 means FALSE
        throw new IOException("계정이 비활성화되었습니다. 고객센터에 문의하세요.");
    }


    // 3. 비밀번호가 누락된 경우 처리
    if (memberDTO.getMemberProfile().getPw() == null) {
        System.out.println("테스트2");
        throw new IOException("비밀번호가 누락되었습니다.");
    }

    // 4. 암호화된 비밀번호 비교 (암호화된 비밀번호를 비교하기 위해 passwordEncoder.matches() 사용)
    if (!passwordEncoder.matches(memberDTO.getMemberProfile().getPw(), member.getMemberProfile().getPw())) {
        System.out.println("테스트3");
        throw new IOException("이메일 또는 비밀번호가 잘못되었습니다.");
    }

    if (member.getMemSocial() != Social.NONE) {
        System.out.println("소셜로 가입된 사용자입니다. 일반 로그인 불가.");
        throw new IOException("소셜로 로그인을 했습니다.");  // 예외 던짐
    }

    // JWT 토큰 받기
    Map<String, Object> claims = new HashMap<>();
    claims.put("email", member.getMemberProfile().getEmail());
    claims.put("name", member.getMemName());
    claims.put("id", member.getId());
    String accessToken = jwtProvider.getAccesToken(claims);
    String refreshToken = jwtProvider.getRefreshToken(claims);
    // DB에 토큰 저장
    member.setAccessToken(accessToken);
    member.setRefreshToken(refreshToken);
    memberRepository.save(member);  // 토큰 저장
    // 응답 반환 
    MemberDTO responseDTO = new MemberDTO(member);
    responseDTO.setAccessToken(accessToken);
    responseDTO.setRefreshToken(refreshToken);
    return responseDTO;  // 토큰 포함한 사용자 정보 반환
    }

    //비밀번호 찾기
    @Transactional
    public MemberDTO findpwForMember(MemberDTO memberDTO) throws IOException {
        // 1. 이메일을 통해 회원 조회
        Member member = memberRepository.findByEmail(memberDTO.getMemberProfile().getEmail());
        if (member == null) {
            throw new IOException("해당 이메일로 등록된 사용자가 없습니다.");
        }

        return null;
    }

    @Transactional
    public boolean changePassword(String email, String newPassword) {
        System.out.println("이메일값"+email);
        // 이메일을 통해 회원 조회
        Member member = memberRepository.findByEmail(email);
        System.out.println("테스트 멤버 값"+member);
        if (member == null) {
            return false; // 이메일에 해당하는 회원이 없을 경우 false 반환
        }
        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(newPassword);
        member.getMemberProfile().setPw(encodedPassword);
        System.out.println("인코딩 password: " + encodedPassword);
        // 변경된 비밀번호 DB에 저장
        memberRepository.save(member);
        System.out.println("패스워드업데이트: " + email);
        return true; // 비밀번호 변경 성공
    }

    // 관리자 회원 정보 조회
    public List<MemberDTO> getAllMembers(){
        List<Member> members = memberRepository.findAll();
        return MemberMapper.INSTANCE.toMemberDTOList(members);
    }
    
    // 로그인한 사용자의 이메일을 기반으로 사용자 정보를 가져오는 메소드
    public MemberDTO getMemberByEmail(String email) {
        Member member = memberRepository.findByEmail(email);
        if (member == null) {
            throw new IllegalArgumentException("해당 이메일로 등록된 사용자가 없습니다.");
        }
        return new MemberDTO(member);
    }
    
    @Transactional
    public MemberDTO getMemberInfo(int id) {
        Member member = memberRepository.findMemberWithGrade(id);
        System.out.println("회원정보 조회 서비스 호출" + member);
        MemberDTO res = MemberMapper.INSTANCE.toMemberDTO(member);
        return res;
    }

    public MemberDTO getMember(Integer memId) {
        Optional<Member> mem = memberRepository.findById(memId);
        
        Member member = mem.get();

        return MemberMapper.INSTANCE.toMemberDTO(member);

    }

    public List<MemberDTO> getMemList(){
        List<Member> list = memberRepository.getMemList();

        return MemberMapper.INSTANCE.toMemberDTOList(list);
    }

    // 토큰 제거 메소드
    private void invalidateTokens(HttpServletResponse response, HttpServletRequest request) {
        
        HttpSession session = request.getSession();
        session.removeAttribute("accessToken");
        session.removeAttribute("refreshToken");
    
        // 쿠키에서 토큰 제거
        Cookie accessTokenCookie = new Cookie("accessToken", null);
        accessTokenCookie.setMaxAge(0);  // 즉시 만료
        accessTokenCookie.setPath("/");
        response.addCookie(accessTokenCookie);
    
        Cookie refreshTokenCookie = new Cookie("refreshToken", null);
        refreshTokenCookie.setMaxAge(0);
        refreshTokenCookie.setPath("/");
        response.addCookie(refreshTokenCookie);
    
        System.out.println("토큰 삭제 완료");
    }

}

