const https = require('https');
const { JSDOM } = require('jsdom');
const fs = require('fs');

const BASE = 'https://www.tienganh123.com';
const OUTPUT = 'tienganh_nangcao_lessons.json';
const DELAY_MS = 1500;

function sleep(ms) {
  return new Promise(r => setTimeout(r, ms));
}

function fetch(url) {
  return new Promise((resolve, reject) => {
    https.get(url, { headers: { 'User-Agent': 'Mozilla/5.0' } }, res => {
      let data = '';
      res.on('data', c => data += c);
      res.on('end', () => resolve({ status: res.statusCode, body: data }));
    }).on('error', reject);
  });
}

function extractNoidung(html) {
  try {
    const dom = new JSDOM(html);
    const doc = dom.window.document;
    const noidung = doc.querySelector('.noidung, #noidung, div[class*="noidung"], div[id*="noidung"], .content-detail, .entry-content, article, .post-content, .lesson-content');
    if (noidung) {
      return noidung.innerHTML.trim();
    }
    // Fallback: get main content
    const main = doc.querySelector('main, .main-content, .content, #content');
    if (main) return main.innerHTML.trim();
    return null;
  } catch (e) {
    return null;
  }
}

function extractTitle(html) {
  try {
    const dom = new JSDOM(html);
    const title = dom.window.document.querySelector('h1, .title, .post-title, .entry-title');
    return title ? title.textContent.trim() : null;
  } catch (e) {
    return null;
  }
}

