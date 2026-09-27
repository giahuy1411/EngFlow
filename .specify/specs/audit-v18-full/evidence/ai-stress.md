# audit-v18-full — T1.6 AI stress ×N≥10 (ngắt quãng)

**Mục đích:** v17 F-17-11 (500 ngắt quãng do raw newline trong JSON) chỉ lộ ở vòng 2. Gọi lặp để tái hiện.

| # | topic | HTTP | ms | body đầu |
|---|---|---|---|---|
| 1 | technology | 200 | 4242 | [{"id":null,"word":"algorithm","pronunciation":"ˈæl.bə.ɡræm","meaning":"1. A set of rules or procedures for solving |
| 2 | environment | 200 | 2914 | [{"id":null,"word":"ecosystem","pronunciation":"e-koh-sis-tum","meaning":"生态系统","exampleSentence":"The ecosystem |
| 3 | travel | 200 | 3258 | [{"id":null,"word":"vacation","pronunciation":"vəˈkeɪʃən","meaning":"vi. to take a break from work or daily routine |
| 4 | health | 200 | 2984 | [{"id":null,"word":"exercise","pronunciation":"ɪɡˈzɪmpl","meaning":"viết tắt của exercise","exampleSentence":" |
| 5 | education | 200 | 3218 | [{"id":null,"word":"education","pronunciation":"ɪˌdəˈkeɪʃən","meaning":"the act of teaching or being taught","exa |
| 6 | business | 200 | 3056 | [{"id":null,"word":"accountant","pronunciation":"əˈkaʊntənt","meaning":"1. 会计员","exampleSentence":"She is a qu |
| 7 | science | 200 | 3224 | [{"id":null,"word":"astronomy","pronunciation":"əˈstrɒnəmɪ","meaning":"天文","exampleSentence":"She studies astro |
| 8 | culture | 200 | 3032 | [{"id":null,"word":"tradition","pronunciation":"trəˈdiːnəns","meaning":"传统","exampleSentence":"The traditional f |
| 9 | sports | 200 | 2961 | [{"id":null,"word":"tennis","pronunciation":"ˈtɛnɪs","meaning":"tennis is played with a racket and a ball","exampleSe |
| 10 | food | 200 | 3684 | [{"id":null,"word":"sandwich","pronunciation":"sɑːndɪʃ","meaning":"1. A food item consisting of two slices of bread  |

Xem log đầy đủ bên dưới. 500/504 = finding.

## enrich-word ×5
| # | word | HTTP | ms |
|---|---|---|---|
| 1 | resilient | 200 | 1450 |
| 2 | abundant | 200 | 1041 |
| 3 | candid | 200 | 980 |
| 4 | meticulous | 200 | 1025 |
| 5 | pragmatic | 200 | 1168 |

## Kết luận

**generate-vocab 10/10 = 200**, **enrich-word 5/5 = 200**. Không tái hiện 500/504 → fix F-17-11 (lenient parse) + F-17-12 (timeout 120s) vẫn hiệu lực.

## Dictionary cold/warm (H7)
| ubiquitous | cold | 200 | 19765 |
| ubiquitous | warm | 200 | 86 |
