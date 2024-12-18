package com.omakase.omastay.controller;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.omakase.omastay.dto.ServiceDTO;
import com.omakase.omastay.entity.enumurate.SCate;
import com.omakase.omastay.entity.enumurate.UserAuth;
import com.omakase.omastay.service.ServiceService;
import io.jsonwebtoken.io.IOException;
import lombok.RequiredArgsConstructor;

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
import org.springframework.web.servlet.ModelAndView;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    @Autowired
    private ServiceService serviceService;

    @Value("${upload}")
    private String upload;

    @RequestMapping("/notice")
    public String usernotice() {
        return "user/notice";
    }

    // 사용자 공지사항 전체 리스트
    @RequestMapping("/noticeList/getList")
    @ResponseBody
    public Map<String, Object> noticeList() {
        Map<String, Object> map = new HashMap<>();

        List<ServiceDTO> list = serviceService.getAllServices(SCate.NOTICE, UserAuth.USER);

        map.put("data", list);

        return map;
    }

     // 사용자 공지사항 검색
    @RequestMapping("/noticelist/search")
    @ResponseBody
    public Map<String, Object> noticesearch(
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "keyword", required = false) String keyword) {

        Map<String, Object> map = new HashMap<>();

        List<ServiceDTO> list = serviceService.searchHostNotice(type, keyword, UserAuth.USER, SCate.NOTICE);

        map.put("list", list);

        return map;
    }

    //사용자 공지사항 상세보기
    @RequestMapping(value = "noticelist/view", method = RequestMethod.GET)
    public ModelAndView noticedetail(@RequestParam("id") String id) {

        ModelAndView mv = new ModelAndView();
        mv.setViewName("user/noticedetail");

        if (id != null) {
            ServiceDTO sDto = serviceService.getServices(Integer.parseInt(id));
            mv.addObject("sDto", sDto);
            mv.addObject("storage", upload);
        }

        return mv;
    }

    //사용자 공지사항 첨부 다운로드
    @RequestMapping(value = "/fileDownload", method = RequestMethod.GET)
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

    //사용자 자주묻는질문
    @RequestMapping("/faq")
    public ModelAndView userFaq() {

        ModelAndView mv = new ModelAndView();

        List<ServiceDTO> list = serviceService.getAllServices(SCate.FAQ, UserAuth.USER);



        mv.addObject("list", list);
        

        mv.setViewName("user/faq");

        return mv;
    }

    //이벤트
    @RequestMapping("/event")
    public ModelAndView userEvent() {

        ModelAndView mv = new ModelAndView();

        List<ServiceDTO> list = serviceService.getAllServices(SCate.EVENT, UserAuth.USER);
        for (ServiceDTO serviceDTO : list) {

            // sTitle에서 [] 안의 날짜 분리
            String sTitle = serviceDTO.getSTitle();
            String dateText = "";
            String remainingText = sTitle;

            if (sTitle.contains("[") && sTitle.contains("]")) {
                int startIndex = sTitle.indexOf("[");
                int endIndex = sTitle.indexOf("]");
                dateText = sTitle.substring(startIndex + 1, endIndex);
                remainingText = sTitle.substring(0, startIndex).trim() + sTitle.substring(endIndex + 1).trim();
            }
            System.out.println("dateText" + dateText);
            System.out.println("remainingText" + remainingText);

            serviceDTO.setSPeriod(dateText); 
            serviceDTO.setSTitle(remainingText);

        }
        System.out.println("리스트다옹"+list);
        System.out.println("업로드"+upload);
        mv.addObject("list", list);
        mv.setViewName("user/event");

        return mv;
    }

    @RequestMapping("/eventdetail")
    public ModelAndView eventDetail(@RequestParam("id") int id){
        ModelAndView mv = new ModelAndView();

        ServiceDTO service = serviceService.getEvent(id);

         // sTitle에서 [] 안의 날짜 분리
         String sTitle = service.getSTitle();
         String dateText = "";
         String remainingText = sTitle;

         if (sTitle.contains("[") && sTitle.contains("]")) {
             int startIndex = sTitle.indexOf("[");
             int endIndex = sTitle.indexOf("]");
             dateText = sTitle.substring(startIndex + 1, endIndex);
             remainingText = sTitle.substring(0, startIndex).trim() + sTitle.substring(endIndex + 1).trim();
         }
         System.out.println("dateText" + dateText);
         System.out.println("remainingText" + remainingText);

         service.setSPeriod(dateText); 
         service.setSTitle(remainingText);


        
        mv.addObject("event", service);
        mv.setViewName("user/eventdetail");
        return mv;
        
    }


}