// All lesson URLs identified from unit pages
const LESSONS = [
  // Unit 1: Science and Technology
  { unit: 1, unitTitle: 'Science and Technology', skill: 'vocabulary', url: '/unit1-1-science-and-technology/10669-unit1-1-science-and-technology-vocabulary.html' },
  { unit: 1, unitTitle: 'Science and Technology', skill: 'grammar', url: '/unit1-1-science-and-technology/10670-unit1-science-and-technology-grammar.html' },
  { unit: 1, unitTitle: 'Science and Technology', skill: 'listening', url: '/unit1-1-science-and-technology/10671-unit1-science-and-technology-listening.html' },
  { unit: 1, unitTitle: 'Science and Technology', skill: 'reading', url: '/unit1-1-science-and-technology/10672-unit1-science-and-technology-reading.html' },
  { unit: 1, unitTitle: 'Science and Technology', skill: 'writing', url: '/unit1-1-science-and-technology/10675-unit1-1-science-and-technology-writing.html' },
  { unit: 1, unitTitle: 'Science and Technology', skill: 'speaking', url: '/unit1-1-science-and-technology/10698-unit1-science-and-technology-speaking.html' },

  // Unit 2: House Problems
  { unit: 2, unitTitle: 'House Problems', skill: 'vocabulary', url: '/unit2-2-house-problems/10775-unit2-2-house-problems.html' },
  { unit: 2, unitTitle: 'House Problems', skill: 'grammar', url: '/unit2-2-house-problems/10794-unit2-2-house-problems-grammar.html' },
  { unit: 2, unitTitle: 'House Problems', skill: 'listening', url: '/unit2-2-house-problems/10801-unit2-2-house-problems-listening.html' },
  { unit: 2, unitTitle: 'House Problems', skill: 'reading', url: '/unit2-2-house-problems/10802-unit2-2-house-problems-reading.html' },

  // Unit 3: Travel and Transportation
  { unit: 3, unitTitle: 'Travel and Transportation', skill: 'vocabulary', url: '/unit3-3-travel-and-transportation/10895-unit3-3-travel-and-transportation.html' },
  { unit: 3, unitTitle: 'Travel and Transportation', skill: 'grammar', url: '/unit3-3-travel-and-transportation/10911-unit3-3-travel-and-transportation-grammar.html' },
  { unit: 3, unitTitle: 'Travel and Transportation', skill: 'listening', url: '/unit3-3-travel-and-transportation/10924-unit3-3-travel-and-transportation-listening.html' },
  { unit: 3, unitTitle: 'Travel and Transportation', skill: 'reading', url: '/unit3-3-travel-and-transportation/10928-unit3-3-travel-and-transportation-reading.html' },

  // Unit 4: Fashion Style
  { unit: 4, unitTitle: 'Fashion Style', skill: 'vocabulary', url: '/unit4-4-fashion-style/10960-unit4-4-fashion-style-tu-vung-ve-phong-cach-thoi-trang.html' },
  { unit: 4, unitTitle: 'Fashion Style', skill: 'grammar', url: '/unit4-4-fashion-style/10994-unit4-4-prepositions-of-location-and-direction-gioi-tu-chi-noi-chon-va-phuong-huong.html' },
  { unit: 4, unitTitle: 'Fashion Style', skill: 'listening', url: '/unit4-4-fashion-style/10995-unit4-4-fashion-style-listening.html' },
  { unit: 4, unitTitle: 'Fashion Style', skill: 'reading', url: '/unit4-4-fashion-style/10999-unit4-4-fashion-style-reading.html' },

  // Unit 5: Tastes and Senses
  { unit: 5, unitTitle: 'Tastes and Senses', skill: 'vocabulary', url: '/unit5-5-tastes-and-senses/11225-unit5-5-tastes-and-senses-tu-vung-ve-vi-giac-va-cam-giac.html' },
  { unit: 5, unitTitle: 'Tastes and Senses', skill: 'grammar', url: '/unit5-5-tastes-and-senses/11227-unit5-5-conjunctions-cac-lien-tu.html' },
  { unit: 5, unitTitle: 'Tastes and Senses', skill: 'listening', url: '/unit5-5-tastes-and-senses/11228-unit5-5-tastes-and-senses-listening.html' },
  { unit: 5, unitTitle: 'Tastes and Senses', skill: 'reading', url: '/unit5-5-tastes-and-senses/11229-unit5-5-tastes-and-senses-reading.html' },

  // Unit 6: Romance and Marriage
  { unit: 6, unitTitle: 'Romance and Marriage', skill: 'vocabulary', url: '/unit6-6-romance-and-marriage/11249-unit6-6-romance-and-marriage-tu-vung-ve-su-lang-man-va-hon-nhan.html' },
  { unit: 6, unitTitle: 'Romance and Marriage', skill: 'grammar', url: '/unit6-6-romance-and-marriage/11250-unit6-6-gerund-and-infinitive-danh-dong-tu-va-dong-tu-nguyen-the.html' },
  { unit: 6, unitTitle: 'Romance and Marriage', skill: 'listening', url: '/unit6-6-romance-and-marriage/11252-unit6-6-romance-and-marriage.html' },
  { unit: 6, unitTitle: 'Romance and Marriage', skill: 'reading', url: '/unit6-6-romance-and-marriage/11253-unit6-6-romance-and-marriage-reading.html' },

  // Unit 7: Arts and Entertainment
  { unit: 7, unitTitle: 'Arts and Entertainment', skill: 'vocabulary', url: '/unit7-7-arts-and-entertainment/11254-unit7-7-arts-and-entertainment-tu-vung-ve-nghe-thuat-va-giai-tri.html' },
  { unit: 7, unitTitle: 'Arts and Entertainment', skill: 'grammar', url: '/unit7-7-arts-and-entertainment/11255-unit7-7-gerund-and-infinitive.html' },
  { unit: 7, unitTitle: 'Arts and Entertainment', skill: 'listening', url: '/unit7-7-arts-and-entertainment/11256-unit7-7-arts-and-entertainment-listening.html' },
  { unit: 7, unitTitle: 'Arts and Entertainment', skill: 'reading', url: '/unit7-7-arts-and-entertainment/11257-unit7-7-arts-and-entertainment-reading.html' },

  // Unit 8: Workplace Safety
  { unit: 8, unitTitle: 'Workplace Safety', skill: 'vocabulary', url: '/unit8-8-workplace-safety/11258-unit8-8-workplace-safety-tu-vung-ve-chu-de-an-toan-lao-dong.html' },
  { unit: 8, unitTitle: 'Workplace Safety', skill: 'grammar', url: '/unit8-8-workplace-safety/11259-unit8-8-word-endings-cac-duoi-ket-thuc-tu-pho-bien.html' },
  { unit: 8, unitTitle: 'Workplace Safety', skill: 'listening', url: '/unit8-8-workplace-safety/11260-unit8-8-workplace-safety-listening.html' },
  { unit: 8, unitTitle: 'Workplace Safety', skill: 'reading', url: '/unit8-8-workplace-safety/11261-unit8-8-workplace-safety-reading.html' },

  // Unit 9: Culture Diversity
  { unit: 9, unitTitle: 'Culture Diversity', skill: 'vocabulary', url: '/unit9-9-culture-diversity/11266-unit9-9-culture-diversity-tu-vung-chu-de-da-dang-van-hoa.html' },
  { unit: 9, unitTitle: 'Culture Diversity', skill: 'grammar', url: '/unit9-9-culture-diversity/11267-unit9-9-prefix-and-suffix-tien-to-va-hau-to.html' },
  { unit: 9, unitTitle: 'Culture Diversity', skill: 'listening', url: '/unit9-9-culture-diversity/11268-unit9-9-culture-diversity-listening.html' },
  { unit: 9, unitTitle: 'Culture Diversity', skill: 'reading', url: '/unit9-9-culture-diversity/11269-unit9-9-culture-diversity-reading.html' },

  // Unit 10: Money Matters
  { unit: 10, unitTitle: 'Money Matters', skill: 'vocabulary', url: '/unit10-10-money-matters/11270-unit10-10-money-matters-tu-vung-ve-van-de-tai-chinh-tien-bac.html' },
  { unit: 10, unitTitle: 'Money Matters', skill: 'grammar', url: '/unit10-10-money-matters/11271-unit10-10-phrasal-verbs-cum-dong-tu.html' },
  { unit: 10, unitTitle: 'Money Matters', skill: 'listening', url: '/unit10-10-money-matters/11273-unit10-10-money-matters-listening.html' },
  { unit: 10, unitTitle: 'Money Matters', skill: 'reading', url: '/unit10-10-money-matters/11274-unit10-10-money-matters-reading.html' },

  // Unit 11: Health Care
  { unit: 11, unitTitle: 'Health Care', skill: 'vocabulary', url: '/unit11-11-health-care/11275-unit11-11-health-care-tu-vung-ve-chu-de-cham-soc-suc-khoe.html' },
  { unit: 11, unitTitle: 'Health Care', skill: 'grammar', url: '/unit11-11-health-care/11276-unit11-11-inversion-cau-dao-ngu.html' },
  { unit: 11, unitTitle: 'Health Care', skill: 'listening', url: '/unit11-11-health-care/11277-unit11-11-health-care-listening.html' },
  { unit: 11, unitTitle: 'Health Care', skill: 'reading', url: '/unit11-11-health-care/11278-unit11-11-health-care-reading.html' },

  // Unit 12: Social Issues
  { unit: 12, unitTitle: 'Social Issues', skill: 'vocabulary', url: '/unit12-12-social-issues/11279-unit12-12-socail-issues-tu-vung-ve-cac-van-de-xa-hoi.html' },
  { unit: 12, unitTitle: 'Social Issues', skill: 'grammar', url: '/unit12-12-social-issues/11280-unit12-12-the-subjunctive-mood-thuc-gia-dinh.html' },
  { unit: 12, unitTitle: 'Social Issues', skill: 'listening', url: '/unit12-12-social-issues/11281-unit12-12-socail-issues-listening.html' },
  { unit: 12, unitTitle: 'Social Issues', skill: 'reading', url: '/unit12-12-social-issues/11282-unit12-12-social-issues-reading.html' },

  // Review Test 1
  { unit: 'review1', unitTitle: 'Review Test 1', skill: 'instruction', url: '/review-test-1/11030-review-test-1-instruction-huong-dan-lam-bai-kiem-tra-on-tap-1.html' },
  { unit: 'review1', unitTitle: 'Review Test 1', skill: 'grammar-and-vocabulary', url: '/review-test-1/11031-review-test-1-grammar-and-vocabulary-kiem-tra-ngu-phap-va-tu-vung.html' },
  { unit: 'review1', unitTitle: 'Review Test 1', skill: 'listening', url: '/review-test-1/11032-review-test-1-listening-kiem-tra-ky-nang-nghe-hieu.html' },
  { unit: 'review1', unitTitle: 'Review Test 1', skill: 'reading', url: '/review-test-1/11033-review-test-1-listening-kiem-tra-ky-nang-doc-hieu.html' },

  // Review Test 2
  { unit: 'review2', unitTitle: 'Review Test 2', skill: 'instruction', url: '/review-test-2/11262-review-test-2-instruction-huong-dan-lam-bai-kiem-tra-on-tap-2.html' },
  { unit: 'review2', unitTitle: 'Review Test 2', skill: 'grammar-and-vocabulary', url: '/review-test-2/11263-review-test-2-grammar-and-vocabulary-kiem-tra-ngu-phap-va-tu-vung.html' },
  { unit: 'review2', unitTitle: 'Review Test 2', skill: 'listening', url: '/review-test-2/11264-review-test-2-listening-kiem-tra-ky-nang-nghe-hieu.html' },
  { unit: 'review2', unitTitle: 'Review Test 2', skill: 'reading', url: '/review-test-2/11265-review-test-2-reading-kiem-tra-ky-nang-doc-hieu.html' },

  // Review Test 3
  { unit: 'review3', unitTitle: 'Review Test 3', skill: 'instruction', url: '/review-test-3/11284-review-test-3-instruction-huong-dan-lam-bai-kiem-tra-on-tap-3.html' },
  { unit: 'review3', unitTitle: 'Review Test 3', skill: 'grammar-and-vocabulary', url: '/review-test-3/11285-review-test-3-grammar-and-vocabulary-kiem-tra-ngu-phap-tu-vung-3.html' },
  { unit: 'review3', unitTitle: 'Review Test 3', skill: 'listening', url: '/review-test-3/11286-review-test-3-listening.html' },
  { unit: 'review3', unitTitle: 'Review Test 3', skill: 'reading', url: '/review-test-3/11287-review-test-3-reading.html' },
];

