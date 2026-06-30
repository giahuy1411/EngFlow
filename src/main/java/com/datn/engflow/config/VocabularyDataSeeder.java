package com.datn.engflow.config;

import com.datn.engflow.model.entity.Deck;
import com.datn.engflow.model.entity.DeckWord;
import com.datn.engflow.model.entity.Vocabulary;
import com.datn.engflow.model.enums.DeckSource;
import com.datn.engflow.repository.DeckRepository;
import com.datn.engflow.repository.DeckWordRepository;
import com.datn.engflow.repository.VocabularyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(2)
public class VocabularyDataSeeder implements CommandLineRunner {

    private final VocabularyRepository vocabularyRepository;
    private final DeckRepository deckRepository;
    private final DeckWordRepository deckWordRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (deckRepository.count() == 0) {
            log.info("Seeding static vocabulary decks...");
            seedOxford3000();
            seedAWL();
            seedTOEIC();
            seedIELTS();
            seedTHPT();
            seedEverydayEnglish();
            seedWorkplaceEnglish();
            seedOxford5000();
            seedPhrasalVerbs();
            seedIdioms();
            log.info("Vocabulary seeding completed.");
        }
    }

    private void seedOxford3000() {
        Deck deck = createDeck("Oxford 3000 (A1-B2)", "The 3000 most important words to learn in English.", DeckSource.OXFORD3000, "B2");
        List<Vocabulary> vocabs = new ArrayList<>();
        vocabs.add(createVocab("ambitious", "/æmˈbɪʃ.əs/", "có tham vọng", "Having a strong desire for success.", "She is ambitious and wants to become a doctor.", "adjective", "B2", DeckSource.OXFORD3000));
        vocabs.add(createVocab("benefit", "/ˈben.ɪ.fɪt/", "lợi ích", "A helpful or good effect.", "The discovery of oil brought many benefits to the town.", "noun", "A2", DeckSource.OXFORD3000));
        vocabs.add(createVocab("candidate", "/ˈkæn.dɪ.dət/", "ứng cử viên", "A person who is competing to get a job or elected position.", "There are three candidates standing in the election.", "noun", "B1", DeckSource.OXFORD3000));
        vocabs.add(createVocab("determine", "/dɪˈtɜː.mɪn/", "xác định", "To control or influence something directly.", "Your health is determined in part by what you eat.", "verb", "B1", DeckSource.OXFORD3000));
        vocabs.add(createVocab("essential", "/ɪˈsen.ʃəl/", "thiết yếu", "Necessary or needed.", "Water is essential for living things.", "adjective", "B1", DeckSource.OXFORD3000));
        vocabs.add(createVocab("explore", "/ɪkˈsplɔːr/", "khám phá", "To travel to or investigate an unfamiliar area.", "They decided to explore the old forest.", "verb", "B1", DeckSource.OXFORD3000));
        vocabs.add(createVocab("frequent", "/ˈfriː.kwənt/", "thường xuyên", "Happening or occurring often.", "She makes frequent trips to the library.", "adjective", "A2", DeckSource.OXFORD3000));
        vocabs.add(createVocab("generous", "/ˈdʒen.ər.əs/", "hào phóng", "Willing to give more than expected.", "He made a generous donation to the school.", "adjective", "B1", DeckSource.OXFORD3000));
        vocabs.add(createVocab("investigate", "/ɪnˈves.tɪ.ɡeɪt/", "điều tra", "To examine a situation systematically.", "The police will investigate the incident.", "verb", "B2", DeckSource.OXFORD3000));
        vocabs.add(createVocab("maintain", "/meɪnˈteɪn/", "duy trì", "To keep something in good condition.", "You should maintain your car regularly.", "verb", "B1", DeckSource.OXFORD3000));
        saveDeckWords(deck, vocabs);
    }

    private void seedAWL() {
        Deck deck = createDeck("Academic Word List", "The most frequent academic words.", DeckSource.AWL, "C1");
        List<Vocabulary> vocabs = new ArrayList<>();
        vocabs.add(createVocab("analyze", "/ˈæn.əl.aɪz/", "phân tích", "To study or examine something in detail.", "Researchers analyzed the purchases of 6,300 households.", "verb", "B2", DeckSource.AWL));
        vocabs.add(createVocab("concept", "/ˈkɒn.sept/", "khái niệm", "A principle or idea.", "The concept of free speech is unknown to them.", "noun", "B2", DeckSource.AWL));
        vocabs.add(createVocab("evident", "/ˈev.ɪ.dənt/", "hiển nhiên", "Easily seen or understood.", "The full extent of the damage only became evident the following morning.", "adjective", "B2", DeckSource.AWL));
        vocabs.add(createVocab("method", "/ˈmeθ.əd/", "phương pháp", "A particular way of doing something.", "Travelling by train is still one of the safest methods of transport.", "noun", "A2", DeckSource.AWL));
        vocabs.add(createVocab("category", "/ˈkæt.ə.ɡər.i/", "hạng mục", "A group of things sharing a common feature.", "This book falls into the fiction category.", "noun", "B1", DeckSource.AWL));
        vocabs.add(createVocab("economy", "/ɪˈkɒn.ə.mi/", "nền kinh tế", "The system of production and trade in a country.", "The economy is growing steadily this year.", "noun", "B1", DeckSource.AWL));
        vocabs.add(createVocab("identify", "/aɪˈden.tɪ.faɪ/", "nhận dạng", "To recognize or establish what something is.", "Can you identify the main problem?", "verb", "B1", DeckSource.AWL));
        vocabs.add(createVocab("occur", "/əˈkɜːr/", "xảy ra", "To happen or take place.", "When did the accident occur?", "verb", "B1", DeckSource.AWL));
        vocabs.add(createVocab("period", "/ˈpɪə.ri.əd/", "thời kỳ", "A length of time.", "The Renaissance was a period of great art.", "noun", "A2", DeckSource.AWL));
        vocabs.add(createVocab("structure", "/ˈstrʌk.tʃər/", "cấu trúc", "The arrangement of parts in something.", "The building has a modern steel structure.", "noun", "B1", DeckSource.AWL));
        saveDeckWords(deck, vocabs);
    }

    private void seedTOEIC() {
        Deck deck = createDeck("TOEIC 600 Essential Words", "Vocabulary for the TOEIC test, focusing on business contexts.", DeckSource.TOEIC, "B2");
        List<Vocabulary> vocabs = new ArrayList<>();
        vocabs.add(createVocab("contract", "/ˈkɒn.trækt/", "hợp đồng", "A legal document that states and explains a formal agreement.", "They could take legal action against you if you break the contract.", "noun", "B1", DeckSource.TOEIC));
        vocabs.add(createVocab("negotiate", "/nəˈɡəʊ.ʃi.eɪt/", "đàm phán", "To have formal discussions with someone in order to reach an agreement.", "The government has refused to negotiate with the strikers.", "verb", "C1", DeckSource.TOEIC));
        vocabs.add(createVocab("revenue", "/ˈrev.ən.juː/", "doanh thu", "The income that a government or company receives regularly.", "Taxes provide most of the government's revenue.", "noun", "C1", DeckSource.TOEIC));
        vocabs.add(createVocab("asset", "/ˈæs.et/", "tài sản", "An item of property owned by a person or company.", "The company's assets include buildings and equipment.", "noun", "B1", DeckSource.TOEIC));
        vocabs.add(createVocab("collateral", "/kəˈlæt.ər.əl/", "tài sản thế chấp", "Property pledged as security for a loan.", "The bank required collateral before approving the loan.", "noun", "C1", DeckSource.TOEIC));
        vocabs.add(createVocab("dividend", "/ˈdɪv.ɪ.dend/", "cổ tức", "A share of profits paid to shareholders.", "The company announced a dividend of $2 per share.", "noun", "C1", DeckSource.TOEIC));
        vocabs.add(createVocab("equity", "/ˈek.wɪ.ti/", "vốn chủ sở hữu", "The value of shares in a company.", "They have a 30% equity stake in the business.", "noun", "C1", DeckSource.TOEIC));
        vocabs.add(createVocab("franchise", "/ˈfræn.tʃaɪz/", "nhượng quyền", "A license to operate a branded business.", "She opened a fast-food franchise in the city.", "noun", "C1", DeckSource.TOEIC));
        vocabs.add(createVocab("goodwill", "/ɡʊdˈwɪl/", "lợi thế thương mại", "The reputation and customer relationships of a business.", "The purchase price included $5 million for goodwill.", "noun", "C1", DeckSource.TOEIC));
        vocabs.add(createVocab("hedge", "/hedʒ/", "phòng ngừa rủi ro", "An investment to reduce financial risk.", "The fund used futures contracts as a hedge.", "noun", "C1", DeckSource.TOEIC));
        saveDeckWords(deck, vocabs);
    }

    private void seedIELTS() {
        Deck deck = createDeck("IELTS Academic: Environment", "Vocabulary related to climate change, pollution, and the environment.", DeckSource.IELTS, "C1");
        List<Vocabulary> vocabs = new ArrayList<>();
        vocabs.add(createVocab("mitigate", "/ˈmɪt.ɪ.ɡeɪt/", "giảm nhẹ", "To make something less severe, harmful, or painful.", "It is unclear how to mitigate the effects of tourism on the island.", "verb", "C1", DeckSource.IELTS));
        vocabs.add(createVocab("sustainable", "/səˈsteɪ.nə.bəl/", "bền vững", "Causing little or no damage to the environment.", "A large international meeting was held with the aim of promoting sustainable development in all countries.", "adjective", "C1", DeckSource.IELTS));
        vocabs.add(createVocab("conservation", "/ˌkɒn.sərˈveɪ.ʃən/", "bảo tồn", "The protection of natural resources and wildlife.", "The conservation of rainforests is critical for biodiversity.", "noun", "B2", DeckSource.IELTS));
        vocabs.add(createVocab("emission", "/ɪˈmɪʃ.ən/", "khí thải", "The release of gases or pollutants into the air.", "The government plans to reduce carbon emissions by 50%.", "noun", "B2", DeckSource.IELTS));
        vocabs.add(createVocab("habitat", "/ˈhæb.ɪ.tæt/", "môi trường sống", "The natural home of an animal or plant.", "Deforestation destroys the habitat of many species.", "noun", "B2", DeckSource.IELTS));
        vocabs.add(createVocab("landfill", "/ˈlænd.fɪl/", "bãi rác", "A site where waste is buried in the ground.", "Most of our household waste ends up in a landfill.", "noun", "B2", DeckSource.IELTS));
        vocabs.add(createVocab("pollutant", "/pəˈluː.tənt/", "chất ô nhiễm", "A substance that contaminates the environment.", "Factories release pollutants into rivers and lakes.", "noun", "B2", DeckSource.IELTS));
        vocabs.add(createVocab("recycling", "/ˌriːˈsaɪ.klɪŋ/", "tái chế", "The process of converting waste into reusable material.", "Recycling helps to reduce the amount of waste in landfills.", "noun", "A2", DeckSource.IELTS));
        vocabs.add(createVocab("solar", "/ˈsəʊ.lər/", "thuộc mặt trời", "Relating to energy from the sun.", "Solar panels provide clean and renewable energy.", "adjective", "B1", DeckSource.IELTS));
        vocabs.add(createVocab("thermal", "/ˈθɜː.məl/", "nhiệt", "Relating to heat or temperature.", "Thermal power plants burn coal to generate electricity.", "adjective", "B2", DeckSource.IELTS));
        saveDeckWords(deck, vocabs);
    }

    private void seedTHPT() {
        Deck deck = createDeck("THPT Quốc Gia: Lớp 12", "Từ vựng sách giáo khoa lớp 12 chuẩn bị cho kỳ thi THPT QG.", DeckSource.THPT, "B1");
        List<Vocabulary> vocabs = new ArrayList<>();
        vocabs.add(createVocab("urbanization", "/ˌɜː.bən.aɪˈzeɪ.ʃən/", "đô thị hóa", "The process by which more and more people leave the countryside to live in cities.", "Fast urbanization can cause severe environmental problems.", "noun", "B2", DeckSource.THPT));
        vocabs.add(createVocab("biodiversity", "/ˌbaɪ.əʊ.daɪˈvɜː.sə.ti/", "đa dạng sinh học", "The number and types of plants and animals that exist in a particular area.", "A new National Biological Survey will protect species habitat and biodiversity.", "noun", "C1", DeckSource.THPT));
        vocabs.add(createVocab("compulsory", "/kəmˈpʌl.sər.i/", "bắt buộc", "Required by law or rule.", "English is a compulsory subject in high school.", "adjective", "B1", DeckSource.THPT));
        vocabs.add(createVocab("heritage", "/ˈher.ɪ.tɪdʒ/", "di sản", "Cultural traditions and historical sites passed down.", "Ha Long Bay is a UNESCO World Heritage site.", "noun", "B1", DeckSource.THPT));
        vocabs.add(createVocab("integrate", "/ˈɪn.tɪ.ɡreɪt/", "hội nhập", "To combine or become part of a larger group.", "It takes time to integrate into a new culture.", "verb", "B2", DeckSource.THPT));
        vocabs.add(createVocab("qualify", "/ˈkwɒl.ɪ.faɪ/", "đủ điều kiện", "To meet the requirements for something.", "You need to pass the exam to qualify for the course.", "verb", "B1", DeckSource.THPT));
        vocabs.add(createVocab("rural", "/ˈrʊə.rəl/", "nông thôn", "Relating to the countryside.", "Many young people move from rural areas to cities.", "adjective", "B1", DeckSource.THPT));
        vocabs.add(createVocab("volunteer", "/ˌvɒl.ənˈtɪər/", "tình nguyện viên", "A person who offers to do work without pay.", "She works as a volunteer at the local charity.", "noun", "B1", DeckSource.THPT));
        vocabs.add(createVocab("economic", "/ˌek.əˈnɒm.ɪk/", "thuộc kinh tế", "Relating to the economy or trade.", "The country is facing economic challenges.", "adjective", "B1", DeckSource.THPT));
        vocabs.add(createVocab("awareness", "/əˈweə.nəs/", "nhận thức", "Knowledge or understanding of something.", "The campaign raised awareness about climate change.", "noun", "B1", DeckSource.THPT));
        saveDeckWords(deck, vocabs);
    }

    private void seedEverydayEnglish() {
        Deck deck = createDeck("Everyday English", "Common vocabulary for daily life situations.", DeckSource.OXFORD3000, "A2");
        List<Vocabulary> vocabs = new ArrayList<>();
        vocabs.add(createVocab("apartment", "/əˈpɑːt.mənt/", "căn hộ", "A set of rooms for living in.", "They rent a small apartment in the city center.", "noun", "A2", DeckSource.OXFORD3000));
        vocabs.add(createVocab("bicycle", "/ˈbaɪ.sɪ.kəl/", "xe đạp", "A vehicle with two wheels that you ride by pedalling.", "She rides her bicycle to school every day.", "noun", "A1", DeckSource.OXFORD3000));
        vocabs.add(createVocab("calendar", "/ˈkæl.ən.dər/", "lịch", "A chart showing days, weeks, and months of the year.", "Mark the meeting date on your calendar.", "noun", "A2", DeckSource.OXFORD3000));
        vocabs.add(createVocab("dictionary", "/ˈdɪk.ʃən.ər.i/", "từ điển", "A book that lists words and their meanings.", "Look up the word in a dictionary.", "noun", "A2", DeckSource.OXFORD3000));
        vocabs.add(createVocab("envelope", "/ˈen.və.ləʊp/", "phong bì", "A paper cover for a letter.", "She sealed the letter in an envelope.", "noun", "A2", DeckSource.OXFORD3000));
        vocabs.add(createVocab("furniture", "/ˈfɜː.nɪ.tʃər/", "đồ nội thất", "Items like tables, chairs, and beds used in a home.", "They bought new furniture for the living room.", "noun", "A2", DeckSource.OXFORD3000));
        vocabs.add(createVocab("grocery", "/ˈɡrəʊ.sər.i/", "hàng tạp hóa", "Food and household supplies sold at a shop.", "I need to buy groceries for dinner tonight.", "noun", "A2", DeckSource.OXFORD3000));
        vocabs.add(createVocab("neighbor", "/ˈneɪ.bər/", "hàng xóm", "A person living near you.", "Our neighbor helped us carry the groceries.", "noun", "A2", DeckSource.OXFORD3000));
        vocabs.add(createVocab("restaurant", "/ˈres.tər.ɒnt/", "nhà hàng", "A place where meals are served to customers.", "We had dinner at a nice restaurant last night.", "noun", "A1", DeckSource.OXFORD3000));
        vocabs.add(createVocab("schedule", "/ˈʃed.juːl/", "lịch trình", "A plan of things to be done at certain times.", "What's your schedule for tomorrow?", "noun", "A2", DeckSource.OXFORD3000));
        saveDeckWords(deck, vocabs);
    }

    private void seedWorkplaceEnglish() {
        Deck deck = createDeck("Workplace English", "Essential vocabulary for professional communication.", DeckSource.TOEIC, "B1");
        List<Vocabulary> vocabs = new ArrayList<>();
        vocabs.add(createVocab("agenda", "/əˈdʒen.də/", "chương trình nghị sự", "A list of topics to discuss at a meeting.", "The first item on the agenda is the budget review.", "noun", "B1", DeckSource.TOEIC));
        vocabs.add(createVocab("collaborate", "/kəˈlæb.ə.reɪt/", "cộng tác", "To work jointly on an activity.", "The two teams will collaborate on the new project.", "verb", "B2", DeckSource.TOEIC));
        vocabs.add(createVocab("deadline", "/ˈded.laɪn/", "hạn chót", "The latest time by which something must be done.", "We need to meet the Friday deadline.", "noun", "B1", DeckSource.TOEIC));
        vocabs.add(createVocab("feedback", "/ˈfiːd.bæk/", "phản hồi", "Information about performance used for improvement.", "Please provide your feedback on the proposal.", "noun", "B1", DeckSource.TOEIC));
        vocabs.add(createVocab("implement", "/ˈɪm.plɪ.ment/", "thực hiện", "To put a plan or decision into effect.", "The company will implement the new policy next month.", "verb", "B2", DeckSource.TOEIC));
        vocabs.add(createVocab("milestone", "/ˈmaɪl.stəʊn/", "cột mốc", "An important stage in a project.", "Completing the prototype is a major milestone.", "noun", "B1", DeckSource.TOEIC));
        vocabs.add(createVocab("negotiate", "/nəˈɡəʊ.ʃi.eɪt/", "đàm phán", "To reach an agreement through discussion.", "The union will negotiate better working conditions.", "verb", "C1", DeckSource.TOEIC));
        vocabs.add(createVocab("prioritize", "/praɪˈɒr.ɪ.taɪz/", "ưu tiên", "To arrange items in order of importance.", "We need to prioritize our tasks for this week.", "verb", "B2", DeckSource.TOEIC));
        vocabs.add(createVocab("proposal", "/prəˈpəʊ.zəl/", "đề xuất", "A plan or suggestion for consideration.", "The committee will review the proposal tomorrow.", "noun", "B1", DeckSource.TOEIC));
        vocabs.add(createVocab("stakeholder", "/ˈsteɪk.həʊl.dər/", "bên liên quan", "A person with an interest in a project.", "All stakeholders were invited to the meeting.", "noun", "B2", DeckSource.TOEIC));
        saveDeckWords(deck, vocabs);
    }

    private void seedOxford5000() {
        Deck deck = createDeck("Oxford 5000 (C1-C2)", "Advanced vocabulary for proficient English learners.", DeckSource.OXFORD5000, "C2");
        List<Vocabulary> vocabs = new ArrayList<>();
        vocabs.add(createVocab("ambiguous", "/æmˈbɪɡ.ju.əs/", "mơ hồ", "Having more than one possible meaning.", "The contract language was deliberately ambiguous.", "adjective", "C1", DeckSource.OXFORD5000));
        vocabs.add(createVocab("articulate", "/ɑːˈtɪk.jə.lət/", "diễn đạt rõ ràng", "Able to express thoughts clearly.", "She gave an articulate presentation.", "adjective", "C1", DeckSource.OXFORD5000));
        vocabs.add(createVocab("collaborate", "/kəˈlæb.ə.reɪt/", "cộng tác", "To work jointly with others.", "The departments must collaborate on this initiative.", "verb", "B2", DeckSource.OXFORD5000));
        vocabs.add(createVocab("demonstrate", "/ˈdem.ən.streɪt/", "chứng minh", "To show clearly how something works.", "The experiment demonstrates the effect of gravity.", "verb", "B2", DeckSource.OXFORD5000));
        vocabs.add(createVocab("elaborate", "/ɪˈlæb.ər.eɪt/", "xây dựng chi tiết", "To add more detail to something.", "Could you elaborate on your proposal?", "verb", "C1", DeckSource.OXFORD5000));
        vocabs.add(createVocab("fluctuate", "/ˈflʌk.tʃu.eɪt/", "dao động", "To change frequently in level or amount.", "Stock prices fluctuate throughout the day.", "verb", "B2", DeckSource.OXFORD5000));
        vocabs.add(createVocab("hypothesize", "/haɪˈpɒθ.ə.saɪz/", "đặt giả thuyết", "To suggest an explanation for something.", "Scientists hypothesize that the planet once had water.", "verb", "C1", DeckSource.OXFORD5000));
        vocabs.add(createVocab("innovate", "/ˈɪn.ə.veɪt/", "đổi mới", "To introduce new ideas or methods.", "Companies must innovate to stay competitive.", "verb", "B2", DeckSource.OXFORD5000));
        vocabs.add(createVocab("legitimate", "/lɪˈdʒɪt.ɪ.mət/", "hợp pháp", "Conforming to the law or rules.", "They have a legitimate claim to the property.", "adjective", "C1", DeckSource.OXFORD5000));
        vocabs.add(createVocab("pragmatic", "/præɡˈmæt.ɪk/", "thực dụng", "Dealing with things in a practical way.", "We need a pragmatic approach to solve this issue.", "adjective", "C1", DeckSource.OXFORD5000));
        saveDeckWords(deck, vocabs);
    }

    private void seedPhrasalVerbs() {
        Deck deck = createDeck("Essential Phrasal Verbs", "Common phrasal verbs for everyday English.", DeckSource.IELTS, "B2");
        List<Vocabulary> vocabs = new ArrayList<>();
        vocabs.add(createVocab("bring up", "/brɪŋ ʌp/", "nuôi nấng / đề cập", "To raise a child or introduce a topic.", "She was brought up by her grandparents.", "phrasal verb", "B1", DeckSource.IELTS));
        vocabs.add(createVocab("carry out", "/ˈkær.i aʊt/", "tiến hành", "To perform or complete a task.", "The team will carry out the experiment tomorrow.", "phrasal verb", "B1", DeckSource.IELTS));
        vocabs.add(createVocab("come across", "/kʌm əˈkrɒs/", "tình cờ gặp", "To find something by chance.", "I came across an interesting article yesterday.", "phrasal verb", "B1", DeckSource.IELTS));
        vocabs.add(createVocab("figure out", "/ˈfɪɡ.ər aʊt/", "tìm ra", "To understand or solve something.", "I need to figure out how to fix this issue.", "phrasal verb", "B1", DeckSource.IELTS));
        vocabs.add(createVocab("give up", "/ɡɪv ʌp/", "từ bỏ", "To stop trying or doing something.", "Don't give up even if it gets difficult.", "phrasal verb", "A2", DeckSource.IELTS));
        vocabs.add(createVocab("look into", "/lʊk ˈɪn.tuː/", "xem xét", "To investigate or examine something.", "The manager will look into the complaint.", "phrasal verb", "B1", DeckSource.IELTS));
        vocabs.add(createVocab("point out", "/pɔɪnt aʊt/", "chỉ ra", "To draw attention to something.", "She pointed out several errors in the report.", "phrasal verb", "B1", DeckSource.IELTS));
        vocabs.add(createVocab("run into", "/rʌn ˈɪn.tuː/", "tình cờ gặp", "To meet someone unexpectedly.", "I ran into an old friend at the supermarket.", "phrasal verb", "B1", DeckSource.IELTS));
        vocabs.add(createVocab("take over", "/teɪk ˈəʊ.vər/", "tiếp quản", "To assume control of something.", "The new manager will take over next month.", "phrasal verb", "B1", DeckSource.IELTS));
        vocabs.add(createVocab("turn down", "/tɜːn daʊn/", "từ chối", "To refuse an offer or request.", "She turned down the job offer.", "phrasal verb", "B1", DeckSource.IELTS));
        saveDeckWords(deck, vocabs);
    }

    private void seedIdioms() {
        Deck deck = createDeck("Common English Idioms", "Popular idiomatic expressions for natural English.", DeckSource.AI_GENERATED, "B2");
        List<Vocabulary> vocabs = new ArrayList<>();
        vocabs.add(createVocab("break the ice", "/breɪk ði aɪs/", "phá vỡ không khí im lặng", "To start a conversation in a social setting.", "He told a joke to break the ice at the party.", "idiom", "B2", DeckSource.AI_GENERATED));
        vocabs.add(createVocab("hit the nail on the head", "/hɪt ðə neɪl ɒn ðə hed/", "nói trúng vấn đề", "To describe exactly what is causing a situation.", "You hit the nail on the head with your analysis.", "idiom", "C1", DeckSource.AI_GENERATED));
        vocabs.add(createVocab("under the weather", "/ˈʌn.dər ðə ˈweð.ər/", "cảm thấy mệt/ốm", "Feeling ill or unwell.", "I'm feeling a bit under the weather today.", "idiom", "B2", DeckSource.AI_GENERATED));
        vocabs.add(createVocab("piece of cake", "/piːs əv keɪk/", "dễ như ăn bánh", "Something very easy to do.", "The exam was a piece of cake.", "idiom", "B1", DeckSource.AI_GENERATED));
        vocabs.add(createVocab("once in a blue moon", "/wʌns ɪn ə bluː muːn/", "hiếm khi", "Very rarely.", "I only go to the cinema once in a blue moon.", "idiom", "B2", DeckSource.AI_GENERATED));
        vocabs.add(createVocab("let the cat out of the bag", "/let ðə kæt aʊt əv ðə bæɡ/", "lộ bí mật", "To reveal a secret accidentally.", "She let the cat out of the bag about the surprise party.", "idiom", "B2", DeckSource.AI_GENERATED));
        vocabs.add(createVocab("bite the bullet", "/baɪt ðə ˈbʊl.ɪt/", "chấp nhận điều khó khăn", "To face a difficult situation bravely.", "I decided to bite the bullet and tell him the truth.", "idiom", "B2", DeckSource.AI_GENERATED));
        vocabs.add(createVocab("cut corners", "/kʌt ˈkɔː.nəz/", "làm việc cẩu thả", "To do something cheaply or easily.", "Don't cut corners when it comes to safety.", "idiom", "B2", DeckSource.AI_GENERATED));
        vocabs.add(createVocab("face the music", "/feɪs ðə ˈmjuː.zɪk/", "đối mặt với hậu quả", "To accept punishment for something you did.", "He knew he'd have to face the music eventually.", "idiom", "B2", DeckSource.AI_GENERATED));
        vocabs.add(createVocab("spill the beans", "/spɪl ðə biːnz/", "tiết lộ bí mật", "To reveal secret information.", "Who spilled the beans about the layoffs?", "idiom", "B2", DeckSource.AI_GENERATED));
        saveDeckWords(deck, vocabs);
    }

    private Deck createDeck(String name, String description, DeckSource source, String cefrLevel) {
        Deck deck = Deck.builder()
                .name(name)
                .description(description)
                .source(source.name())
                .cefrLevel(cefrLevel)
                .isPublic(true)
                .build();
        return deckRepository.save(deck);
    }

    private Vocabulary createVocab(String word, String ipa, String vi, String en, String example, String pos, String level, DeckSource source) {
        return Vocabulary.builder()
                .word(word)
                .pronunciation(ipa)
                .meaning(vi)
                .definitionEn(en)
                .exampleSentence(example)
                .wordType(pos)
                .cefrLevel(level)
                .source(source.name())
                .build();
    }

    private void saveDeckWords(Deck deck, List<Vocabulary> vocabs) {
        vocabularyRepository.saveAll(vocabs);
        int index = 1;
        List<DeckWord> deckWords = new ArrayList<>();
        for (Vocabulary v : vocabs) {
            deckWords.add(DeckWord.builder()
                    .deck(deck)
                    .vocabulary(v)
                    .orderIndex(index++)
                    .build());
        }
        deckWordRepository.saveAll(deckWords);
    }
}
