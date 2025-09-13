package com.ww.app.redpacket.rpc;

import com.ww.app.redpacket.constants.ApiConstants;
import feign.Param;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

import java.math.BigDecimal;

/**
 * @author ww
 * @create 2024-12-30- 15:50
 * @description:
 */
@Tag(name = "RPC 服务 - 红包业务")
@FeignClient(value = ApiConstants.NAME)
public interface RedpacketApi {

    String PREFIX = ApiConstants.PREFIX;

    /**
     * 生成红包
     *
     * @param totalAmount 红包总金额
     * @param totalCount 红包个数
     * @return 红包id
     */
    @PostMapping(PREFIX + "/generateRedPacket")
    @Operation(summary = "生成红包")
    @Parameters({
            @Parameter(name = "totalAmount", description = "红包总金额", required = true, in = ParameterIn.QUERY),
            @Parameter(name = "totalCount", description = "红包数量", required = true, in = ParameterIn.QUERY),
    })
    String generateRedPacket(@Param BigDecimal totalAmount, @Param int totalCount);

    /**
     * 领取红包
     *
     * @param redPacketId 红包id
     * @return 领取红包金额
     */
    @PostMapping(PREFIX + "/receiveRedPacket")
    @Operation(summary = "领红包")
    @Parameter(name = "redPacketId", description = "红包id", required = true, in = ParameterIn.QUERY)
    String receiveRedPacket(@Param String redPacketId);

}
