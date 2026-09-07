package com.Zx1nggg.FAMS;

import com.Zx1nggg.FAMS.common.annotation.Log;
import com.Zx1nggg.FAMS.common.aspect.LogAspect;
import com.Zx1nggg.FAMS.common.exception.BusinessException;
import com.Zx1nggg.FAMS.modules.log.service.IOperLogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class LogAspectTest {
    @Test void auditFailureDoesNotTurnCommittedSuccessIntoRetryOrMaskBusinessError() throws Throwable {
        var service = mock(IOperLogService.class);
        when(service.save(any())).thenThrow(new IllegalStateException("audit storage unavailable"));
        var aspect = new LogAspect(); ReflectionTestUtils.setField(aspect, "operLogService", service);
        var annotation = mock(Log.class); when(annotation.title()).thenReturn("test");
        var call = mock(ProceedingJoinPoint.class); Object result = new Object();
        when(call.proceed()).thenReturn(result);
        assertThat(aspect.around(call, annotation)).isSameAs(result);
        var original = new BusinessException(409, "conflict"); when(call.proceed()).thenThrow(original);
        assertThatThrownBy(() -> aspect.around(call, annotation)).isSameAs(original);
        verify(service, times(2)).save(any());
    }
}
