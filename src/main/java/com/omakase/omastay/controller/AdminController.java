package com.omakase.omastay.controller;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.omakase.omastay.dto.AdminMemberDTO;
import com.omakase.omastay.dto.CalculationDTO;
import com.omakase.omastay.dto.CouponDTO;
import com.omakase.omastay.dto.HostInfoDTO;
import com.omakase.omastay.dto.ImageDTO;
import com.omakase.omastay.dto.InquiryDTO;
import com.omakase.omastay.dto.MemberDTO;
import com.omakase.omastay.dto.PointDTO;
import com.omakase.omastay.dto.PriceDTO;
import com.omakase.omastay.dto.ServiceDTO;
import com.omakase.omastay.dto.custom.AdminMainCustomDTO;
import com.omakase.omastay.dto.custom.CalculationCustomDTO;
import com.omakase.omastay.dto.custom.CouponHistoryDTO;
import com.omakase.omastay.dto.custom.HostRequestInfoDTO;
import com.omakase.omastay.dto.custom.MemberCustomDTO;
import com.omakase.omastay.dto.custom.PointCustomDTO;
import com.omakase.omastay.dto.custom.SalesCustomDTO;
import com.omakase.omastay.dto.custom.Top5SalesDTO;
import com.omakase.omastay.dto.custom.RecommendationCustomDTO;
import com.omakase.omastay.entity.Calculation;
import com.omakase.omastay.entity.enumurate.CalStatus;
import com.omakase.omastay.entity.enumurate.HCate;
import com.omakase.omastay.entity.enumurate.HStatus;
import com.omakase.omastay.entity.enumurate.SCate;
import com.omakase.omastay.entity.enumurate.UserAuth;
import com.omakase.omastay.service.AdminMemberService;
import com.omakase.omastay.service.CalculationService;
import com.omakase.omastay.service.CouponService;
import com.omakase.omastay.service.FileUploadService;
import com.omakase.omastay.service.HostInfoService;
import com.omakase.omastay.service.ImageService;
import com.omakase.omastay.service.InquiryService;
import com.omakase.omastay.service.IssuedCouponService;
import com.omakase.omastay.service.MemberService;
import com.omakase.omastay.service.PointService;
import com.omakase.omastay.service.PriceService;
import com.omakase.omastay.service.SalesService;
import com.omakase.omastay.service.ServiceService;
import com.omakase.omastay.service.RecommendationService;
import com.omakase.omastay.service.ReservationService;
import com.omakase.omastay.util.FileRenameUtil;
import com.omakase.omastay.vo.FileImageNameVo;
import com.omakase.omastay.vo.StartEndVo;

import io.jsonwebtoken.io.IOException;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.ByteArrayInputStream;

@Controller
@RequestMapping("/admin")
public class AdminController {

    // 파일 업로드 경로 -> application.properties에 설정
    @Value("${upload}")
    private String upload;

    @Autowired
    ServiceService ss;

    @Autowired
    CouponService cs;

    @Autowired
    IssuedCouponService ics;

    @Autowired
    PointService ps;

    @Autowired
    PriceService prs;

    @Autowired
    MemberService ms;

    @Autowired
    InquiryService is;

    @Autowired
    HostInfoService hs;

    @Autowired
    ImageService ims;

    @Autowired 
    SalesService salesService;

    @Autowired
    CalculationService calculationService;

    @Autowired
    RecommendationService recommendationService;

    @Autowired
    ReservationService reservationService;

    @Autowired
    AdminMemberService adminMemberService;

    @Autowired
    FileUploadService fileUploadService;

    @Autowired
    private ServletContext application;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private HttpSession session;

    @Value("${upload}")
    private String storage;

    //*** 로그인 ***//
    @RequestMapping("/login")
    public ModelAndView login(@RequestParam(value="error", required=false) String error) {

        ModelAndView mv = new ModelAndView();
        if(error!=null && error.equals("sessionExpired")){
            mv.addObject("errorMessage", "로그인이 필요합니다.");
        }
        mv.setViewName("admins/admin_login");

        return mv;
    }

