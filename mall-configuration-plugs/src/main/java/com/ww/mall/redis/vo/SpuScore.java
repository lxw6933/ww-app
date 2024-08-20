package com.ww.mall.redis.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author ww
 * @create 2024-08-20- 15:05
 * @description:
 */
@Data
public class SpuScore implements Serializable {

    private int count = 0;
    private double totalScore = 0;
    private LocalDateTime lastTime;

    public SpuScore(int count, double score) {
        this.count = count;
        this.totalScore = score;
        this.lastTime = LocalDateTime.now();
    }

    public void addScore(int count, double score) {
        this.count += count;
        this.totalScore += score;
        this.lastTime = LocalDateTime.now();
    }

    public double getAverageScore() {
        return this.count == 0 ? 0.0 : this.totalScore / this.count;
    }

}
