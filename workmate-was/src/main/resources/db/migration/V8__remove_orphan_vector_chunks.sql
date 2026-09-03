-- 원본 문서가 사라진 임베딩(고아 청크)을 정리한다.
--
-- [증상]
-- 채팅 답변의 RAG 출처 뱃지에 같은 제목이 두 번 나왔다. 확인해 보니 guide 테이블에는 없는
-- guideSeq(45·46)의 청크가 vector_store 에 남아 있었고, 살아 있는 문서(43·44)와 제목이 같았다.
-- 서버의 출처 중복 제거는 guideSeq 기준이라(ChatServiceImpl.distinctSources) 같은 제목이어도
-- seq 가 다르면 둘 다 통과한다.
--
-- [왜 고쳐야 하나 — 보이는 것보다 나쁘다]
--   1. 그 뱃지를 누르면 없는 문서로 간다 (GET /api/v1/guides/45 → "찾을 수 없습니다")
--   2. RAG 가 같은 내용을 두 번 회수한다 — topK 자리를 하나 낭비하고, 중복된 본문이 프롬프트에
--      실려 입력 토큰을 그만큼 더 쓴다
--
-- [왜 코드가 아니라 데이터 문제인가]
-- GuideServiceImpl 은 문서를 수정·삭제할 때 deleteEmbeddings(guideSeq) 로 청크를 지운다.
-- 즉 지금 코드로는 새로 생기지 않는다. 남아 있는 것은 과거 시드/재시드 과정에서 문서만 갈리고
-- 벡터가 남은 흔적이라, 한 번 걷어내면 된다. 환경마다 손으로 지우지 않도록 마이그레이션으로 남긴다.
--
-- metadata->>'guideSeq' 는 텍스트라 bigint 로 캐스팅해 비교한다.
-- 값이 없거나 숫자가 아닌 행이 섞이면 캐스팅에서 실패하므로, 그런 행은 대상에서 제외한다.
DELETE FROM vector_store
WHERE metadata ->> 'guideSeq' ~ '^[0-9]+$'
  AND NOT EXISTS (
      SELECT 1
      FROM guide g
      WHERE g.guide_seq = (vector_store.metadata ->> 'guideSeq')::bigint
  );
