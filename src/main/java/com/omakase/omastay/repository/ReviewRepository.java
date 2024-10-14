package com.omakase.omastay.repository;
 
import java.time.LocalDateTime;

import com.omakase.omastay.entity.Good;
import com.omakase.omastay.entity.HostInfo;
import com.omakase.omastay.entity.Review;
import com.omakase.omastay.entity.enumurate.BooleanStatus;
import com.omakase.omastay.repository.custom.ReviewRepositoryCustom;

import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.Query;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;


import org.springframework.transaction.annotation.Transactional;



public interface ReviewRepository extends JpaRepository<Review, Integer>, ReviewRepositoryCustom {
            @Query("SELECT r FROM Review r " +
            "JOIN FETCH r.member m " +
            "JOIN FETCH m.grade g " +
            "JOIN FETCH r.hostInfo h " +
            "JOIN FETCH r.reservation res " +
            "JOIN FETCH res.roomInfo room " + 
            "WHERE h.id = :hIdx AND r.revStatus = 0 " + 
            "ORDER BY " +
            "CASE WHEN :sortOption = '최신순' THEN r.revDate END DESC, " +
            "CASE WHEN :sortOption = '평점 높은 순' THEN r.revRating END DESC, " +
            "CASE WHEN :sortOption = '평점 낮은 순' THEN r.revRating END ASC")
    List<Review> findAll(@Param("sortOption") String sortOption,@Param("hIdx") Integer hIdx);  
    
    @Query("SELECT g.review.id, COUNT(g) as goodCount FROM Good g " +
                "WHERE g.goodStatus = 1 AND g.review.hostInfo.id = :hIdx " +
                "GROUP BY g.review.id " +
                "ORDER BY goodCount DESC")
                List<Object[]> findAllGoodsByHost( @Param("hIdx") Integer hIdx);



    List<Review> findByHostInfoAndRevStatus(HostInfo hostInfo, BooleanStatus revStatus);

    @Query("SELECT r.member.id, COUNT(r) FROM Reservation r GROUP BY r.member.id")
    List<Object[]> countReservationsByMemIdx(); 

    //리뷰 이미지 조회
     @Query("SELECT r FROM Review r WHERE r.hostInfo.id = :hIdx")
    List<Review> findByReviewAndImage(@Param("hIdx") Integer hIdx);

    //오늘날짜 리뷰 조회
    @Query("SELECT r FROM Review r WHERE r.hostInfo = :hostInfo AND (r.revDate = :date AND r.revStatus = 0)")
    List<Review> findReviewByDate(@Param("date") LocalDateTime date, @Param("hostInfo") HostInfo hostInfo);

    //이번주 리뷰 조회
    @Query("SELECT r FROM Review r WHERE r.hostInfo = :hostInfo AND ((r.revDate <= :endOfWeek AND r.revDate >= :startOfWeek) AND r.revStatus = 0)")
    List<Review> findReviewByWeek(@Param("startOfWeek") LocalDateTime startOfWeek, @Param("endOfWeek") LocalDateTime endOfWeek, @Param("hostInfo") HostInfo hostInfo);

    //이번달 리뷰 조회\
    @Query("SELECT r FROM Review r WHERE r.hostInfo = :hostInfo AND ((r.revDate <= :endOfMonth AND r.revDate >= :startOfMonth) AND r.revStatus = 0)")
    List<Review> findReviewByMonth(@Param("startOfMonth") LocalDateTime startOfMonth, @Param("endOfMonth") LocalDateTime endOfMonth, @Param("hostInfo") HostInfo hostInfo);

    @Query("SELECT r.hostInfo.id AS hostId, COUNT(r.id) AS reviewCount, SUM(r.revRating) AS totalRating " +
            "FROM Review r " +
            "WHERE r.revStatus = 0 " + 
            "GROUP BY r.hostInfo.id")
    List<Object[]> findReviewCount();

    @Query("SELECT r.hostInfo.id AS hostId, COUNT(r.id) AS reviewCount, SUM(r.revRating) AS totalRating " +
            "FROM Review r " +
            "WHERE r.hostInfo.id = :hIdx AND r.revStatus = 0 " +
            "GROUP BY r.hostInfo.id")
        List<Object[]> findHostReviewCount(@Param("hIdx") Integer hIdx);

    //리뷰 삭제 후 상태 업데이트
    @Transactional
    @Modifying
    @Query("UPDATE Review r SET r.revStatus = 1 WHERE r.id = :revIdx")
    int updateReviewStatus(@Param("revIdx") Integer revIdx);

    //리뷰 조회
    @Query("SELECT r FROM Review r JOIN FETCH r.hostInfo h JOIN FETCH r.reservation res WHERE r.member.id = :memIdx AND r.revStatus = 0")
    List<Review> findReviewsWithRoomAndHotelByMemberId(@Param("memIdx") int memIdx);
}
