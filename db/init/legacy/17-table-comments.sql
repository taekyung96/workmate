-- =============================================================
-- 기존 테이블·컬럼 코멘트 일괄 부여
--
-- 프로젝트 규칙(CLAUDE.md): 모든 테이블·컬럼에 COMMENT 를 단다.
-- 16번까지의 테이블은 코멘트 없이 만들어졌으므로 여기서 한 번에 채운다.
-- 17번 이후 새로 만드는 테이블은 생성 SQL 안에 COMMENT 를 함께 적는다.
--
-- COMMENT ON 은 덮어쓰기라 여러 번 실행해도 안전하다(멱등).
-- 기존 볼륨에 수동 적용:
--   docker exec -i workmate-db psql -U workmate -d workmate_db < db/init/17-table-comments.sql
-- =============================================================

-- ── 사용자 계정 (테이블명은 app_user 지만 관리자 전용이 아니라 전체 사용자다. 구분은 role) ──
COMMENT ON TABLE  app_user                  IS '사용자 계정 — 일반/관리자를 role 로 구분한다. 이메일·전화번호는 AES-256 암호화 저장';
COMMENT ON COLUMN app_user.user_seq         IS '사용자 식별자 (PK)';
COMMENT ON COLUMN app_user.email            IS '로그인 이메일 — AES-256 결정적 암호화(고정 IV). 같은 평문이 같은 암호문이라 UK·검색이 가능해 512자';
COMMENT ON COLUMN app_user.password         IS '비밀번호 — BCrypt 단방향 해시(60자). 소셜 전용 계정은 임의값';
COMMENT ON COLUMN app_user.user_name        IS '표시 이름';
COMMENT ON COLUMN app_user.phone            IS '전화번호 — AES-256 결정적 암호화(고정 IV)';
COMMENT ON COLUMN app_user.role             IS '권한 — ROLE_USER | ROLE_ADMIN';
COMMENT ON COLUMN app_user.login_fail_count IS '연속 로그인 실패 횟수 (F1-06) — 임계치 도달 시 locked_at 설정';
COMMENT ON COLUMN app_user.locked_at        IS '계정 잠금 시각 (F1-06) — 설정 후 lock-minutes 동안 로그인 차단. NULL 이면 잠금 아님';
COMMENT ON COLUMN app_user.use_yn           IS '사용 여부 — false 는 논리 삭제(행을 지우지 않는다)';
COMMENT ON COLUMN app_user.created_at       IS '가입 일시';

-- ── 소셜 로그인 연동 (F1-1) ──
COMMENT ON TABLE  user_social_account                  IS '소셜 로그인 연동 정보 (F1-1) — 한 계정에 여러 제공자를 연결할 수 있다';
COMMENT ON COLUMN user_social_account.social_seq       IS '연동 식별자 (PK)';
COMMENT ON COLUMN user_social_account.user_seq         IS '연결된 사용자 식별자 (app_user.user_seq)';
COMMENT ON COLUMN user_social_account.provider         IS '제공자 — naver | kakao | google';
COMMENT ON COLUMN user_social_account.provider_user_id IS '제공자가 발급한 고유 사용자 ID — 제공자+ID 조합으로 계정을 찾는다';
COMMENT ON COLUMN user_social_account.created_at       IS '연동 일시';

-- ── 관리자 감사 로그 (F6) ──
COMMENT ON TABLE  admin_audit_log                 IS '관리자 행위 감사 로그 (F6) — append-only, UPDATE/DELETE 금지';
COMMENT ON COLUMN admin_audit_log.audit_seq       IS '감사 로그 식별자 (PK)';
COMMENT ON COLUMN admin_audit_log.admin_user_seq  IS '행위를 수행한 관리자 식별자';
COMMENT ON COLUMN admin_audit_log.target_user_seq IS '대상 사용자 식별자';
COMMENT ON COLUMN admin_audit_log.action          IS '수행 행위 — UNLOCK(잠금해제) | RESET_PASSWORD(비밀번호 초기화)';
COMMENT ON COLUMN admin_audit_log.created_at      IS '수행 일시';