    @RequestMapping("/login/validate")
    public ModelAndView adminLogin(@RequestParam("id") String id, @RequestParam("pw") String pw) {
        AdminMemberDTO adminMemberDTO = adminMemberService.adminlogin(id,pw);

        ModelAndView mv = new ModelAndView();
        if (adminMemberDTO != null) {
            session.setAttribute("adminMember", adminMemberDTO); // 세션에 사용자 정보 저장
            mv.setViewName("redirect:/admin/main");
        } else {
            mv.addObject("errorMessage", "아이디 또는 비밀번호가 일치하지 않습니다.");
            mv.setViewName("admins/admin_login");
        }

        return mv;
    }

    @RequestMapping("/logout")
    public ModelAndView logout() {
        session.removeAttribute("adminMember");

        ModelAndView mv = new ModelAndView();
        mv.addObject("errorMessage", "로그아웃 되었습니다.");
        mv.setViewName("admins/admin_login");
        return mv; 
    }

    //*** 메인 시작 ***//
    @RequestMapping("/main")
    public ModelAndView main() {
        ModelAndView mv = new ModelAndView();
        mv.setViewName("admins/main");

        //판매실적 top5 
        List<Top5SalesDTO> top5List = salesService.getTop5SalesThisMonth();
        mv.addObject("top5List", top5List);

        //이번달 정산 승인 대기(요청) 수
        //이번달 정산 승인 수
        //이번달 정산 정산완료 수
        List<AdminMainCustomDTO> calCountList = calculationService.getCalculationCount();
        System.out.println("calCountList: "+ calCountList);
        //calCountList: [AdminMainCustomDTO(name=2, count=1), AdminMainCustomDTO(name=0, count=2)]
        for(AdminMainCustomDTO dto : calCountList){
            int index = Integer.parseInt(dto.getName()); // 문자열을 int로 변환
            CalStatus calStatus = CalStatus.values()[index]; // index로 enum 값 얻기

            switch(calStatus){
                case REQUEST:
                    mv.addObject("calRequestCount", dto.getCount());
                    break;
                case APPROVE:
                    mv.addObject("calApproveCount", dto.getCount());
                    break;
                case COMPLETED:
                    mv.addObject("calCompletedCount", dto.getCount());
                    break;
            }
        }

        //입점 요청 대기 수
        //입점 완료 가맹점 수
        List<AdminMainCustomDTO> reqCountList = hs.getrequestCount();
        System.out.println("reqCountList: "+reqCountList);

        for(AdminMainCustomDTO dto : reqCountList){
            if(dto.getName() != null){
                int index = Integer.parseInt(dto.getName()); // 문자열을 int로 변환
                HStatus hStatus = HStatus.values()[index]; // index로 enum 값 얻기

                switch(hStatus){
                    case APPLY:
                        mv.addObject("reqApplyCount", dto.getCount());
                        break;
                    case APPROVE:
                        mv.addObject("reqApproveCount", dto.getCount());
                        break;
                    default:
                        break;
                }
            }
        }

        //최근 신규 회원 10건 (이메일, 가입일)
        List<MemberDTO> memList = ms.getMemList();
        System.out.println(memList);
        mv.addObject("memList", memList);

        
        return mv;
    }

    //*** 입점 요청 시작 ***//
    // 관리자 - 입점 요청 조회
    @RequestMapping("/request")
    public ModelAndView request() {
        ModelAndView mv = new ModelAndView();

        List<HostInfoDTO> list = hs.getAllHostInfos();
        mv.addObject("list", list);

        mv.setViewName("admins/request");
        return mv;
    }

    // 관리자 - 입점 요청 상세 조회
    @RequestMapping("/request/detail")
    public ModelAndView request_detail(@RequestParam("id") Integer id) {
        ModelAndView mv = new ModelAndView();

        HostRequestInfoDTO host = hs.getHostRequestInfo(id);
        
        mv.addObject("host", host);
        mv.addObject("storage", storage);
        mv.setViewName("admins/request_detail");
        return mv;
    }

