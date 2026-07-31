package com.workmate.was.voice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * 회의 오디오 파일 저장소 (F8-1 확장).
 *
 * <p>DB 에는 파일명만 남기고 저장 루트는 설정값(app.upload.voice-dir)으로 둔다.
 * 영수증(ReceiptServiceImpl)은 절대경로를 DB 에 넣어 PC 이식·배포 시 경로가 깨지는데,
 * 그 방식을 반복하지 않기 위한 분리다.</p>
 */
@Slf4j
@Component
public class VoiceAudioStore {

    private final Path rootDir;

    public VoiceAudioStore(@Value("${app.upload.voice-dir}") String voiceDir) {
        this.rootDir = Paths.get(voiceDir).toAbsolutePath().normalize();
    }

    /**
     * 업로드된 오디오를 저장하고 생성된 파일명을 반환한다.
     *
     * @param file 업로드 오디오
     * @return 저장된 파일명 ({UUID}.확장자)
     */
    public String store(MultipartFile file) {
        try {
            Files.createDirectories(rootDir);
            String fileName = UUID.randomUUID() + extensionOf(file.getOriginalFilename());
            Path target = rootDir.resolve(fileName);
            file.transferTo(target.toFile());
            log.info("회의 오디오 저장 완료 - 파일: {}, 크기: {} bytes", fileName, file.getSize());
            return fileName;
        } catch (IOException e) {
            log.error("회의 오디오 저장 실패 - 원본: {}", file.getOriginalFilename(), e);
            throw new IllegalStateException("오디오 파일을 저장하지 못했습니다.", e);
        }
    }

    /**
     * 파일명으로 오디오 리소스를 찾는다.
     *
     * @param fileName 저장된 파일명
     * @return 리소스. 파일이 없으면 빈 Optional
     */
    public Optional<Resource> load(String fileName) {
        Path target = resolveSafely(fileName);
        if (!Files.isReadable(target)) {
            log.warn("회의 오디오 파일 없음 - 파일: {}", fileName);
            return Optional.empty();
        }
        return Optional.of(new FileSystemResource(target));
    }

    /**
     * 오디오 파일을 삭제한다.
     *
     * @param fileName 저장된 파일명
     * @return 실제로 삭제했으면 true, 파일이 없었으면 false
     */
    public boolean delete(String fileName) {
        Path target = resolveSafely(fileName);
        try {
            boolean deleted = Files.deleteIfExists(target);
            if (!deleted) {
                log.warn("삭제할 회의 오디오 파일이 이미 없음 - 파일: {}", fileName);
            }
            return deleted;
        } catch (IOException e) {
            log.error("회의 오디오 삭제 실패 - 파일: {}", fileName, e);
            throw new IllegalStateException("오디오 파일을 삭제하지 못했습니다.", e);
        }
    }

    /**
     * 원본 파일명에서 확장자를 소문자로 뽑는다.
     *
     * @param originalFilename 업로드 원본 파일명 (null 허용)
     * @return {@code .m4a} 형태. 확장자가 없으면 빈 문자열
     */
    static String extensionOf(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            return "";
        }
        return originalFilename.substring(dot).toLowerCase(Locale.ROOT);
    }

    /**
     * 파일명을 저장 루트 기준으로 안전하게 해석한다.
     * 경로 구분자·상위 경로가 섞인 입력은 저장 루트를 벗어날 수 있어 거부한다.
     *
     * @param fileName 저장된 파일명 (경로 없이 순수 파일명)
     * @return 해석된 절대 경로
     */
    private Path resolveSafely(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("오디오 파일명이 비어 있습니다.");
        }
        Path resolved = rootDir.resolve(fileName).normalize();
        if (!resolved.getParent().equals(rootDir)) {
            log.warn("허용되지 않는 오디오 파일 경로 접근 - 입력: {}", fileName);
            throw new IllegalArgumentException("잘못된 오디오 파일명입니다.");
        }
        return resolved;
    }
}
