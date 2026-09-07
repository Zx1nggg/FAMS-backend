package com.Zx1nggg.FAMS.modules.regulator.service;

import com.Zx1nggg.FAMS.modules.regulator.dto.*;
import com.Zx1nggg.FAMS.modules.regulator.entity.InspectionRecord;
import com.Zx1nggg.FAMS.modules.regulator.vo.InspectionRecordVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface IInspectionRecordService extends IService<InspectionRecord> {
    Page<InspectionRecordVO> pageQuery(int pageNum, int pageSize, Long farmId, String result, String rectifyStatus, LocalDate startDate, LocalDate endDate);
    InspectionRecordVO detail(Long id);
    InspectionRecordVO create(InspectionRecordDTO dto);
    InspectionRecordVO updateRecord(Long id, InspectionRecordDTO dto);
    void deleteRecords(List<Long> ids);
    void rectify(Long id, InspectionRectifyDTO dto);
    Map<String, Object> stats();
}
