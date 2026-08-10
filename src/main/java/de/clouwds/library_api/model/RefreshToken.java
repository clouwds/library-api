package de.clouwds.library_api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Entity
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String tokenHash;

    @NotNull
    @Column(nullable = false)
    private Date issuedAt;

    @NotNull
    @Column(nullable = false)
    private Date expiresAt;

    @NotNull
    @Column(nullable = false)
    private boolean used;

    @NotNull
    @ManyToOne
    private Member member;


    public RefreshToken() {
    }

    private RefreshToken(String tokenHash, Date issuedAt, Date expiresAt, boolean used, Member member) {
        this.tokenHash = tokenHash;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.used = used;
        this.member = member;
    }

    public static RefreshToken issuedNow(String tokenHash, Member member, Duration validity) {
        Instant now = Instant.now();
        return new RefreshToken(tokenHash, Date.from(now), Date.from(now.plus(validity)), false, member);
    }

    public Long getId() {
        return id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public Date getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(Date issuedAt) {
        this.issuedAt = issuedAt;
    }

    public Date getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Date expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
    }
}