    // 관리자 - 입점 요청 상세 조회 모달 정보 가져오기
    @ResponseBody
    @RequestMapping("/request/roomPrice")
    public Map<String, Object> request_room(@RequestParam("roomId") String roomId) {
        Map<String, Object> map = new HashMap<>();

        PriceDTO price = prs.getPrice(Integer.parseInt(roomId));

        map.put("price", price);

        List<ImageDTO> images = ims.getImages(Integer.parseInt(roomId));

        map.put("images", images);

        return map;
    }

    // 관리자 - 입점 요청 승인
    @ResponseBody
    @RequestMapping("/request/approve")
    public Map<String, Object> request_approve(@RequestParam("hidx") String hidx) {
        Map<String, Object> map = new HashMap<>();

        hs.approveHost(Integer.parseInt(hidx));

        return map;
    }

    // 관리자 - 입점 요청 반려
    @ResponseBody
    @RequestMapping("/request/reject")
    public Map<String, Object> request_reject(@RequestParam("hidx") String hidx) {
        Map<String, Object> map = new HashMap<>();

        hs.rejectHost(Integer.parseInt(hidx));

        return map;
    }


    //*** 정산 관리 시작 ***//
    //정산 리스트 가져오기
    @RequestMapping("/calculation")
    public ModelAndView calculation(@RequestParam(value="period", required=false) String period) {

        System.out.println("period"+period);

        ModelAndView mv = new ModelAndView();

        List<CalculationCustomDTO> list = calculationService.getCalculationMonthly(period);

        mv.addObject("list", list);
        mv.setViewName("admins/calculation");

        mv.addObject("period", period);

        return mv;
    }

    //정산 상세보기
    @RequestMapping("/calculation/detail")
    public ModelAndView calculation_detail(@RequestParam("cIdx") Integer cIdx) {
        ModelAndView mv = new ModelAndView();

        CalculationCustomDTO calculation = calculationService.getCal(cIdx);
        mv.addObject("period", calculation.getCalculationDTO().getCalMonth());
        mv.addObject("hname", calculation.getHostName());
        
        List<SalesCustomDTO> list = salesService.getMonthlySalesByHost(calculation.getCalculationDTO());
        System.out.println("list: "+ list);
        
        mv.addObject("list", list);

        mv.setViewName("admins/calculation_detail");
        return mv;
    }

    //정산 승인
    @RequestMapping("/calculation/approve")
    @ResponseBody
    public Map<Object, String> calculation_approve(@RequestParam("calIdx") Integer calIdx) {
        Map<Object, String> map = new HashMap<>();

        calculationService.approveCalculation(calIdx);

        return map;
    }

    //정산 완료
    @RequestMapping("/calculation/complete")
    @ResponseBody
    public Map<Object, String> calculation_complete(@RequestParam("calIdx") Integer calIdx) {
        Map<Object, String> map = new HashMap<>();

        calculationService.completeCalculation(calIdx);

        return map;
    }

    /************************ 정산 관리 끝 ************************/

    //*** 판매 실적 ***//
    @RequestMapping("/sales")
    public ModelAndView sales(@RequestParam(value="region", required=false) String region) {
        //기본 설정: 현재 날짜 월의 1일~ 오늘
        ModelAndView mv = new ModelAndView();

        //이번달 전체 지역 매출 테이블 가져오기
        List<SalesCustomDTO> list = salesService.getAllSalesThisMonth();
        mv.addObject("list", list);
        
        //이번달 전체 지역 판매 실적 Top5
        List<Top5SalesDTO> top5List = salesService.getTop5SalesThisMonth();
        mv.addObject("top5List", top5List);

        String date = LocalDate.now().withDayOfMonth(1).toString() + " ~ "+ LocalDate.now().toString();
        mv.addObject("date", date);

        mv.setViewName("admins/sales");

        return mv;
    }

