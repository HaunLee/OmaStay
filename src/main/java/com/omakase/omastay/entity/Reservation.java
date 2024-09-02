package com.omakase.omastay.entity;

import com.omakase.omastay.vo.StartEndVo;
import com.omakase.omastay.entity.enumurate.ResStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "reservation")
@ToString(exclude = {"member", "roomFacility", "nonMember", "payment"})
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "res_idx", nullable = false)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_idx", referencedColumnName = "room_idx")
    private RoomFacilities roomFacility;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mem_idx", referencedColumnName = "mem_idx")
    private Member member;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "non_idx", referencedColumnName = "non_idx")
    private NonMember nonMember;


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pay_idx", referencedColumnName = "pay_idx")
    private Payment payment;

    @Column(name = "res_num", nullable = false, length = 100)
    private String resNum;

    //예약 시작일 종료일
    @Embedded
    private StartEndVo startEndVo;

    @Column(name = "res_start", nullable = false, insertable = false, updatable = false)
    private LocalDateTime resStart;

    @Column(name = "res_end", nullable = false, insertable = false, updatable = false)
    private LocalDateTime resEnd;

    @Column(name = "res_person", nullable = false)
    private int resPerson;

    @Column(name = "res_price", nullable = false)
    private int resPrice;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "res_status", nullable = false)
    private ResStatus resStatus;

    @Column(name = "res_none", length = 100)
    private String resNone;
}