async function main() {
  const results = [];
  let completed = 0;
  let failed = 0;

  for (const lesson of LESSONS) {
    const fullUrl = BASE + lesson.url;
    process.stdout.write(`[${completed + 1}/${LESSONS.length}] Fetching ${lesson.skill} of unit ${lesson.unit}... `);
    
    try {
      const res = await fetch(fullUrl);
      if (res.status === 200) {
        const title = extractTitle(res.body);
        const content = extractNoidung(res.body);
        results.push({
          unit: lesson.unit,
          unitTitle: lesson.unitTitle,
          skill: lesson.skill,
          title: title || '',
          url: fullUrl,
          content: content || '',
          status: 200
        });
        process.stdout.write(`OK (${content ? content.length : 0} chars)\n`);
      } else {
        results.push({
          unit: lesson.unit,
          unitTitle: lesson.unitTitle,
          skill: lesson.skill,
          url: fullUrl,
          content: '',
          status: res.status
        });
        process.stdout.write(`HTTP ${res.status}\n`);
        failed++;
      }
    } catch (e) {
      results.push({
        unit: lesson.unit,
        unitTitle: lesson.unitTitle,
        skill: lesson.skill,
        url: fullUrl,
        content: '',
        status: 0,
        error: e.message
      });
      process.stdout.write(`ERROR: ${e.message}\n`);
      failed++;
    }

    completed++;
    if (completed < LESSONS.length) {
      await sleep(DELAY_MS);
    }
  }

  // Build structured JSON
  const structured = {};
  for (const r of results) {
    const key = String(r.unit);
    if (!structured[key]) {
      structured[key] = {
        unit: r.unit,
        title: r.unitTitle,
        skills: {}
      };
    }
    structured[key].skills[r.skill] = {
      title: r.title,
      url: r.url,
      content: r.content,
      status: r.status
    };
  }

  const output = {
    course: 'Tiếng Anh Nâng Cao',
    source: BASE,
    fetchedAt: new Date().toISOString(),
    totalLessons: LESSONS.length,
    successCount: completed - failed,
    failedCount: failed,
    units: Object.values(structured)
  };

  fs.writeFileSync(OUTPUT, JSON.stringify(output, null, 2), 'utf-8');
  console.log(`\nDone! ${completed - failed}/${LESSONS.length} succeeded. Output saved to ${OUTPUT}`);
}

main().catch(console.error);
