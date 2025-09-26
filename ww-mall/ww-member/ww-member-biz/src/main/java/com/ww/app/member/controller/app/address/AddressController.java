package com.ww.app.member.controller.app.address;

import com.ww.app.member.controller.app.address.req.MemberAddressBO;
import com.ww.app.member.controller.app.address.res.MemberAddressVO;
import com.ww.app.member.service.address.MemberAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author ww
 * @create 2025-09-26 13:56
 * @description:
 */
@Tag(name = "用户 APP - 用户收件地址")
@Validated
@RestController
@RequestMapping("/member/address")
public class AddressController {

    @Resource
    private MemberAddressService memberAddressService;

    @PostMapping
    @Operation(summary = "创建用户收件地址")
    public boolean add(@RequestBody MemberAddressBO memberAddressBO) {
        return memberAddressService.add(memberAddressBO);
    }

    @PutMapping
    @Operation(summary = "更新用户收件地址")
    public boolean update(@RequestBody MemberAddressBO memberAddressBO) {
        return memberAddressService.update(memberAddressBO);
    }

    @DeleteMapping
    @Operation(summary = "删除用户收件地址")
    @Parameter(name = "id", description = "编号", required = true)
    public boolean delete(@RequestParam("id") Long id) {
        return memberAddressService.delete(id);
    }

    @GetMapping
    @Operation(summary = "获得用户收件地址")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    public MemberAddressVO get(@RequestParam("id") Long id) {
        return memberAddressService.get(id);
    }

    @GetMapping("/default")
    @Operation(summary = "获得默认的用户收件地址")
    public MemberAddressVO getDefaultUserAddress() {
        return memberAddressService.getDefaultMemberAddress();
    }

    @GetMapping("/list")
    @Operation(summary = "获得用户收件地址列表")
    public List<MemberAddressVO> getAddressList() {
        return memberAddressService.getMemberAddressList();
    }

}
