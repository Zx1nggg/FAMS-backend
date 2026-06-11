package com.Zx1nggg.FAMS.modules.system.service;

import com.Zx1nggg.FAMS.modules.system.dto.ApprovalReqDTO;
import com.Zx1nggg.FAMS.modules.system.dto.RegistrationReqDTO;
import com.Zx1nggg.FAMS.modules.system.entity.RegistrationApplication;
import com.Zx1nggg.FAMS.modules.system.vo.RegistrationApplicationVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 入驻申请表 Service 接口
 * </p>
 *
 * @author Zx1nggg
 * @since 2026-06-09
 */
public interface IRegistrationApplicationService extends IService<RegistrationApplication> {

    /**
     * 提交入驻申请
     */
    void submitApplication(RegistrationReqDTO dto);

    /**
     * 检查手机号是否已被占用（包括sys_user表和申请表）
     */
    boolean isPhoneAvailable(String phone);

    /**
     * 分页查询入驻申请列表
     */
    Page<RegistrationApplicationVO> listApplications(Integer pageNum, Integer pageSize, Integer status);

    /**
     * 查看申请详情
     */
    RegistrationApplicationVO getApplicationDetail(Long id);

    /**
     * 审批入驻申请（通过或拒绝）
     * @param id 申请ID
     * @param reviewerId 审批人ID
     * @param dto 审批请求
     * @return 审批通过时返回新创建的用户ID，拒绝时返回null
     */
    Long approveApplication(Long id, Long reviewerId, ApprovalReqDTO dto);

    /**
     * 查询申请状态（按手机号查询）
     */
    RegistrationApplicationVO queryStatusByPhone(String phone);
}
