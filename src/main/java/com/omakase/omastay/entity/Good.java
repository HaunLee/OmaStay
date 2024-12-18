package com.omakase.omastay.entity;

import com.omakase.omastay.entity.enumurate.BooleanStatus;
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
@Table(name = "good")
@ToString(exclude = {"review", "member"})
public class Good {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "good_idx", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rev_idx" , referencedColumnName = "rev_idx")
    private Review review = new Review();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mem_idx" , referencedColumnName = "mem_idx")
    private Member member = new Member();

    @Column(name = "good_date", nullable = false)
    private LocalDateTime goodDate;

    //FALSE : 추천 취소, TRUE : 추천
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "good_status", nullable = false)
    private BooleanStatus goodStatus;

    @Column(name = "good_none", length = 100)
    private String goodNone;
}