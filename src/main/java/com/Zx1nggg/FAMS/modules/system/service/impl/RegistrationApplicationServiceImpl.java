package com.Zx1nggg.FAMS.modules.system.service.impl;

import com.Zx1nggg.FAMS.common.exception.BusinessException;
import com.Zx1nggg.FAMS.modules.base.entity.Farm;
import com.Zx1nggg.FAMS.modules.base.mapper.FarmMapper;
import com.Zx1nggg.FAMS.modules.system.dto.ApprovalReqDTO;
import com.Zx1nggg.FAMS.modules.system.dto.RegistrationReqDTO;
import com.Zx1nggg.FAMS.modules.system.entity.RegistrationApplication;
import com.Zx1nggg.FAMS.modules.system.entity.User;
import com.Zx1nggg.FAMS.modules.system.mapper.RegistrationApplicationMapper;
import com.Zx1nggg.FAMS.modules.system.mapper.UserMapper;
import com.Zx1nggg.FAMS.modules.system.service.IRegistrationApplicationService;
import com.Zx1nggg.FAMS.modules.system.vo.RegistrationApplicationVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 入驻申请表 Service 实现
 * </p>
 *
 * @author Zx1nggg
 * @since 2026-06-09
 */
@Service
public class RegistrationApplicationServiceImpl
        extends ServiceImpl<RegistrationApplicationMapper, RegistrationApplication>
        implements IRegistrationApplicationService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private FarmMapper farmMapper;

    @Override
    @Transactional
    public void submitApplication(RegistrationReqDTO dto) {
        // 1. 检查手机号是否已被占用
        if (!isPhoneAvailable(dto.getPhone())) {
            throw new BusinessException(400, "该手机号已被注册或正在审核中，请更换手机号");
        }

        // 2. 构建入驻申请实体
        RegistrationApplication app = new RegistrationApplication();
        app.setPassword(passwordEncoder.encode(dto.getPassword())); // BCrypt加密存储
        app.setRealName(dto.getRealName());
        app.setPhone(dto.getPhone());
        app.setEmail(dto.getEmail());
        app.setFarmName(dto.getFarmName());
        app.setFarmProvince(dto.getFarmProvince());
        app.setFarmCity(dto.getFarmCity());
        app.setFarmAddress(dto.getFarmAddress());
        app.setApplicationReason(dto.getApplicationReason());
        app.setStatus(0); // 待审批
        app.setCreatedAt(LocalDateTime.now());
        app.setUpdatedAt(LocalDateTime.now());

        // 3. 入库
        save(app);
    }

    @Override
    public boolean isPhoneAvailable(String phone) {
        // 检查 sys_user 表
        Long userCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (userCount > 0) {
            return false;
        }
        // 检查 sys_registration_application 表（仅待审批状态，已拒绝的允许重新申请）
        Long appCount = baseMapper.selectCount(
                new LambdaQueryWrapper<RegistrationApplication>()
                        .eq(RegistrationApplication::getPhone, phone)
                        .eq(RegistrationApplication::getStatus, 0)); // 仅检查审核中的
        return appCount == 0;
    }

    @Override
    public Page<RegistrationApplicationVO> listApplications(Integer pageNum, Integer pageSize, Integer status) {
        LambdaQueryWrapper<RegistrationApplication> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(RegistrationApplication::getStatus, status);
        }
        wrapper.orderByAsc(RegistrationApplication::getStatus)  // 待审批排最前
               .orderByDesc(RegistrationApplication::getCreatedAt);

        Page<RegistrationApplication> page = page(new Page<>(pageNum, pageSize), wrapper);
        return toVOPage(page);
    }

    @Override
    public RegistrationApplicationVO getApplicationDetail(Long id) {
        RegistrationApplication app = getById(id);
        if (app == null) {
            throw new BusinessException(404, "申请记录不存在");
        }
        return toVO(app);
    }

    @Override
    @Transactional
    public Long approveApplication(Long id, Long reviewerId, ApprovalReqDTO dto) {
        RegistrationApplication app = getById(id);
        if (app == null) {
            throw new BusinessException(404, "申请记录不存在");
        }
        if (app.getStatus() != 0) {
            throw new BusinessException(400, "该申请已被处理，无需重复审批");
        }

        Integer status = dto.getStatus();

        if (status == 1) {
            // === 审批通过 ===
            // 1. 创建 sys_user 记录
            User user = new User();
            user.setPassword(app.getPassword()); // 申请时已BCrypt加密
            user.setRealName(app.getRealName());
            user.setPhone(app.getPhone());
            user.setEmail(app.getEmail());
            user.setUserType("FARMER"); // 入驻申请默认为养殖户
            user.setStatus((byte) 1);   // 正常状态
            user.setFarmId(null);       // 先不设置，等创建农场后更新
            userMapper.insert(user);

            // 2. 创建 t_farm 记录
            Farm farm = new Farm();
            farm.setUserId(user.getId());
            farm.setFarmName(app.getFarmName());
            // 注意：Farm实体目前只有 id, userId, farmName, isDeleted
            // 如果需要存储省市区地址，需要扩展Farm实体（暂时跳过，后续可扩展）
            farmMapper.insert(farm);

            // 3. 回写用户的 farmId
            user.setFarmId(farm.getId());
            userMapper.updateById(user);

            // 4. 更新申请状态为"已通过"
            app.setStatus(1);
            app.setReviewerId(reviewerId);
            app.setReviewComment(dto.getReviewComment());
            app.setReviewedAt(LocalDateTime.now());
            app.setUpdatedAt(LocalDateTime.now());
            updateById(app);

            return user.getId();

        } else if (status == 2) {
            // === 审批拒绝 ===
            if (dto.getReviewComment() == null || dto.getReviewComment().trim().isEmpty()) {
                throw new BusinessException(400, "拒绝申请时必须填写审批意见/拒绝原因");
            }

            app.setStatus(2);
            app.setReviewerId(reviewerId);
            app.setReviewComment(dto.getReviewComment());
            app.setReviewedAt(LocalDateTime.now());
            app.setUpdatedAt(LocalDateTime.now());
            updateById(app);

            return null;
        }

        throw new BusinessException(400, "无效的审批状态");
    }

    @Override
    public RegistrationApplicationVO queryStatusByPhone(String phone) {
        // 先查 sys_user 表看是否已通过并创建账号
        Long userCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (userCount > 0) {
            // 已创建用户账号，说明已通过
            RegistrationApplication app = baseMapper.selectOne(
                    new LambdaQueryWrapper<RegistrationApplication>()
                            .eq(RegistrationApplication::getPhone, phone)
                            .orderByDesc(RegistrationApplication::getCreatedAt)
                            .last("LIMIT 1"));
            if (app != null) {
                return toVO(app);
            }
            // 兼容手动创建的用户（无申请记录）
            RegistrationApplicationVO vo = new RegistrationApplicationVO();
            vo.setPhone(phone);
            vo.setStatus(1); // 已通过
            return vo;
        }

        // 查申请表
        RegistrationApplication app = baseMapper.selectOne(
                new LambdaQueryWrapper<RegistrationApplication>()
                        .eq(RegistrationApplication::getPhone, phone)
                        .orderByDesc(RegistrationApplication::getCreatedAt)
                        .last("LIMIT 1"));
        if (app == null) {
            throw new BusinessException(404, "未找到该手机号的申请记录");
        }
        return toVO(app);
    }

    // ==================== 私有转换方法 ====================

    private RegistrationApplicationVO toVO(RegistrationApplication app) {
        RegistrationApplicationVO vo = new RegistrationApplicationVO();
        BeanUtils.copyProperties(app, vo);
        return vo;
    }

    private Page<RegistrationApplicationVO> toVOPage(Page<RegistrationApplication> page) {
        Page<RegistrationApplicationVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<RegistrationApplicationVO> voList = page.getRecords().stream().map(this::toVO).toList();
        voPage.setRecords(voList);
        return voPage;
    }
}