    //판매실적 검색(동기식)
    @RequestMapping("/sales/search")
    public ModelAndView sales_search(@RequestParam(value="dateRange", required=false) String dateRange,
                                        @RequestParam(value="region", required=false) String region) {
        System.out.println("dateRange"+dateRange);
        System.out.println("region"+region);

        ModelAndView mv = new ModelAndView();

        //검색어로 매출 테이블 가져오기
        List<SalesCustomDTO> list = salesService.searchSales(dateRange, region);
        System.out.println("list"+list);
        mv.addObject("list", list);

        //검색어로 판매 실적 Top5
        List<Top5SalesDTO> top5List = salesService.searchTop5Sales(dateRange, region);
        System.out.println("top5List"+top5List);
        mv.addObject("top5List", top5List);

        if(dateRange != null)
            mv.addObject("date", dateRange);
        if(region != null)
            mv.addObject("region", region);

        mv.setViewName("admins/sales");

        return mv;
    }


    //*** 가맹점 공지사항 ***//
    // 가맹점 공지사항 리스트로 이동
    @RequestMapping("/host_notice")
    public String host_notice() {
        return "admins/host_notice";
    }

    // 가맹점 공지사항 전체 리스트 가져오기
    @RequestMapping("/host_notice/getList")
    @ResponseBody
    public Map<String, Object> host_notice_list() {
        Map<String, Object> map = new HashMap<>();

        List<ServiceDTO> list = ss.getAllServices(null, UserAuth.HOST);

        map.put("data", list);

        return map;
    }