-- ── 채팅 (F2) ──
COMMENT ON TABLE  chat_room            IS '채팅방 (F2) — 사용자별 대화 묶음';
COMMENT ON COLUMN chat_room.room_seq   IS '채팅방 식별자 (PK)';
COMMENT ON COLUMN chat_room.user_seq   IS '방 소유자 식별자 (app_user.user_seq)';
COMMENT ON COLUMN chat_room.title      IS '방 제목 — 첫 질문에서 자동 생성 (F2-02)';
COMMENT ON COLUMN chat_room.use_yn     IS '사용 여부 — false 는 논리 삭제 (C4)';
COMMENT ON COLUMN chat_room.created_at IS '생성 일시';

COMMENT ON TABLE  chat_message              IS '채팅 메시지 (F2) — 사용자 질문과 AI 응답을 시간순으로 보관';
COMMENT ON COLUMN chat_message.message_seq  IS '메시지 식별자 (PK)';
COMMENT ON COLUMN chat_message.room_seq     IS '소속 채팅방 식별자 (chat_room.room_seq)';
COMMENT ON COLUMN chat_message.role         IS '발화 주체 — user | assistant';
COMMENT ON COLUMN chat_message.content      IS '메시지 본문 (마크다운)';
COMMENT ON COLUMN chat_message.model_name   IS '응답 생성에 쓰인 모델명 (assistant 메시지에만)';
COMMENT ON COLUMN chat_message.sources      IS 'RAG 출처 목록 JSON (F4-07) — [{guideSeq, title}]. 대화 이력 재조회 시 출처를 복원한다';
COMMENT ON COLUMN chat_message.created_at   IS '생성 일시';

-- ── 영수증 (F3) ──
COMMENT ON TABLE  receipt              IS '영수증 인식 결과 (F3) — 이미지를 멀티모달로 분석해 추출한 결제 내역';
COMMENT ON COLUMN receipt.receipt_seq  IS '영수증 식별자 (PK)';
COMMENT ON COLUMN receipt.user_seq     IS '소유자 식별자 (app_user.user_seq)';
COMMENT ON COLUMN receipt.image_path   IS '이미지 저장 경로 — 저장 루트는 설정값(app.upload.receipt-dir) 기준 상대경로';
COMMENT ON COLUMN receipt.pay_amount   IS '결제 금액 (원)';
COMMENT ON COLUMN receipt.biz_no       IS '사업자등록번호 — 하이픈 없는 10자리';
COMMENT ON COLUMN receipt.pay_date     IS '결제일 — YYYYMMDD 8자리 문자열';
COMMENT ON COLUMN receipt.card_name    IS '카드사명';
COMMENT ON COLUMN receipt.biz_no_valid IS '사업자등록번호 체크섬 검증 통과 여부';
COMMENT ON COLUMN receipt.select_type  IS '입력 방식 — AUTO(AI 추출) | MANUAL(사용자 수기 입력)';
COMMENT ON COLUMN receipt.raw_json     IS 'AI 원본 추출 결과 JSON — 재분석·디버깅용';
COMMENT ON COLUMN receipt.created_at   IS '저장 일시';

-- ── 사내 가이드 · RAG (F4) ──
COMMENT ON TABLE  guide            IS '사내 가이드 문서 (F4) — RAG 검색의 근거 자료. 본문이 청크로 분할돼 vector_store 에 임베딩된다';
COMMENT ON COLUMN guide.guide_seq  IS '가이드 문서 식별자 (PK)';
COMMENT ON COLUMN guide.user_seq   IS '작성자 식별자 (app_user.user_seq)';
COMMENT ON COLUMN guide.title      IS '문서 제목 — 청크 메타데이터로도 복사된다(임베딩 벡터에는 미반영)';
COMMENT ON COLUMN guide.content    IS '문서 본문 (마크다운) — 이 값이 바뀔 때만 재임베딩한다';
COMMENT ON COLUMN guide.is_public  IS '공개 여부 — true 면 전체 공개, false 면 작성자만. RAG 접근 필터에 쓰인다 (F4-08)';
COMMENT ON COLUMN guide.created_at IS '생성 일시';
COMMENT ON COLUMN guide.updated_at IS '최종 수정 일시';

