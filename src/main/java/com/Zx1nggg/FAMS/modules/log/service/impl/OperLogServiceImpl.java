package com.Zx1nggg.FAMS.modules.log.service.impl;

import com.Zx1nggg.FAMS.modules.log.entity.OperLog;
import com.Zx1nggg.FAMS.modules.log.mapper.OperLogMapper;
import com.Zx1nggg.FAMS.modules.log.service.IOperLogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 系统操作审计日志 服务实现类
 * </p>
 *
 * @author Zx1nggg
 * @since 2026-05-12
 */
@Service
public class OperLogServiceImpl extends ServiceImpl<OperLogMapper, OperLog> implements IOperLogService {

}
