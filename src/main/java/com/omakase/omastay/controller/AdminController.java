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

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.omakase.omastay.dto.CouponDTO;
import com.omakase.omastay.dto.HostInfoDTO;
import com.omakase.omastay.dto.ImageDTO;
import com.omakase.omastay.dto.InquiryDTO;
import com.omakase.omastay.dto.IssuedCouponDTO;
import com.omakase.omastay.dto.MemberDTO;
import com.omakase.omastay.dto.PointDTO;
import com.omakase.omastay.dto.PriceDTO;
import com.omakase.omastay.dto.ServiceDTO;
import com.omakase.omastay.dto.custom.CouponHistoryDTO;
import com.omakase.omastay.dto.custom.HostRequestInfoDTO;
import com.omakase.omastay.entity.Image;
import com.omakase.omastay.entity.Point;
import com.omakase.omastay.entity.enumurate.SCate;
import com.omakase.omastay.entity.enumurate.UserAuth;
import com.omakase.omastay.service.CouponService;
import com.omakase.omastay.service.HostInfoService;
import com.omakase.omastay.service.ImageService;
import com.omakase.omastay.service.InquiryService;
import com.omakase.omastay.service.IssuedCouponService;
import com.omakase.omastay.service.MemberService;
import com.omakase.omastay.service.PointService;
import com.omakase.omastay.service.PriceService;
import com.omakase.omastay.service.ServiceService;
import com.omakase.omastay.util.FileRenameUtil;
import com.omakase.omastay.vo.FileImageNameVo;
import com.omakase.omastay.vo.StartEndVo;

import io.jsonwebtoken.io.IOException;
import io.lettuce.core.dynamic.annotation.Param;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/admin")
public class AdminController {

    // 파일 업로드 경로 -> application.properties에 설정
    // @Value("${upload}")
    private String upload = "/upload/admin";

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
    private ServletContext application;

    @Autowired
    private HttpServletRequest request;

    @Value("${upload}")
    private String storage;

    @RequestMapping("/login")
    public String login() {
        return "admins/login";
    }

    @RequestMapping("/main")
    public String main() {
        return "admins/main";
    }

    /************************ 입점 요청 시작 ************************/
    @RequestMapping("/request")
    public ModelAndView request() {
        ModelAndView mv = new ModelAndView();

        List<HostInfoDTO> list = hs.getAllHostInfos();

        mv.addObject("list", list);
        mv.setViewName("admins/request");
        return mv;
    }

    @RequestMapping("/request/detail")
    public ModelAndView request_detail(@RequestParam("id") String id) {
        ModelAndView mv = new ModelAndView();

        HostRequestInfoDTO host = hs.getHostRequestInfo(Integer.parseInt(id));
        System.out.println(host);
        
        mv.addObject("host", host);
        mv.addObject("storage", storage);
        mv.setViewName("admins/request_detail");
        return mv;
    }

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

    /************************ 입점 요청 끝 ************************/
    @RequestMapping("/payment")
    public String payment() {
        return "admins/payment";
    }

    @RequestMapping("/payment_detail")
    public String payment_detail() {
        return "admins/payment_detail";
    }

    @RequestMapping("/sales")
    public String sales() {
        return "admins/sales";
    }

    /****************************** 가맹점 공지사항 ******************************/
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

        List<ServiceDTO> list = ss.getAllServices(SCate.NOTICE, UserAuth.HOST);

        map.put("data", list);

