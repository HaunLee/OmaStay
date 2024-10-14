package com.omakase.omastay.service;
import com.omakase.omastay.dto.GoodDTO;
import com.omakase.omastay.dto.HostInfoDTO;
import com.omakase.omastay.dto.ReviewDTO;
import com.omakase.omastay.entity.Good;
import com.omakase.omastay.entity.HostInfo;
import com.omakase.omastay.entity.Member;
import com.omakase.omastay.entity.Reservation;
import com.omakase.omastay.entity.Review;
import com.omakase.omastay.entity.enumurate.BooleanStatus;
import com.omakase.omastay.mapper.GoodMapper;
import com.omakase.omastay.mapper.HostInfoMapper;
import com.omakase.omastay.mapper.ReviewMapper;
import com.omakase.omastay.repository.ReviewRepository;
import com.omakase.omastay.vo.FileImageNameVo;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;



@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Transactional
    public Integer addReview(ReviewDTO reviewDTO, List<String> onames, List<String> fnames) {
        Review review = ReviewMapper.INSTANCE.toReview(reviewDTO);

        review.setRevDate(LocalDateTime.now());
        review.setRevNone(null);
        review.setRevStatus(BooleanStatus.TRUE);

        FileImageNameVo vo = new FileImageNameVo();
        String allFnames = String.join(",", fnames);
        String allOnames = String.join(",", onames);
        vo.setFName(allFnames);
        vo.setOName(allOnames);
        review.setRevFileImageNameVo(vo);

        Review savedReview = reviewRepository.save(review);

        return savedReview.getId();
    }

    public Review findById(Integer revIdx) {
        return reviewRepository.findById(revIdx)
                .orElseThrow(() -> new EntityNotFoundException("Review not found with id: " + revIdx)); 
    }

      public List<Review> findAllReview(String sortOption,Integer hIdx){
        
        return reviewRepository.findAll(sortOption,hIdx);
    }


     // 호스트 전체 리뷰 가져오기
    public List<ReviewDTO> getAllReview(HostInfoDTO hostInfoDTO) {

        HostInfo hostInfo = HostInfoMapper.INSTANCE.toHostInfo(hostInfoDTO);

        List<Review> review = reviewRepository.findByHostInfoAndRevStatus(hostInfo, BooleanStatus.TRUE);

        return ReviewMapper.INSTANCE.toReviewDTOList(review);
    }

    // 호스트 리뷰 검색
    public List<ReviewDTO> searchHostReview(String type, String keyword, int hIdx){

        List<Review> review = reviewRepository.searchHostReview(type, keyword, hIdx);
 
        return ReviewMapper.INSTANCE.toReviewDTOList(review);
    }

    // 호스트 리뷰 상세보기
    public ReviewDTO getReview(int id) {
        Review review  = reviewRepository.findById(id).get();
        return ReviewMapper.INSTANCE.toReviewDTO(review);
    }

    public List<Object[]> getResCounts(){
        return reviewRepository.countReservationsByMemIdx();
    }

    public List<ReviewDTO> getAllReviewImages(Integer hIdx) {
        List<Review> reviewImage = reviewRepository.findByReviewAndImage(hIdx);
    return ReviewMapper.INSTANCE.toReviewDTOList(reviewImage);
    }


    //호스트 오늘 리뷰 가져오기
    public List<ReviewDTO> getReviewDay(HostInfoDTO hostInfoDTO) {

        HostInfo hostInfo = HostInfoMapper.INSTANCE.toHostInfo(hostInfoDTO);

        LocalDateTime date = LocalDate.now().atStartOfDay();

        List<Review> review = reviewRepository.findReviewByDate(date, hostInfo);

        return ReviewMapper.INSTANCE.toReviewDTOList(review);
    }

    //호스트 이번주 리뷰 가져오기
    public List<ReviewDTO> getReviewWeek(HostInfoDTO hostInfoDTO) {

        HostInfo hostInfo = HostInfoMapper.INSTANCE.toHostInfo(hostInfoDTO);

        LocalDateTime now =LocalDate.now().atStartOfDay();
        LocalDateTime startOfWeek = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDateTime endOfWeek = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        List<Review> review = reviewRepository.findReviewByWeek(startOfWeek, endOfWeek, hostInfo);

        return ReviewMapper.INSTANCE.toReviewDTOList(review);
    }

    //호스트 이번달 리뷰 가져오기
    public List<ReviewDTO> getReviewMonth(HostInfoDTO hostInfoDTO) {

        HostInfo hostInfo = HostInfoMapper.INSTANCE.toHostInfo(hostInfoDTO);

        LocalDateTime now = LocalDate.now().atStartOfDay();
        LocalDateTime startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth());
        LocalDateTime endOfMonth = now.with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX);

        List<Review> review = reviewRepository.findReviewByMonth(startOfMonth, endOfMonth, hostInfo);

        return ReviewMapper.INSTANCE.toReviewDTOList(review);
    }

    //메인 페이지 리뷰 개수 & 평균
    public Map<Integer, Map<String, Object>> getRecomReviewCount(){
        List<Object[]> reviewCountList = reviewRepository.findReviewCount();

        Map<Integer, Map<String, Object>> reviewCountMap = new HashMap<>();

        for (Object[] row : reviewCountList) {
            Integer hostId = (Integer) row[0];
            
            Long reviewCount = ((Number) row[1]).longValue(); 
            Double totalRating = ((Number) row[2]).doubleValue();

            Double averageRating = (reviewCount > 0) ? totalRating / reviewCount : 0.0;

            Map<String, Object> stats = new HashMap<>();
            stats.put("reviewCount", reviewCount);
            stats.put("averageRating", averageRating); 

            reviewCountMap.put(hostId, stats);
        }

        return reviewCountMap;
    }

    //상세 페이지 리뷰 개수 & 평균
    public String getReviewStatsByHostId(Integer hIdx) {
        List<Object[]> reviewCountList = reviewRepository.findHostReviewCount(hIdx);
            if (reviewCountList.isEmpty()) {
                return "0.0, 0"; 
            }
            
            Object[] row = reviewCountList.get(0);

            Long reviewCount = ((Number) row[1]).longValue(); 
            Double totalRating = ((Number) row[2]).doubleValue(); 
            
            Double averageRating = (reviewCount > 0) ? totalRating / reviewCount : 0.0;

            return String.format("%.1f, %d", averageRating, reviewCount);
        }
    
    public void deleteReviewById(int revIdx) {
        reviewRepository.updateReviewStatus(revIdx);
    }

    public List<Object[]> getGoodStatus(Integer hIdx) {
        return reviewRepository.findAllGoodsByHost(hIdx);
    }

}

    