COMMENT ON TABLE  vector_store           IS 'Spring AI PgVectorStore 표준 스키마 — 가이드 청크 임베딩 (initialize-schema:false 라 직접 관리)';
COMMENT ON COLUMN vector_store.id        IS '청크 식별자 (PK, UUID)';
COMMENT ON COLUMN vector_store.content   IS '청크 본문 — 임베딩 벡터의 원천 텍스트';
COMMENT ON COLUMN vector_store.metadata  IS '청크 메타데이터 JSONB — {guideSeq, userSeq, title, isPublic}. 접근 필터(F4-08)와 본문 무변경 수정의 메타데이터 갱신에 쓰인다';
COMMENT ON COLUMN vector_store.embedding IS '임베딩 벡터 768차원 — 모델 출력 차원과 반드시 일치해야 한다';

-- ── 음성 회의록 (F8-1) ──
COMMENT ON TABLE  voice_record                  IS '음성 회의록 (F8-1) — 오디오 전사문과 AI 요약, 원본 오디오 메타데이터';
COMMENT ON COLUMN voice_record.record_seq       IS '회의록 식별자 (PK)';
COMMENT ON COLUMN voice_record.user_seq         IS '소유자 식별자 (app_user.user_seq)';
COMMENT ON COLUMN voice_record.title            IS '회의록 제목 — 미입력 시 자동 생성';
COMMENT ON COLUMN voice_record.stt_text         IS '음성 전사 원문 (STT 결과)';
COMMENT ON COLUMN voice_record.summary_md       IS 'AI 구조화 요약 (마크다운) — 핵심 요약/결정 사항/Action Items 3단';
COMMENT ON COLUMN voice_record.audio_file_name  IS '저장된 오디오 파일명 — 저장 루트는 설정값(app.upload.voice-dir)';
COMMENT ON COLUMN voice_record.origin_file_name IS '업로드 당시 원본 파일명 (표시용)';
COMMENT ON COLUMN voice_record.file_size        IS '오디오 파일 크기 (바이트)';
COMMENT ON COLUMN voice_record.content_type     IS '오디오 MIME 타입 — 재생 시 Content-Type 으로 사용';
COMMENT ON COLUMN voice_record.created_at       IS '생성 일시';

-- ── 공통 코드 (F9) ──
COMMENT ON TABLE  common_code_group             IS '공통 코드 그룹 (F9) — 코드 집합의 상위 분류';
COMMENT ON COLUMN common_code_group.group_code  IS '그룹 코드 (PK) — 예: AI_MODEL';
COMMENT ON COLUMN common_code_group.group_name  IS '그룹 표시명';
COMMENT ON COLUMN common_code_group.description IS '그룹 설명';
COMMENT ON COLUMN common_code_group.use_yn      IS '사용 여부 — false 면 조회 대상에서 제외';

COMMENT ON TABLE  common_code            IS '공통 코드 (F9) — AI_MODEL 그룹은 요청 가능한 모델 화이트리스트로 쓰인다 (F9-04)';
COMMENT ON COLUMN common_code.group_code IS '소속 그룹 코드 (common_code_group.group_code)';
COMMENT ON COLUMN common_code.code       IS '코드 값 (그룹 내 고유) — AI_MODEL 그룹에서는 모델명';
COMMENT ON COLUMN common_code.code_name  IS '코드 표시명';
COMMENT ON COLUMN common_code.sort_order IS '화면 정렬 순서';
COMMENT ON COLUMN common_code.use_yn     IS '사용 여부 — false 면 선택 불가';