        return map;
    }

    // 가맹점 공지사항 검색하기
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

    // 게시물 삭제하기
    @ResponseBody
    @RequestMapping("/notice/delete")
    public Map<String, Object> notice_delete(@RequestParam("ids") List<Integer> ids) {

        Map<String, Object> map = new HashMap<>();

        int cnt = ss.deleteService(ids);
        System.out.println("삭제 완료 개수 : " + cnt);

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
    public String host_notice_modify_save(ServiceDTO modified, @RequestParam("file") MultipartFile f, @RequestParam("selectedFile") String selectedFile) {
    
        ServiceDTO sDto = ss.getServices(modified.getId());
        
        // 해당 id에 대해 modified 객체의 값으로 수정한다.
        sDto.setSTitle(modified.getSTitle());
        sDto.setSContent(modified.getSContent());

        // 파일이 수정되었을 경우
        if(f.getSize() > 0) {
            String realPath = application.getRealPath(upload);
            String fname = f.getOriginalFilename();
            FileImageNameVo fvo = new FileImageNameVo();
            fvo.setOName(fname);
            fname = FileRenameUtil.checkSameFileName(fname, realPath);
            fvo.setFName(fname);
            try {
                File uploadDir = new File(realPath);
                if (!uploadDir.exists()) {
                    uploadDir.mkdirs();
                }
                File dest = new File(uploadDir, fname);
                f.transferTo(dest);
                sDto.setFileName(fvo);

                //ss에서 업데이트 하기
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if(selectedFile.length()<1 && sDto.getFileName() != null) { //파일이 있었는데 삭제된 경우 -> fileName을 null로 바꾸고 업데이트
            //파일이 있었는데 삭제된 경우 -> fileName을 null로 바꾸고 업데이트
            sDto.setFileName(null);

        } else {
            // 그냥 title이랑 content만 업데이트하는 경우
            // 1) 파일이 원래 없었고 file name도 nulll일때
            // 2) 파일이 계속 유지되는 경우 -> f == null fileName은 있는 경우
        }

        ss.modifyServices(sDto); // 업데이트: sDto에는 수정된 값이 들어있음

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
                String realPath = application.getRealPath(upload);

                fname = f.getOriginalFilename();
                FileImageNameVo fvo = new FileImageNameVo();
                fvo.setOName(fname);
                fname = FileRenameUtil.checkSameFileName(fname, realPath);
                fvo.setFName(fname);

                try {
                    File uploadDir = new File(realPath);
                    if (!uploadDir.exists()) {
                        uploadDir.mkdirs();
                    }

                    // 파일 업로드(upload폴더에 저장)
                    File dest = new File(uploadDir, fname);
                    f.transferTo(dest);

                    sDto.setFileName(fvo);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            sDto.setSCate(SCate.NOTICE);
            sDto.setSAuth(UserAuth.HOST);

            ss.saveService(sDto);
        }

        ModelAndView mv = new ModelAndView();
        mv.setViewName("redirect:/admin/host_notice");

        return mv;
    }
    /***************************** 가맹점 공지사항 *****************************/

    // 이미지 첨부
    @RequestMapping(value = "/saveImg", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, String> saveImg(@RequestParam("s_file") MultipartFile f) throws java.io.IOException {

        // 반환객체 생성
        Map<String, String> map = new HashMap<String, String>();
        String fname = null;

        if (f.getSize() > 0) { 

            String realPath = application.getRealPath(upload); //실행되는 tomcat 서버의 경로

            fname = f.getOriginalFilename();
            fname = FileRenameUtil.checkSameFileName(fname, realPath);

            try { 
                File uploadDir = new File(realPath);

                if (!uploadDir.exists()) {
                    uploadDir.mkdirs();
                }

                // 전달된 파일을 저장합니다.
                File dest = new File(uploadDir, fname);
                f.transferTo(dest);
                map.put("fname", fname);

            } catch (IOException e) {
                e.printStackTrace();
                map.put("error", "File upload failed");
            }
        } else {
            map.put("error", "File is empty");
        }

        map.put("url", upload + System.getProperty("file.separator") + fname);

        return map;
    }

    // 파일 다운로드
    @RequestMapping(value = "/fileDownload", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<InputStreamResource> fileDownload(@RequestParam("fName") String fName)
            throws FileNotFoundException, UnsupportedEncodingException {

        String realPath = application.getRealPath(upload);

        // 전체경로를 만들어서 File객체 생성
        String fullPath = realPath + System.getProperty("file.separator") + fName;
        File file = new File(fullPath);

        if (!file.exists() || !file.isFile()) {
            throw new IOException("File not found");
        }
        FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fis);
        InputStreamResource resource = new InputStreamResource(bis);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION,
                "attachment;filename=" + new String(fName.getBytes("UTF-8"), "ISO-8859-1"));
        headers.add(HttpHeaders.CONTENT_TYPE, "application/octet-stream;charset=8859_1");
        headers.add(HttpHeaders.CONTENT_ENCODING, "binary");

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(file.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    /***************************** 1:1문의 시작 *****************************/

    @RequestMapping("/host_inquiry")
    public ModelAndView host_inquiry() {
        ModelAndView mv = new ModelAndView();

        List<InquiryDTO> list = is.getAllInquiries();
        mv.addObject("list", list);

        mv.setViewName("admins/host_inquiry");

        return mv;
    }

    @RequestMapping("/host_inquiry/answer")
    public String host_inquiry_answer() {
        return "admins/host_inquiry_answer";
    }

    /***************************** 1:1문의 끝 *****************************/
    /***************************** 회원 조회 시작 *****************************/
    @RequestMapping("/member")
    public ModelAndView member() {
        ModelAndView mv = new ModelAndView();

        List<MemberDTO> list = ms.getAllMembers();
        mv.addObject("list", list);

        mv.setViewName("admins/member");
        
        return mv;
    }

    /***************************** 회원 조회 끝 *****************************/
    /***************************** 회원 공지사항 시작 *****************************/

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
    public String user_notice_modify_save(ServiceDTO modified, @RequestParam("file") MultipartFile f, @RequestParam("selectedFile") String selectedFile) {
        System.out.println("modified.getSCate() : "+modified.getSCate());
        ServiceDTO sDto = ss.getServices(modified.getId());
        
        // 해당 id에 대해 modified 객체의 값으로 수정한다.
        sDto.setSTitle(modified.getSTitle());
        sDto.setSContent(modified.getSContent());
        sDto.setSCate(modified.getSCate());

        // 파일이 수정되었을 경우
        if(f.getSize() > 0) {
            String realPath = application.getRealPath(upload);
            String fname = f.getOriginalFilename();
            FileImageNameVo fvo = new FileImageNameVo();
            fvo.setOName(fname);
            fname = FileRenameUtil.checkSameFileName(fname, realPath);
            fvo.setFName(fname);
            try {
                File uploadDir = new File(realPath);
                if (!uploadDir.exists()) {
                    uploadDir.mkdirs();
                }
                File dest = new File(uploadDir, fname);
                f.transferTo(dest);
                sDto.setFileName(fvo);

                //ss에서 업데이트 하기
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if(selectedFile.length()<1 && sDto.getFileName() != null) { //파일이 있었는데 삭제된 경우 -> fileName을 null로 바꾸고 업데이트
            //파일이 있었는데 삭제된 경우 -> fileName을 null로 바꾸고 업데이트
            sDto.setFileName(null);

        } else {
            // 그냥 title이랑 content만 업데이트하는 경우
            // 1) 파일이 원래 없었고 file name도 nulll일때
            // 2) 파일이 계속 유지되는 경우 -> f == null fileName은 있는 경우
        }

        ss.modifyServices(sDto); // 업데이트: sDto에는 수정된 값이 들어있음

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
        System.out.println("modified.getSCate() : "+sDto.getSCate());

        // 폼양식에서 첨부파일이 전달될 때 enctype이 지정된다.
        String c_type = request.getContentType();
        if (c_type.startsWith("multipart")) {

            String fname = null;
            if (f != null && f.getSize() > 0) {
                String realPath = application.getRealPath(upload);

                fname = f.getOriginalFilename();
                FileImageNameVo fvo = new FileImageNameVo();
                fvo.setOName(fname);
                fname = FileRenameUtil.checkSameFileName(fname, realPath);
                fvo.setFName(fname);

                try {
                    File uploadDir = new File(realPath);
                    if (!uploadDir.exists()) {
                        uploadDir.mkdirs();
                    }

                    // 파일 업로드(upload폴더에 저장)
                    File dest = new File(uploadDir, fname);
                    f.transferTo(dest);

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

    /***************************** 회원 공지사항 끝 *****************************/

    /***************************** 쿠폰 시작 *****************************/
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
    public Map<String, Object> coupon_history(@RequestParam("id") String id){
        Map<String, Object> map = new HashMap<>();

        int idx = Integer.parseInt(id);

        List<CouponHistoryDTO> list = ics.getIssuedCouponsById(idx);

        for(CouponHistoryDTO item : list){
            System.out.println("item : " + item);
        }

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

        System.out.println("cDto : " + cDto);
        System.out.println("cDto : " + cDto.getCpStartEnd().getEnd());
        System.out.println("selectGrade : " + selectGrade);
        System.out.println("code : " + code);
        System.out.println("count : " + count);
        System.out.println("date : " + date);

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

    /***************************** 쿠폰 끝 *****************************/

    /***************************** 포인트 시작 *****************************/
    @RequestMapping("/point")
    public ModelAndView point() {

        ModelAndView mv = new ModelAndView();

        List<PointDTO> list = ps.getAllPoints();

        mv.addObject("list", list);
        mv.setViewName("admins/point");

        return mv;
    }

    @RequestMapping("/point/add")
    public String add_point(@RequestParam("email") String email, PointDTO pDto) {
        System.out.println("email : " + email);
        System.out.println("pDto : " + pDto);

        int cnt = ps.addPoint(email, pDto);

        if(cnt < 1){
            System.out.println("포인트 추가 실패");
            return "error";
        }

        return "redirect:/admin/point";
    }

    /***************************** 포인트 끝 *****************************/

    @RequestMapping("/recommendation")
    public String recommend() {
        return "admins/recommendation";
    }

}
