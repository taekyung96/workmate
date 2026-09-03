package com.workmate.was.voice.service.impl;

import com.workmate.was.voice.dao.VoiceRecordRepository;
import com.workmate.was.voice.service.VoiceAudioStore;
import com.workmate.was.voice.service.VoiceTranscriber;
import com.workmate.was.voice.vo.VoiceRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.workmate.was.global.config.ChatClientRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VoiceServiceImplTest {

    private final VoiceTranscriber transcriber = mock(VoiceTranscriber.class);
    private final VoiceRecordRepository repository = mock(VoiceRecordRepository.class);
    private final VoiceAudioStore audioStore = mock(VoiceAudioStore.class);

    /** ChatClient 는 요약 호출에만 쓰이므로 요약을 우회하도록 오버라이드한다 */
    private VoiceServiceImpl service() {
        ChatClientRegistry registry = mock(ChatClientRegistry.class);
        when(registry.multimodal()).thenReturn(mock(ChatClient.class));
        return new VoiceServiceImpl(transcriber, repository, audioStore, registry,
                mock(com.workmate.was.usage.service.LlmUsageService.class)) {
            @Override
            protected String summarize(Long userSeq, String sttText) {
                return "### 📌 핵심 요약\n- 요약";
            }
        };
    }

    @Test
    @DisplayName("파일이 비어 있으면 분석을 거부한다")
    void analyze_emptyFile_throws() {
        VoiceServiceImpl service = service();
        MultipartFile empty = new MockMultipartFile("file", "a.m4a", "audio/mp4", new byte[0]);

        assertThat(catchIllegalArgument(() -> service.analyze(1L, "제목", empty)))
                .contains("오디오 파일을 첨부해주세요");
    }

    @Test
    @DisplayName("저장 시 오디오 파일명·원본명·크기·타입이 함께 기록된다")
    void analyze_persistsAudioMetadata() {
        VoiceServiceImpl service = service();
        MockMultipartFile file = new MockMultipartFile(
                "file", "2026 회의.m4a", "audio/mp4", "bytes".getBytes());
        when(transcriber.transcribe(any(), any(), any())).thenReturn("전사된 텍스트");
        when(audioStore.store(file)).thenReturn("uuid-1.m4a");
        when(repository.save(any(VoiceRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        service.analyze(7L, "아키텍처 회의", file);

        ArgumentCaptor<VoiceRecord> captor = ArgumentCaptor.forClass(VoiceRecord.class);
        org.mockito.Mockito.verify(repository).save(captor.capture());
        VoiceRecord saved = captor.getValue();
        assertThat(saved.getUserSeq()).isEqualTo(7L);
        assertThat(saved.getTitle()).isEqualTo("아키텍처 회의");
        assertThat(saved.getAudioFileName()).isEqualTo("uuid-1.m4a");
        assertThat(saved.getOriginFileName()).isEqualTo("2026 회의.m4a");
        assertThat(saved.getFileSize()).isEqualTo("bytes".length());
        assertThat(saved.getContentType()).isEqualTo("audio/mp4");
    }

    @Test
    @DisplayName("이력 목록은 사용자 소유분만 최신순으로 요약 VO 로 반환한다")
    void getHistory_returnsSummaries() {
        VoiceServiceImpl service = service();
        when(repository.findByUserSeqOrderByCreatedAtDesc(7L)).thenReturn(java.util.List.of(
                VoiceRecord.builder().userSeq(7L).title("회의A").sttText("s").summaryMd("m")
                        .audioFileName("uuid-a.m4a").originFileName("a.m4a").fileSize(100L).build(),
                VoiceRecord.builder().userSeq(7L).title("회의B").sttText("s").summaryMd("m").build()));

        var history = service.getHistory(7L);

        assertThat(history).hasSize(2);
        assertThat(history.get(0).getTitle()).isEqualTo("회의A");
        assertThat(history.get(0).isHasAudio()).isTrue();
        assertThat(history.get(1).isHasAudio()).isFalse();
    }

    @Test
    @DisplayName("타인의 회의록 상세 조회는 거부된다")
    void getRecord_otherUser_throws() {
        VoiceServiceImpl service = service();
        when(repository.findById(5L)).thenReturn(java.util.Optional.of(
                VoiceRecord.builder().userSeq(99L).title("남의 회의").sttText("s").summaryMd("m").build()));

        assertThat(catchIllegalArgument(() -> service.getRecord(7L, 5L)))
                .contains("본인의 회의록만");
    }

    @Test
    @DisplayName("존재하지 않는 회의록 조회는 거부된다")
    void getRecord_notFound_throws() {
        VoiceServiceImpl service = service();
        when(repository.findById(404L)).thenReturn(java.util.Optional.empty());

        assertThat(catchIllegalArgument(() -> service.getRecord(7L, 404L)))
                .contains("존재하지 않는 회의록");
    }

    @Test
    @DisplayName("오디오가 없는 회의록의 재생 요청은 거부된다")
    void getAudio_noAudio_throws() {
        VoiceServiceImpl service = service();
        when(repository.findById(5L)).thenReturn(java.util.Optional.of(
                VoiceRecord.builder().userSeq(7L).title("옛 회의").sttText("s").summaryMd("m").build()));

        assertThat(catchIllegalArgument(() -> service.getAudio(7L, 5L)))
                .contains("오디오가 없습니다");
    }

    @Test
    @DisplayName("파일이 사라진 회의록의 재생 요청은 거부된다")
    void getAudio_fileMissing_throws() {
        VoiceServiceImpl service = service();
        when(repository.findById(5L)).thenReturn(java.util.Optional.of(
                VoiceRecord.builder().userSeq(7L).title("회의").sttText("s").summaryMd("m")
                        .audioFileName("gone.m4a").contentType("audio/mp4").build()));
        when(audioStore.load("gone.m4a")).thenReturn(java.util.Optional.empty());

        assertThat(catchIllegalArgument(() -> service.getAudio(7L, 5L)))
                .contains("오디오 파일을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("삭제 시 DB 행과 오디오 파일을 함께 지운다")
    void deleteRecord_removesRowAndFile() {
        VoiceServiceImpl service = service();
        VoiceRecord record = VoiceRecord.builder().userSeq(7L).title("회의").sttText("s")
                .summaryMd("m").audioFileName("uuid-x.m4a").build();
        when(repository.findById(5L)).thenReturn(java.util.Optional.of(record));
        when(audioStore.delete("uuid-x.m4a")).thenReturn(true);

        service.deleteRecord(7L, 5L);

        org.mockito.Mockito.verify(audioStore).delete("uuid-x.m4a");
        org.mockito.Mockito.verify(repository).delete(record);
    }

    @Test
    @DisplayName("오디오가 없는 과거 회의록도 예외 없이 삭제된다")
    void deleteRecord_noAudio_deletesRowOnly() {
        VoiceServiceImpl service = service();
        VoiceRecord record = VoiceRecord.builder().userSeq(7L).title("옛 회의").sttText("s")
                .summaryMd("m").build();
        when(repository.findById(5L)).thenReturn(java.util.Optional.of(record));

        service.deleteRecord(7L, 5L);

        org.mockito.Mockito.verify(audioStore, org.mockito.Mockito.never()).delete(any());
        org.mockito.Mockito.verify(repository).delete(record);
    }

    @Test
    @DisplayName("타인의 회의록 삭제는 거부된다")
    void deleteRecord_otherUser_throws() {
        VoiceServiceImpl service = service();
        when(repository.findById(5L)).thenReturn(java.util.Optional.of(
                VoiceRecord.builder().userSeq(99L).title("남의 회의").sttText("s").summaryMd("m").build()));

        assertThat(catchIllegalArgument(() -> service.deleteRecord(7L, 5L)))
                .contains("본인의 회의록만");
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never()).delete(any());
    }

    /** 예외 메시지만 꺼내는 보조 — assertThatThrownBy 체인을 짧게 쓰기 위함 */
    private String catchIllegalArgument(Runnable action) {
        try {
            action.run();
            return "";
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }
}
