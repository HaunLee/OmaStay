package com.omakase.omastay.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

import com.omakase.omastay.dto.ReservationDTO;
import com.omakase.omastay.entity.Reservation;
import com.omakase.omastay.entity.RoomInfo;
import com.omakase.omastay.entity.enumurate.ResStatus;
import com.omakase.omastay.repository.custom.ReservationRepositoryCustom;

import jakarta.persistence.LockModeType;



public interface ReservationRepository extends JpaRepository<Reservation, Integer>, ReservationRepositoryCustom {

    List<Reservation> findByRoomInfo(RoomInfo roomInfo);

    @Modifying
    @Transactional
    @Query("Update Reservation r set r.resStatus = 1 where r.id in :ids AND r.resStatus = 0")
    int confirmById(@Param("ids") int[] ids);

    @Modifying
    @Transactional
    @Query("Update Reservation r set r.resStatus = 2 where r.id in :ids AND r.resStatus = 0")
    int rejectById(@Param("ids") int[] ids);

    @Modifying
    @Query("UPDATE Reservation r SET r.resStatus = 3 WHERE r.startEndVo.end < CURRENT_TIMESTAMP AND r.resStatus = 1")
    void updateExpiredStatuses();

    // 하루가 지난 예약 중 Sale 테이블에 없는 예약의 ID를 조회
    @Query("SELECT r FROM Reservation r LEFT JOIN FETCH Sales s ON r.id = s.reservation.id WHERE r.startEndVo.end < :yesterday AND s.id IS NULL")
    List<Reservation> findExpiredReservationsNotInSale(@Param("yesterday") LocalDateTime yesterday);

    // 모든 예약 중 Sale 테이블에 없는 예약의 ID를 조회
    @Query("SELECT r FROM Reservation r LEFT JOIN FETCH Sales s ON r.id = s.reservation.id WHERE r.startEndVo.end < :today AND s.id IS NULL")
    List<Reservation> findAllExpiredReservationsNotInSale(@Param("today") LocalDateTime today);

    @Query("SELECT r FROM Reservation r WHERE r.roomInfo.id = :roomInfo AND ((r.startEndVo.start < :end AND r.startEndVo.end > :start))")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Reservation> checkSameRoom(@Param("roomInfo") int roomInfo, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT r FROM Reservation r WHERE r.member.id = :memberId AND r.startEndVo.end < CURRENT_TIMESTAMP ORDER BY r.startEndVo.end DESC")
    Page<Reservation> findByMemIdxAndEndBefore(@Param("memberId") int memberId, Pageable pageable);
    
    //오늘날짜 예약 조회
    @Query("SELECT r FROM Reservation r WHERE r.roomInfo = :roomInfo AND (r.startEndVo.start <= :date AND r.startEndVo.end >= :date) AND (r.resStatus = 1 OR r.resStatus = 3)")
    List<Reservation> findReservationsByDate(@Param("date") LocalDateTime date, @Param("roomInfo") RoomInfo roomInfo);

    //이번주 예약 조회
    @Query("SELECT r FROM Reservation r WHERE r.roomInfo = :roomInfo AND (r.startEndVo.start <= :endOfWeek AND r.startEndVo.end >= :startOfWeek) AND (r.resStatus = 1 OR r.resStatus = 3)")
    List<Reservation> findReservationsByWeek(@Param("startOfWeek") LocalDateTime startOfWeek, @Param("endOfWeek") LocalDateTime endOfWeek, @Param("roomInfo") RoomInfo roomInfo);

    //이번달 예약 조회
    @Query("SELECT r FROM Reservation r WHERE r.roomInfo = :roomInfo AND (r.startEndVo.start <= :endOfMonth AND r.startEndVo.end >= :startOfMonth) AND (r.resStatus = 1 OR r.resStatus = 3)")
    List<Reservation> findReservationsByMonth(@Param("startOfMonth") LocalDateTime startOfMonth, @Param("endOfMonth") LocalDateTime endOfMonth, @Param("roomInfo") RoomInfo roomInfo);

    //입실예정정보(시작일 제일 빠른 순으로 정렬)
    @Query("SELECT r FROM Reservation r WHERE r.roomInfo = :roomInfo AND (r.startEndVo.start >= :nowDate AND r.resStatus = 1) ORDER BY r.startEndVo.start ASC")
    List<Reservation> findReservationsByCheckIn(@Param("nowDate") LocalDateTime nowDate, @Param("roomInfo") RoomInfo roomInfo);

    //예약대기정보
    @Query("SELECT r FROM Reservation r WHERE r.resStatus = 0")
    List<Reservation> findReservationsByPending();
  
    @Query("SELECT r FROM Reservation r JOIN r.nonMember nm WHERE r.resNum = :resNum AND nm.nonEmail = :nonEmail")
    Reservation findByResNumAndNonEmail(@Param("resNum") String resNum, @Param("nonEmail") String nonEmail);

    @Query("SELECT r FROM Reservation r WHERE r.member.id = :memIdx AND r.startEndVo.end > CURRENT_TIMESTAMP")
    List<Reservation> findByMemIdx(@Param("memIdx") Integer memIdx);

    @Query("SELECT r FROM Reservation r WHERE r.roomInfo.id = :roomInfo AND r.startEndVo.start < :end AND r.startEndVo.end > :start")
    Optional<Reservation> findConflictingReservationWithLock(@Param("roomInfo") int roomInfo, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    //관리자의 회원조회에서 회원의 최근 3개월 예약 확정&사용완료 횟수를 가져옴
    @Query("SELECT COUNT(r) FROM Reservation r JOIN r.payment p WHERE r.member.id = :memId AND p.payDate >= :time AND r.resStatus IN (1, 3)")
    Integer get3MonthCount(@Param("memId") Integer memId, @Param("time") LocalDateTime time);
    
    //관리자의 회원조회에서 회원의 전체 예약 확정&사용완료 횟수를 가져옴
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.member.id = :memId AND r.resStatus IN (1, 3)")
    Integer getTotalCount(@Param("memId") Integer memId);

    @Query("SELECT r FROM Reservation r JOIN FETCH r.roomInfo ri JOIN FETCH r.member m WHERE m.id = :memberId")
    Page<Reservation> findByMemberId(@Param("memberId") Integer memberId, Pageable pageable);

    //해당 호텔 예약자만 리뷰작성 가능
    @Query("SELECT DISTINCT r FROM Reservation r JOIN r.roomInfo ro JOIN ro.hostInfo h " +
            "WHERE r.member.id = :memIdx AND h.id = :hIdx AND r.resStatus = 3")
            List<ReservationDTO> findSingleByMemIdxAndHIdx(@Param("hIdx") Integer hIdx, @Param("memIdx") Integer memIdx);

   
}


