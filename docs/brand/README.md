# 브랜드 에셋

Workmate 심볼과 외부 업로드용 아이콘을 모아둔다.

## 심볼

라운드 스퀘어 안의 `W` 워드마크에 반짝임을 하나 얹은 형태다. 반짝임은 AI 서비스라는 걸 암시하는 요소인데, 작은 크기에서는 뭉개져 사라지므로 24px 이하에서는 빼고 쓴다.

색은 앱 테마의 `--primary`(`oklch(0.205 0 0)` ≈ `#262626`)를 그대로 쓴다. 프로젝트가 무채색 테마라 로고에만 색을 넣으면 겉돈다.

## 파일

| 파일                                       | 용도                                                          |
| :----------------------------------------- | :------------------------------------------------------------ |
| `workmate-app-icon-128.png`                | 카카오 개발자센터 앱 아이콘 (규격: 128×128 이하 · 250KB 미만) |
| `workmate-app-icon-512.png`                | 그 외 외부 등록용 여유분                                      |
| `../../workmate-vue/public/favicon.svg`    | 파비콘 (반짝임 없는 단순형)                                   |
| `../../workmate-vue/public/favicon-96.png` | 파비콘 PNG 폴백                                               |

화면 안에서 쓰는 심볼은 이미지가 아니라 `workmate-vue/src/common/components/BrandMark.vue` 가 SVG 로 직접 그린다. Tailwind 유틸로 색을 지정해 라이트/다크 테마를 따라가게 하기 위해서다.

## 다시 뽑기

PNG 는 `scripts/gen-brand-assets.js` 로 생성한다.

```bash
node scripts/gen-brand-assets.js
```
