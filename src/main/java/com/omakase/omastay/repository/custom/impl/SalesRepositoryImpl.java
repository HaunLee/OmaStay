package com.omakase.omastay.repository.custom.impl;

import com.omakase.omastay.dto.custom.Top5SalesDTO;
import com.omakase.omastay.entity.QHostInfo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.List;
import com.omakase.omastay.dto.custom.CalculationCustomDTO;
import com.omakase.omastay.dto.custom.HostSalesDTO;
import com.omakase.omastay.entity.QPayment;
import com.omakase.omastay.entity.QReservation;
import com.omakase.omastay.entity.QReview;
import com.omakase.omastay.entity.QRoomInfo;
import com.omakase.omastay.entity.QSales;
import com.omakase.omastay.entity.Sales;
import com.omakase.omastay.entity.enumurate.PayStatus;
import com.omakase.omastay.repository.custom.SalesRepositoryCustom;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;

import groovy.transform.Undefined.EXCEPTION;

public class SalesRepositoryImpl implements SalesRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static final QSales sales = QSales.sales;
    public static final QReservation reservation = QReservation.reservation;
    public static final QRoomInfo roomInfo = QRoomInfo.roomInfo;
    public static final QPayment payment = QPayment.payment;

    public static final QHostInfo hostInfo = QHostInfo.hostInfo;
    public static final QReview review = QReview.review;


    public SalesRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    //이번달 전체 지역 판매 실적 Top5
    @Override
    public List<Top5SalesDTO> getTop5SalesThisMonth(LocalDate firstDay, LocalDate today){

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(payment.payStatus.eq(PayStatus.PAY));
        builder.and(hostInfo.hname.isNotNull());

        return queryFactory
                    .select(Projections.constructor(
                            Top5SalesDTO.class,
                            hostInfo.hname.as("hostName"),
                            payment.nsalePrice.castToNum(Integer.class).sum().as("totalSales")
                        ))
                    .from(sales)
                    .join(sales.hostInfo, hostInfo)
                    .join(sales.reservation, reservation)
                    .join(reservation.roomInfo, roomInfo)
                    .join(reservation.payment, payment)
                    .where(
                        isAfterStartDate(firstDay.toString()),
                        isBeforeEndDate(today.toString())
                    ) // hname이 null이 아닌 조건을 추가
                    .groupBy(hostInfo.id) // hname으로 그룹핑
                    .orderBy(payment.nsalePrice.castToNum(Integer.class).sum().desc()) // 총 매출로 정렬
                    .limit(5) // 상위 5개 제한
                    .fetch();
    
    }

    //관리자 판매 실적 - 판매 실적 Top5 검색
    @Override
    public List<Top5SalesDTO> searchTop5Sales(String startDate, String endDate, String region){
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(payment.payStatus.eq(PayStatus.PAY));
        builder.and(hostInfo.hname.isNotNull());

        return queryFactory
                    .select(Projections.constructor(
                            Top5SalesDTO.class,
                            hostInfo.hname.as("hostName"),
                            payment.nsalePrice.castToNum(Integer.class).sum().as("totalSales")
                        ))
                    .from(sales)
                    .join(sales.hostInfo, hostInfo)
                    .join(sales.reservation, reservation)
                    .join(reservation.roomInfo, roomInfo)
                    .join(reservation.payment, payment)
                    .where(
                        eqRegion(region),
                        isAfterStartDate(startDate),
                        isBeforeEndDate(endDate)
                    ) 
                    .groupBy(hostInfo.id) // hname으로 그룹핑
                    .orderBy(payment.nsalePrice.castToNum(Integer.class).sum().desc()) // 총 매출로 정렬
                    .limit(5) // 상위 5개 제한
                    .fetch();
    }

    //관리자 판매 실적 - 검색
    @Override
    public List<Sales> searchSales(String startDate, String endDate, String region) {

        return queryFactory.selectFrom(sales)
                .join(sales.hostInfo, hostInfo)
                .join(sales.reservation, reservation)
                .join(reservation.payment, payment)
                .join(reservation.roomInfo, roomInfo)
                .where(
                    eqRegion(region),
                    isAfterStartDate(startDate),
                    isBeforeEndDate(endDate)
                )
                .orderBy(sales.id.desc())
                .fetch();
    }

    //region이 null이 아닌 경우 region 조건 추가
    private BooleanExpression eqRegion(String region) {
        if (region.equals("전체") || region.isEmpty()) {
            return null;
        }
        return sales.hostInfo.region.contains(region);
    }

    private BooleanExpression isAfterStartDate(String startDate) {
        if (startDate == null || startDate.isEmpty()) {
            return null;
        }
        try {
            LocalDate startDateTime = LocalDate.parse(startDate, formatter);
            return sales.salDate.goe(startDateTime);
        } catch (EXCEPTION e) {
            // 날짜 형식이 잘못된 경우 처리
            return null;
        }
    }

    private BooleanExpression isBeforeEndDate(String endDate) {
        if (endDate == null || endDate.isEmpty()) {
            return null;
        }
        try {
            LocalDate endDateTime = LocalDate.parse(endDate, formatter);
            return sales.salDate.loe(endDateTime);
        } catch (EXCEPTION e) {
            // 날짜 형식이 잘못된 경우 처리
            return null;
        }
    }

    @Override
    public List<HostSalesDTO> findHostSales(Integer hidx) {
        return queryFactory
        .select(Projections.constructor(HostSalesDTO.class,roomInfo.roomName, roomInfo.roomType, reservation.startEndVo, reservation.resNum, payment.payMethod, payment.payDate, sales.salDate, payment.nsalePrice))
                .from(sales)
                .join(sales.reservation, reservation)
                .join(reservation.roomInfo, roomInfo)
                .join(reservation.payment, payment)
                .where(roomInfo.hostInfo.id.eq(hidx))
                .orderBy(sales.id.desc())
                .fetch();
    }
     
    

    @Override
    public List<HostSalesDTO> searchHostSales(String roomType, String startDate, String endDate, Integer hidx) {
        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;
        if (startDate != null && endDate != null) {
        startDateTime = LocalDate.parse(startDate, formatter).atStartOfDay();
        endDateTime = LocalDate.parse(endDate, formatter).atTime(23, 59, 59);
        }

        return queryFactory
        .select(Projections.constructor(HostSalesDTO.class,roomInfo.roomName, roomInfo.roomType, reservation.startEndVo, reservation.resNum, payment.payMethod, payment.payDate, sales.salDate, payment.nsalePrice))
                .from(sales)
                .join(sales.reservation, reservation)
                .join(reservation.roomInfo, roomInfo)
                .join(reservation.payment, payment)
                .where(roomInfo.hostInfo.id.eq(hidx)
                        .and(roomType != null ? roomInfo.roomType.eq(roomType) : null)
                        .and(startDate != null ? reservation.startEndVo.start.after(startDateTime) : null)
                        .and(endDate != null ? reservation.startEndVo.end.before(endDateTime) : null))
                .orderBy(sales.id.desc())
                .fetch();
    }

    @Override
    public List<HostSalesDTO> findHostYearSales(Integer hidx, Integer year) {

        return queryFactory
        .select(Projections.constructor(HostSalesDTO.class,roomInfo.roomName, roomInfo.roomType, reservation.startEndVo, reservation.resNum, payment.payMethod, payment.payDate, sales.salDate, payment.nsalePrice))
                .from(sales)
                .join(sales.reservation, reservation)
                .join(reservation.roomInfo, roomInfo)
                .join(reservation.payment, payment)
                .where(roomInfo.hostInfo.id.eq(hidx)
                        .and(year != null ? payment.payDate.year().eq(year) : null))
                .orderBy(sales.id.desc())
                .fetch();
    }

    @Override
    public List<HostSalesDTO> findHostMonthSales(Integer hidx, Integer year, Integer month) {

        return queryFactory
        .select(Projections.constructor(HostSalesDTO.class,roomInfo.roomName, roomInfo.roomType, reservation.startEndVo, reservation.resNum, payment.payMethod, payment.payDate, sales.salDate, payment.nsalePrice))
                .from(sales)
                .join(sales.reservation, reservation)
                .join(reservation.roomInfo, roomInfo)
                .join(reservation.payment, payment)
                .where(roomInfo.hostInfo.id.eq(hidx)
                        .and(year != null ? payment.payDate.year().eq(year) : null)
                        .and(month != null ? payment.payDate.month().eq(month) : null))
                .orderBy(sales.id.desc())
                .fetch();
    }


    @Override
    public CalculationCustomDTO findHostMonthPayment(Integer hIdx, LocalDate firstDay, LocalDate lastDay){

        return queryFactory
        .select(Projections.constructor(CalculationCustomDTO.class,
            Expressions.asNumber(sales.reservation.payment.nsalePrice.castToNum(Integer.class)).sum().intValue(), // nsalePrice를 Integer로 변환 후 sum() 적용
            Expressions.asNumber(sales.reservation.payment.salePrice.castToNum(Integer.class)).sum().intValue()))
        .from(sales)
        .join(sales.reservation, reservation)
        .join(sales.reservation.payment, payment)
        .where(sales.hostInfo.id.eq(hIdx)
            .and(sales.salDate.between(firstDay, lastDay)))
        .fetchOne(); // 결과가 하나일 경우 fetchOne() 사용
    }



}
