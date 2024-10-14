package com.omakase.omastay.controller;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.google.cloud.WriteChannel;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.omakase.omastay.dto.GoodDTO;
import com.omakase.omastay.dto.GradeDTO;
import com.omakase.omastay.dto.HostInfoDTO;
import com.omakase.omastay.dto.ImageDTO;
import com.omakase.omastay.dto.MemberDTO;
import com.omakase.omastay.dto.ReservationDTO;
import com.omakase.omastay.dto.ReviewCommentDTO;
import com.omakase.omastay.dto.ReviewDTO;
import com.omakase.omastay.dto.RoomInfoDTO;
import com.omakase.omastay.entity.Reservation;
import com.omakase.omastay.entity.Review;
import com.omakase.omastay.service.FileUploadService;
import com.omakase.omastay.service.GoodService;
import com.omakase.omastay.service.MemberService;
import com.omakase.omastay.service.ReservationService;
import com.omakase.omastay.service.ReviewCommentService;
import com.omakase.omastay.service.ReviewService;



@Controller
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private GoodService goodService;

    @Autowired
    private ReviewCommentService reviewCommentService;

    @Autowired
    private FileUploadService fileUploadService;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private MemberService memberService;

    @Value("${upload}")
    private String uploadPath;


    @PostMapping("/review_insert")
    @ResponseBody
    public ResponseEntity<Integer> addReview(@RequestParam Map<String, String> params) {
        System.out.println("들어는 왔니?");
        System.out.println("param: " + params);
        
        Integer memIdx = Integer.parseInt(params.get("memIdx"));
        Integer hIdx = Integer.parseInt(params.get("hIdx"));
        MemberDTO memberDTO = memberService.getMember(memIdx);
        ReservationDTO latestReservation = null;
    
        try {
            // 사용자의 예약 내역에서 최근 예약 건을 가져옴
            List<ReservationDTO> reservationList = reservationService.getMemIdxListByHIdx(memIdx, hIdx);
            System.out.println("리뷰 작성 가능한 회원: " + reservationList);
            if (!reservationList.isEmpty()) {
                  latestReservation = reservationList.get(0); // 첫 번째 결과를 선택 (필터링 가능)
                  
            }else{
                return ResponseEntity.ok(0);
            
            }
        
            ReviewDTO reviewDTO = new ReviewDTO();
            String revContent = params.get("revContent");
            System.out.println("왜 잘리는거냐: " + revContent);
            
            String revRatingStr = params.get("revRating"); 
            float revRating = Float.parseFloat(revRatingStr);
           
            reviewDTO.setRevContent(revContent);
            reviewDTO.setRevRating(revRating);
            reviewDTO.setMemIdx(memIdx);
            reviewDTO.setHIdx(hIdx);
            reviewDTO.setResIdx(latestReservation.getId());
            reviewDTO.setRevWriter(memberDTO.getMemName());
    
            // 파일 이름들 리스트 생성
            List<String> onames = new ArrayList<>();
            List<String> fnames = new ArrayList<>();
    
            // 파일 이름들을 파라미터에서 추출
            for (int i = 0; ; i++) {
                String onameKey = "oname_" + i;
                String fnameKey = "fname_" + i;
    
                if (params.containsKey(onameKey) && params.containsKey(fnameKey)) {
                    onames.add(params.get(onameKey));
                    fnames.add(params.get(fnameKey));
                } else {
                    break; 
                }
            }
    
            // 리뷰 저장
            Integer newRevIdx = reviewService.addReview(reviewDTO, onames, fnames);
    
            return ResponseEntity.ok(1);  // 성공시 1 반환
    
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(-1);  // 에러 발생시 -1 반환
        }
    }
    
    @RequestMapping("/review_list")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> allReviewList(@RequestParam(value = "sort", defaultValue = "최신순") String sortOption, @RequestParam("hIdx") Integer hIdx) {
        System.out.println("호텔번호 잘 들어오냐" + hIdx);
    
        // 리뷰 리스트 가져오기
        List<Review> reviewList = reviewService.findAllReview(sortOption, hIdx);
        List<ReviewCommentDTO> allReviewCommentList = reviewCommentService.findAllReviewComment(); 
        List<Map<String, Object>> responseList = new ArrayList<>();
    
        // 리뷰 이미지 리스트와 추천 데이터 가져오기
        List<ReviewDTO> reviewImages = reviewService.getAllReviewImages(hIdx);
        List<Object[]> goodList = reviewService.getGoodStatus(hIdx); // Object[] -> goodCount 포함
    
        // 추천수를 매핑하기 위한 맵 생성 (key: reviewId, value: goodCount)
        Map<Integer, Long> goodCountMap = new HashMap<>();
        for (Object[] result : goodList) {
            Integer reviewId = (Integer) result[0];
            Long goodCount = (Long) result[1];
            goodCountMap.put(reviewId, goodCount);  // 리뷰 ID로 추천수 맵핑
        }
    
        for (Review review : reviewList) {
            ReviewDTO reviewDTO = new ReviewDTO();
            reviewDTO.setRevContent(review.getRevContent());
            reviewDTO.setRevRating(review.getRevRating());
            reviewDTO.setMemIdx(review.getMember().getId());
            reviewDTO.setResIdx(review.getReservation().getId());
            reviewDTO.setHIdx(review.getHostInfo().getId());
            reviewDTO.setRevDate(review.getRevDate());
            reviewDTO.setId(review.getId());
    
            MemberDTO memberDTO = new MemberDTO();
            memberDTO.setMemName(review.getMember().getMemName());
    
            RoomInfoDTO roomInfoDTO = new RoomInfoDTO();
            roomInfoDTO.setRoomName(review.getReservation().getRoomInfo().getRoomName());
    
            HostInfoDTO hostInfoDTO = new HostInfoDTO();
            hostInfoDTO.setHname(review.getHostInfo().getHname());
    
            ReservationDTO reservationDTO = new ReservationDTO();
            reservationDTO.setMemIdx(review.getMember().getId());
    
            GradeDTO gradeDTO = new GradeDTO();
            gradeDTO.setGCate(review.getMember().getGrade().getGCate());
    
            Integer revIdx = review.getId();
            List<ReviewCommentDTO> matchedReviewCommentList = new ArrayList<>();
    
            // 리뷰 코멘트 매칭
            for (ReviewCommentDTO comment : allReviewCommentList) {
                if (comment.getRevIdx().equals(revIdx)) {
                    matchedReviewCommentList.add(comment);
                }
            }
    
            // 리뷰 이미지 처리
            List<String> imagePaths = new ArrayList<>();
            for (ReviewDTO reviewDtoImg : reviewImages) {
                if (reviewDtoImg.getId() == review.getId()) {
                    if (reviewDtoImg.getRevFileImageNameVo() != null && reviewDtoImg.getRevFileImageNameVo().getFName() != null) {
                        String[] fileNames = reviewDtoImg.getRevFileImageNameVo().getFName().split(",");
                        for (String fileName : fileNames) {
                            String reviewImg = uploadPath + "review/" + fileName;
                            imagePaths.add(reviewImg);
                        }
                    }
                }
            }
    
            // goodCount 가져오기 (없으면 0)
            Long goodCount = goodCountMap.getOrDefault(revIdx, 0L);
    
            Map<String, Object> response = new HashMap<>();
            response.put("review", reviewDTO);
            response.put("member", memberDTO);
            response.put("hostinfo", hostInfoDTO);
            response.put("room", roomInfoDTO);
            response.put("grade", gradeDTO);
            response.put("reservation", reservationDTO);
            response.put("reviewImages", imagePaths);
            response.put("goodCount", goodCount);  // 추천 수 추가
            response.put("reviewComment", matchedReviewCommentList.isEmpty() ? null : matchedReviewCommentList);
    
            responseList.add(response);
        }
    
        Map<String, Object> response = new HashMap<>();
        response.put("reviewList", responseList);
    
        return ResponseEntity.ok(response);
    }
    
    
    //review 모달창 띄우기
    @GetMapping("/review_write")
    public String showReviewWriteModal() {
        return "search/review_write";
    }

     

    @PostMapping("/upload_image")
    @ResponseBody
    public Map<String, String> uploadImage(@RequestParam("file") MultipartFile file) throws Exception {
        Map<String, String> map = new HashMap<>();

        if (file.getSize() > 0) {
            // 파일명 생성
            String originalFilename = file.getOriginalFilename();
            String uniqueFileName = UUID.randomUUID().toString() + "_" + originalFilename;
    
            // 파일 업로드를 FileUploadService로 처리 (Google Cloud Storage에 저장)
            String fileUrl = fileUploadService.uploadFile(file, "review", uniqueFileName);
    
            // 저장된 파일의 URL 및 파일명 정보 리턴
            map.put("url", fileUrl);
            map.put("fname", uniqueFileName);  // 저장된 파일 이름
            map.put("oname", originalFilename); // 원본 파일 이름
        }
    
        return map;
    }

    //추천 저장
    @RequestMapping("/good_insert")
    public  ResponseEntity<Map<String, Object>> goodInsert(GoodDTO goodDTO) {
        System.out.println("추천 들어왔냥"+goodDTO);

        Map<String, Object> result = goodService.addGood(goodDTO); 

        return ResponseEntity.ok(result); 
    }

    @RequestMapping("/review_resCount")
    public ResponseEntity<List<String>> resCounts() {
        System.out.println("예약 수 가져오기");
        List<Object[]> resCounts = reviewService.getResCounts();

        List<String> resCountsList = new ArrayList<>();
        for (Object[] resCount : resCounts) {
            String resCountString = String.format("memIdx:%d 예약개수:%d", resCount[0], resCount[1]);
            resCountsList.add(resCountString);
        }
        return ResponseEntity.ok(resCountsList);
    }
    
    @PostMapping("/review_delete")
    public ResponseEntity<?> deleteReview(@RequestParam int revIdx) {
        reviewService.deleteReviewById(revIdx);
        return ResponseEntity.ok().build();
    }

}



  
    
    
