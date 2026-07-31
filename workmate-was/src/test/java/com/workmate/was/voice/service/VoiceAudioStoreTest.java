package com.workmate.was.voice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VoiceAudioStoreTest {

    @TempDir
    Path tempDir;

    private VoiceAudioStore store() {
        return new VoiceAudioStore(tempDir.toString());
    }

    @Test
    @DisplayName("원본 파일명에서 확장자를 소문자로 추출한다")
    void extensionOf_extractsLowercase() {
        assertThat(VoiceAudioStore.extensionOf("회의록.M4A")).isEqualTo(".m4a");
        assertThat(VoiceAudioStore.extensionOf("a.b.webm")).isEqualTo(".webm");
    }

    @Test
    @DisplayName("확장자가 없거나 파일명이 없으면 빈 문자열을 반환한다")
    void extensionOf_noExtension_returnsEmpty() {
        assertThat(VoiceAudioStore.extensionOf("확장자없음")).isEmpty();
        assertThat(VoiceAudioStore.extensionOf(null)).isEmpty();
    }

    @Test
    @DisplayName("저장한 파일을 파일명으로 다시 읽고 삭제할 수 있다")
    void store_load_delete_roundTrip() throws IOException {
        VoiceAudioStore store = store();
        MockMultipartFile file = new MockMultipartFile(
                "file", "회의.m4a", "audio/mp4", "audio-bytes".getBytes());

        String saved = store.store(file);

        assertThat(saved).endsWith(".m4a");
        Optional<Resource> loaded = store.load(saved);
        assertThat(loaded).isPresent();
        assertThat(loaded.get().contentLength()).isEqualTo("audio-bytes".length());

        assertThat(store.delete(saved)).isTrue();
        assertThat(store.load(saved)).isEmpty();
    }

    @Test
    @DisplayName("없는 파일 조회는 빈 Optional, 없는 파일 삭제는 false 를 반환한다")
    void load_delete_missingFile() {
        VoiceAudioStore store = store();
        assertThat(store.load("없는파일.m4a")).isEmpty();
        assertThat(store.delete("없는파일.m4a")).isFalse();
    }

    @Test
    @DisplayName("파일명에 경로 구분자나 상위 경로가 섞이면 거부한다 (경로 탈출 차단)")
    void load_pathTraversal_rejected() {
        VoiceAudioStore store = store();
        assertThatThrownBy(() -> store.load("../../etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> store.delete("sub/dir.m4a"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