    // 호스트 공지사항 검색하기
    @RequestMapping("/host_notice/search")
    @ResponseBody
    public Map<String, Object> host_notice_search(
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "date", required = false) String date) {

        Map<String, Object> map = new HashMap<>();

        List<ServiceDTO> list = ss.searchService(type, keyword, date, UserAuth.HOST, SCate.NOTICE);

        map.put("list", list);

        return map;
    }

    // 호스트 공지사항 게시글 삭제하기
    @ResponseBody
    @RequestMapping("/notice/delete")
    public Map<String, Object> notice_delete(@RequestParam("ids") List<Integer> ids) {

        Map<String, Object> map = new HashMap<>();

        int cnt = ss.deleteService(ids);
        System.out.println("호스트 공지사항 삭제 " + cnt +"건");

        map.put("cnt", cnt);

        return map;
    }

    // 가맹점 공지사항 세부 조회
    @RequestMapping(value = "/host_notice/view", method = RequestMethod.GET)
    public ModelAndView host_notice_detail(@RequestParam("id") String id) {

        ModelAndView mv = new ModelAndView();
        mv.setViewName("admins/host_notice_view");
        
        if (id != null) {
            ServiceDTO sDto = ss.getServices(Integer.parseInt(id));
            mv.addObject("sDto", sDto);
        }

        return mv;
    }

    // 가맹점 공지사항 수정하기
    @RequestMapping(value = "/host_notice/modify", method = RequestMethod.GET)
    public ModelAndView host_notice_modify(@RequestParam("id") String id) {

        ModelAndView mv = new ModelAndView();
        mv.setViewName("admins/host_notice_modify");

        if (id != null) {
            ServiceDTO sDto = ss.getServices(Integer.parseInt(id));
            mv.addObject("sDto", sDto);
        }

        return mv;
    }

    // 가맹점 공지사항 수정하기로 저장
    @RequestMapping(value = "/host_notice/modify", method=RequestMethod.POST)
    public String host_notice_modify_save(ServiceDTO modified, @RequestParam(value="file", required = false) MultipartFile f, @RequestParam(value = "selectedFile", required = false) String selectedFile) {
        int key =0;
        ServiceDTO sDto = ss.getServices(modified.getId());
        
        // 해당 id에 대해 modified 객체의 값으로 수정한다.
        sDto.setSTitle(modified.getSTitle());
        sDto.setSContent(modified.getSContent());
        sDto.setSCate(modified.getSCate());

        if(f.getSize() > 0) { // 새 파일이 존재하는 경우
            String realPath = upload+"notice";
            String fname = f.getOriginalFilename();
            FileImageNameVo fvo = new FileImageNameVo();
            fvo.setOName(fname);
            fname = FileRenameUtil.checkSameFileName(fname, realPath);
            fvo.setFName(fname);
            try {
                String fileUrl = fileUploadService.uploadFile(f, "notice", fname);
                System.out.println("파일 업로드 URL: " + fileUrl);
                sDto.setFileName(fvo);
            } catch (Exception e) {
                e.printStackTrace();
            }
            key =0;

        } else if(selectedFile != null && selectedFile.length() > 0) { // 원래 파일이 있고 새 파일이 없는 경우
            key=1;

        } else { //selectedFile.length()<1 => 파일이 있었는데 삭제된 경우 
            sDto.setFileName(null);

            key=2;
        }

        ss.modifyServices(sDto, key); // 업데이트: sDto에는 수정된 값이 들어있음

        return "redirect:/admin/host_notice/view?id=" + sDto.getId();
    }
    
    // 가맹점 공지사항 글쓰기로 이동
    @RequestMapping(value = "/host_notice/write", method = RequestMethod.GET)
    public String host_notice_write() {

        return "admins/host_notice_write";
    }

    // 가맹점 공지사항 글쓰기로 저장
    @RequestMapping(value = "/host_notice/write", method = RequestMethod.POST)
    public ModelAndView host_notice_write_save(ServiceDTO sDto, @RequestParam("file") MultipartFile f) {
        // 폼양식에서 첨부파일이 전달될 때 enctype이 지정된다.
        String c_type = request.getContentType();
        if (c_type.startsWith("multipart")) {

            String fname = null;
            if (f != null && f.getSize() > 0) {
                //String realPath = application.getRealPath(upload);
                String realPath = upload + "notice";

                fname = f.getOriginalFilename();
                FileImageNameVo fvo = new FileImageNameVo();
                fvo.setOName(fname);
                fname = FileRenameUtil.checkSameFileName(fname, realPath);
                fvo.setFName(fname);

                try {
                    String fileUrl = fileUploadService.uploadFile(f, "notice", fname);
                    System.out.println("파일 업로드 URL: " + fileUrl);

                    sDto.setFileName(fvo);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            sDto.setSAuth(UserAuth.HOST);

            ss.saveService(sDto);
        }

        ModelAndView mv = new ModelAndView();
        mv.setViewName("redirect:/admin/host_notice");

        return mv;
    }

    // 이미지 첨부
    @RequestMapping(value = "/saveImg", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, String> saveImg(@RequestParam("s_file") MultipartFile f) throws java.io.IOException {

        // 반환객체 생성
        Map<String, String> map = new HashMap<String, String>();
        String fname = null;

        if (f.getSize() > 0) { 

            String realPath = upload + "notice";

            fname = f.getOriginalFilename();
            fname = FileRenameUtil.checkSameFileName(fname, realPath);

            try {
                System.out.println("fname"+fname);
                String fileUrl = fileUploadService.uploadFile(f, "notice", fname);
                System.out.println("파일 업로드 URL: " + fileUrl);
                map.put("url", fileUrl);
            } catch (Exception e) {
                e.printStackTrace();
                map.put("error", "File upload failed");
            }
            
        } else {
            map.put("error", "File is empty");
        }

        return map;
    }

    // 파일 다운로드
    @RequestMapping(value="/fileDownload", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<InputStreamResource> fileDownload(@RequestParam("fName") String fName)
            throws FileNotFoundException, UnsupportedEncodingException {

                // 버킷 이름과 파일 이름을 추출
        String bucketName = upload.replace("https://storage.googleapis.com/", "").replace("/", "");
        String filePath = "notice/" + fName; // 파일 경로 설정

        Storage storage = StorageOptions.getDefaultInstance().getService();
        Blob blob = storage.get(bucketName, filePath);

        if (blob == null || !blob.exists()) {
            throw new IOException("File not found");
        }

        ByteArrayInputStream bis = new ByteArrayInputStream(blob.getContent());
        InputStreamResource resource = new InputStreamResource(bis);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION,
                "attachment;filename=" + new String(fName.getBytes("UTF-8"), "ISO-8859-1"));
        headers.add(HttpHeaders.CONTENT_TYPE, "application/octet-stream;charset=8859_1");
        headers.add(HttpHeaders.CONTENT_ENCODING, "binary");

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(blob.getSize())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
        
    }

    //**** 회원 조회 ***//
    //회원 조회로 이동
    @RequestMapping("/member")
    public ModelAndView member() {
        ModelAndView mv = new ModelAndView();

        List<MemberDTO> list = ms.getAllMembers();
        mv.addObject("list", list);

        mv.setViewName("admins/member");
        
        return mv;
    }

    //회원 상세 조회
    @RequestMapping("/member/detail")
    @ResponseBody
    public Map<String, Object> member_detail(@RequestParam("memId") Integer memId) {

        //member 최근 예약 정보 가져오기
        Map<String, Object> map = reservationService.member_reservation(memId);
        
        //member 정보 가져오기
        MemberDTO member = ms.getMember(memId);
        map.put("member", member);

        return map;
    }


    //*** 회원 공지사항 시작 ***//
     // 회원 공지사항 리스트로 이동
    @RequestMapping("/user_notice")
    public String user_notice() {
        return "admins/user_notice";
    }

    // 회원 공지사항 전체 리스트 가져오기
    @RequestMapping("/user_notice/getList")
    @ResponseBody
    public Map<String, Object> user_notice_list() {
        Map<String, Object> map = new HashMap<>();

        List<ServiceDTO> list = ss.getAllServices(null, UserAuth.USER);

        map.put("data", list);

        return map;
    }

    // 회원 공지사항 검색하기
    @RequestMapping("/user_notice/search")
    @ResponseBody
    public Map<String, Object> user_notice_search(
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "date", required = false) String date,
            @RequestParam(value = "noticeType", required = false) SCate noticeType) {

        System.out.println("noticeType : " + noticeType);
        Map<String, Object> map = new HashMap<>();

        List<ServiceDTO> list = ss.searchService(type, keyword, date, UserAuth.USER, noticeType);

        map.put("list", list);

        return map;
    }

    // 가맹점 공지사항 세부 조회
    @RequestMapping(value = "/user_notice/view", method = RequestMethod.GET)
    public ModelAndView user_notice_detail(@RequestParam("id") String id) {
        ServiceDTO sDto = null;
        ModelAndView mv = new ModelAndView();
        mv.setViewName("admins/user_notice_view");
        
        if (id != null) {
            sDto = ss.getServices(Integer.parseInt(id));
            mv.addObject("sDto", sDto);
        }
        
        return mv;
    }

    // 회원 공지사항 수정하기
    @RequestMapping(value = "/user_notice/modify", method = RequestMethod.GET)
    public ModelAndView user_notice_modify(@RequestParam("id") String id) {

        ModelAndView mv = new ModelAndView();
        mv.setViewName("admins/user_notice_modify");

        if (id != null) {
            ServiceDTO sDto = ss.getServices(Integer.parseInt(id));
            mv.addObject("sDto", sDto);
        }

        return mv;
    }
 
    // 회원 공지사항 수정하기로 저장
    @RequestMapping(value = "/user_notice/modify", method=RequestMethod.POST)
    public String user_notice_modify_save(ServiceDTO modified, @RequestParam(value="file", required = false) MultipartFile f, @RequestParam(value = "selectedFile", required = false) String selectedFile) {
        
        int key =0;
        ServiceDTO sDto = ss.getServices(modified.getId());
        
        // 해당 id에 대해 modified 객체의 값으로 수정한다.
        sDto.setSTitle(modified.getSTitle());
        sDto.setSContent(modified.getSContent());
        sDto.setSCate(modified.getSCate());

        if(f.getSize() > 0) { // 새 파일이 존재하는 경우
            String realPath = upload+"notice";
            String fname = f.getOriginalFilename();
            FileImageNameVo fvo = new FileImageNameVo();
            fvo.setOName(fname);
            fname = FileRenameUtil.checkSameFileName(fname, realPath);
            fvo.setFName(fname);
            try {
                String fileUrl = fileUploadService.uploadFile(f, "notice", fname);
                System.out.println("파일 업로드 URL: " + fileUrl);
                sDto.setFileName(fvo);
            } catch (Exception e) {
                e.printStackTrace();
            }
            key =0;

        } else if(selectedFile != null && selectedFile.length() > 0) { // 원래 파일이 있고 새 파일이 없는 경우
            key=1;

        } else { //selectedFile.length()<1 => 파일이 있었는데 삭제된 경우 
            sDto.setFileName(null);

            key=2;
        }

        ss.modifyServices(sDto, key); // 업데이트: sDto에는 수정된 값이 들어있음

        return "redirect:/admin/user_notice/view?id=" + sDto.getId();
    }
    
    // 회원 공지사항 글쓰기로 이동
    @RequestMapping(value = "/user_notice/write", method = RequestMethod.GET)
    public String user_notice_write() {

        return "admins/user_notice_write";
    }

    // 회원 공지사항 글쓰기로 저장
    @RequestMapping(value = "/user_notice/write", method = RequestMethod.POST)
    public ModelAndView user_notice_write_save(ServiceDTO sDto, @RequestParam("file") MultipartFile f) {
        
        // 폼양식에서 첨부파일이 전달될 때 enctype이 지정된다.
        String c_type = request.getContentType();
        if (c_type.startsWith("multipart")) {

            String fname = null;
            if (f != null && f.getSize() > 0) {
                //String realPath = application.getRealPath(upload);
                String realPath = upload + "notice";

                fname = f.getOriginalFilename();
                FileImageNameVo fvo = new FileImageNameVo();
                fvo.setOName(fname);
                fname = FileRenameUtil.checkSameFileName(fname, realPath);
                fvo.setFName(fname);

                try {
                    String fileUrl = fileUploadService.uploadFile(f, "notice", fname);
                    System.out.println("파일 업로드 URL: " + fileUrl);
                    sDto.setFileName(fvo);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            sDto.setSAuth(UserAuth.USER);
            ss.saveService(sDto);
        }

        ModelAndView mv = new ModelAndView();
        mv.setViewName("redirect:/admin/user_notice");

        return mv;
    }

    

    //*** 쿠폰 시작 ***//
    // 쿠폰 리스트 select 후 쿠폰 관리로 이동
    @RequestMapping("/coupon")
    public ModelAndView coupon() {
        ModelAndView mv = new ModelAndView();

        List<CouponDTO> list =  cs.getAllCoupons();
        mv.addObject("list", list);
        mv.setViewName("admins/coupon");
        return mv;
    }

    // 쿠폰 발급 내역 리스트 가져오기
    @RequestMapping("/coupon/history")
    @ResponseBody
    public Map<String, Object> coupon_history(@RequestParam("id") Integer id){
        Map<String, Object> map = new HashMap<>();

        List<CouponHistoryDTO> list = ics.getIssuedCouponsById(id);

        // for(CouponHistoryDTO item : list){
        //     System.out.println("item : " + item);
        // }

        map.put("list", list);

        return map;
    }

    // 쿠폰 등록 후 쿠폰 관리로 이동
    @RequestMapping("/coupon/add")
    public String coupon_add(CouponDTO cDto, @RequestParam("date") String date,
                            @RequestParam(value = "selectGrade", required = false) String selectGrade,
                            @RequestParam(value = "code", required = false) String code,
                            @RequestParam(value = "count", required = false) Integer count)  {    
        
        // DateTimeFormatter를 사용하여 문자열을 LocalDate로 변환
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd"); // 날짜 형식에 맞게 패턴을 설정
        LocalDate localDate = LocalDate.parse(date, formatter);

        // LocalDateTime으로 변환하고 시간 부분을 23:59:59로 설정
        LocalDateTime endDate = localDate.atTime(LocalTime.MAX);

        StartEndVo tempDate = new StartEndVo();
        tempDate.setStart(LocalDateTime.now());
        tempDate.setEnd(endDate);
                                
        cDto.setCpStartEnd(tempDate);

        // System.out.println("cDto : " + cDto);
        // System.out.println("cDto : " + cDto.getCpStartEnd().getEnd());
        // System.out.println("selectGrade : " + selectGrade);
        // System.out.println("code : " + code);
        // System.out.println("count : " + count);
        // System.out.println("date : " + date);

        int cnt;
        switch(cDto.getCpMethod()){
            case DESIGNATED:
                cnt = cs.issueDesignatedCoupon(cDto, selectGrade);
                break;
            case SINGLE_USE:
                cnt = cs.issueSingleUseCoupon(cDto, count);
                break;
            case MULTI_USE:
                cnt = cs.issueMultiUseCoupon(cDto, code, count);
                break;

            default:
                throw new IllegalArgumentException("Unknown coupon method: " + cDto.getCpMethod());
        }
        if(cnt < 1){
            System.out.println("쿠폰 발급 실패");
            return "error";
        }
        return "redirect:/admin/coupon";
    }

    @RequestMapping("/coupon_history")
    public String coupon_history() {
        return "admins/modals/coupon_history";
    }


    //***  포인트 시작 ***//
    //관리자 포인트 내역 리스트 가져오기
    @RequestMapping("/point")
    public ModelAndView point(@RequestParam(value = "message", required = false) String message) {

        ModelAndView mv = new ModelAndView();

        List<PointCustomDTO> list = ps.getAllPoints();

        mv.addObject("list", list);
        mv.setViewName("admins/point");

        if(message!=null){
            if(message.equals("email")){
                mv.addObject("message", "이메일이 잘못 됐습니다.");
            }else if(message.equals("success")){
                mv.addObject("message", "포인트 등록에 성공했습니다.");
            }
        }

        return mv;
    }

    //관리자 포인트 추가
    @RequestMapping("/point/add")
    public String add_point(@RequestParam("email") String email, PointDTO pDto) {

        int cnt = ps.addPoint(email, pDto);

        if(cnt < 1){
            System.out.println("포인트 추가 실패");
            return "redirect:/admin/point?message=email";
        }

        return "redirect:/admin/point?message=success";
    }

    
    //*** 추천숙소 시작 ***//
    @RequestMapping("/recommendation")
    public ModelAndView recommend() {
        ModelAndView mv = new ModelAndView();

        List<RecommendationCustomDTO> totalList = recommendationService.findTotal();
        System.out.println("totalList: "+totalList);
        mv.addObject("totalList", totalList);

        if(!totalList.isEmpty())
            mv.addObject("date", totalList.get(0).getRecommendationDTO().getRecDate());

        List<RecommendationCustomDTO> hotelList = recommendationService.getRecommendationByHCate(HCate.HOTEL_RESORT);
        mv.addObject("hotelList", hotelList);

        List<RecommendationCustomDTO> motelList = recommendationService.getRecommendationByHCate(HCate.MOTEL);
        mv.addObject("motelList", motelList);

        List<RecommendationCustomDTO> poolList = recommendationService.getRecommendationByHCate(HCate.POOL_VILLA);
        mv.addObject("poolList", poolList);

        List<RecommendationCustomDTO> guestList = recommendationService.getRecommendationByHCate(HCate.GUESTHOUSE_HANOK);
        mv.addObject("guestList", guestList);

        mv.setViewName("admins/recommendation");

        return mv;
    }

}
