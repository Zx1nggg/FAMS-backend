package com.Zx1nggg.FAMS;

import com.Zx1nggg.FAMS.modules.system.controller.UserController;
import com.Zx1nggg.FAMS.modules.system.service.IUserService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.*;
import java.nio.file.*;
import java.io.ByteArrayOutputStream;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserAvatarTest {
    @TempDir Path directory;
    UserController controller; IUserService users;
    @BeforeEach void setup() {
        controller = new UserController(); users = mock(IUserService.class);
        ReflectionTestUtils.setField(controller, "userService", users); ReflectionTestUtils.setField(controller, "avatarDir", directory.toString());
        var request = new MockHttpServletRequest(); request.setAttribute("currentUserId", 1L);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
    @AfterEach void clear() { RequestContextHolder.resetRequestAttributes(); }
    @Test void fakeImageIsRejectedWithoutWritingOrUpdatingProfile() throws Exception {
        var file = new MockMultipartFile("file", "image.png", "image/png", "<svg onload='alert(1)'/>".getBytes());
        assertThat(controller.uploadAvatar(file).getCode()).isEqualTo(400);
        verifyNoInteractions(users);
        try (var files = Files.list(directory)) { assertThat(files.count()).isZero(); }
    }
    @Test void realImageIsReencodedWithServerControlledName() throws Exception {
        var bytes = new ByteArrayOutputStream(); javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB), "png", bytes);
        var file = new MockMultipartFile("file", "../../image.html", "text/html", bytes.toByteArray());
        var result = controller.uploadAvatar(file); assertThat(result.getCode()).isEqualTo(200);
        assertThat(result.getData()).matches("uploads/avatar/1_[a-f0-9-]+\\.png");
        Path saved = directory.resolve(Path.of(result.getData()).getFileName());
        assertThat(javax.imageio.ImageIO.read(saved.toFile()).getWidth()).isEqualTo(2);
        verify(users).updateAvatar(1L, result.getData());
    }
}
