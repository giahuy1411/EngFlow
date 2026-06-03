-- =============================================
-- V2__seed_data.sql - Dữ liệu mẫu cho EngFlow
-- =============================================

-- Seed Users (Password is BCrypt hash of '123456': $2a$10$8.tX9s86n.H5lE8pB8dJbeK6G2wT/P9nC/NpyBwRjUjJ2B53/1BvG)
INSERT INTO users (username, email, password_hash, full_name, avatar_url, role, current_level, total_points, is_active)
VALUES 
('student', 'user@gmail.com', '$2a$10$8.tX9s86n.H5lE8pB8dJbeK6G2wT/P9nC/NpyBwRjUjJ2B53/1BvG', N'Học Viên Mẫu', 'https://api.dicebear.com/7.x/adventurer/svg?seed=student', 'USER', 'BEGINNER', 0, 1),
('administrator', 'admin@gmail.com', '$2a$10$8.tX9s86n.H5lE8pB8dJbeK6G2wT/P9nC/NpyBwRjUjJ2B53/1BvG', N'Quản Trị Viên', 'https://api.dicebear.com/7.x/adventurer/svg?seed=admin', 'ADMIN', 'ADVANCED', 0, 1);

-- =============================================
-- Seed Lessons (6 bài học đầy đủ)
-- =============================================
SET IDENTITY_INSERT lessons ON;
INSERT INTO lessons (lesson_id, title, description, content, level, category, duration_minutes, thumbnail_url, audio_url, order_index, is_published)
VALUES 
(1, N'Chào hỏi và Giới thiệu bản thân', N'Học cách chào hỏi thông dụng và giới thiệu bản thân bằng tiếng Anh cơ bản.', N'Chào hỏi là bước đầu tiên và quan trọng nhất khi giao tiếp bằng tiếng Anh. Trong bài học này, chúng ta sẽ học các mẫu câu chào hỏi trang trọng và thân mật, cách tự giới thiệu tên, tuổi, quê quán của mình.', 'BEGINNER', 'Communication', 15, 'https://images.unsplash.com/photo-1517486808906-6ca8b3f04846?w=500', '', 1, 1),
(2, N'Thói quen Hằng ngày', N'Mở rộng vốn từ vựng về các hoạt động thường ngày và thì Hiện tại đơn.', N'Thói quen hằng ngày mô tả những việc chúng ta làm lặp đi lặp lại mỗi ngày. Chúng ta sẽ làm quen với các động từ chỉ hoạt động và cấu trúc câu mô tả thói quen.', 'BEGINNER', 'Vocabulary', 20, 'https://images.unsplash.com/photo-1506784983877-45594efa4cbe?w=500', '', 2, 1),
(3, N'Du lịch và Hỏi đường', N'Học từ vựng liên quan đến du lịch, sân bay và các mẫu câu hỏi đường thông dụng.', N'Khi đi du lịch nước ngoài, khả năng giao tiếp tại sân bay và hỏi đường là cực kỳ cần thiết. Bài học này sẽ giúp bạn tự tin di chuyển ở các địa điểm công cộng.', 'INTERMEDIATE', 'Travel', 25, 'https://images.unsplash.com/photo-1488646953014-85cb44e25828?w=500', '', 3, 1),
(4, N'Mua sắm và Trả giá', N'Từ vựng về quần áo, giá cả và các mẫu câu giao tiếp khi mua sắm.', N'Mua sắm là một hoạt động phổ biến khi du lịch hoặc sinh sống ở nước ngoài. Bài học này trang bị cho bạn các từ vựng, mẫu câu hỏi giá, trả giá, và thanh toán.', 'BEGINNER', 'Shopping', 20, 'https://images.unsplash.com/photo-1483985988355-763728e1935b?w=500', '', 4, 1),
(5, N'Đồ ăn và Nhà hàng', N'Học cách gọi món, đọc thực đơn và giao tiếp tại nhà hàng.', N'Thưởng thức ẩm thực là một phần quan trọng khi trải nghiệm văn hóa mới. Bài học này giúp bạn tự tin gọi món, hỏi thông tin về thành phần, và thanh toán hóa đơn tại nhà hàng.', 'INTERMEDIATE', 'Food', 25, 'https://images.unsplash.com/photo-1414235077428-338989a2e8c0?w=500', '', 5, 1),
(6, N'Phỏng vấn xin việc', N'Luyện kỹ năng trả lời phỏng vấn bằng tiếng Anh chuyên nghiệp.', N'Phỏng vấn xin việc bằng tiếng Anh đòi hỏi sự chuẩn bị kỹ lưỡng. Bài học này hướng dẫn cách trả lời các câu hỏi phổ biến, mô tả kinh nghiệm làm việc, và thể hiện bản thân một cách chuyên nghiệp.', 'ADVANCED', 'Business', 30, 'https://images.unsplash.com/photo-1573497019940-1c28c88b4f3e?w=500', '', 6, 1);
SET IDENTITY_INSERT lessons OFF;

-- =============================================
-- Seed Vocabulary
-- =============================================
INSERT INTO vocabulary (lesson_id, word, pronunciation, meaning, example_sentence, audio_url, image_url, word_type)
VALUES 
-- Lesson 1: Chào hỏi
(1, 'Introduce', '/ˌɪntrəˈdjuːs/', N'Giới thiệu', 'Let me introduce myself. I am Nam.', '', '', 'Verb'),
(1, 'Pleasure', '/ˈpleʒə(r)/', N'Niềm vinh hạnh, hân hạnh', 'It is a pleasure to meet you.', '', '', 'Noun'),
(1, 'Greeting', '/ˈɡriːtɪŋ/', N'Lời chào hỏi', 'In English, "Hello" is a common greeting.', '', '', 'Noun'),
(1, 'Acquaintance', '/əˈkweɪntəns/', N'Người quen', 'She is an acquaintance of mine from work.', '', '', 'Noun'),
(1, 'Farewell', '/ˌfeəˈwel/', N'Lời tạm biệt', 'We said our farewells at the airport.', '', '', 'Noun'),

-- Lesson 2: Thói quen
(2, 'Wake up', '/weɪk ʌp/', N'Thức giấc, tỉnh dậy', 'I usually wake up at 6 AM.', '', '', 'Verb'),
(2, 'Routine', '/ruːˈtiːn/', N'Thói quen hằng ngày, nề nếp', 'Getting exercise is part of my daily routine.', '', '', 'Noun'),
(2, 'Breakfast', '/ˈbrekfəst/', N'Bữa ăn sáng', 'I have bread and milk for breakfast.', '', '', 'Noun'),
(2, 'Commute', '/kəˈmjuːt/', N'Đi lại (giữa nhà và nơi làm việc)', 'My daily commute takes about 30 minutes.', '', '', 'Verb'),
(2, 'Schedule', '/ˈʃedʒuːl/', N'Lịch trình, thời gian biểu', 'I have a very tight schedule today.', '', '', 'Noun'),

-- Lesson 3: Du lịch
(3, 'Departure', '/dɪˈpɑːtʃə(r)/', N'Sự khởi hành, giờ đi', 'Please check the departure board for your flight.', '', '', 'Noun'),
(3, 'Destination', '/ˌdestɪˈneɪʃn/', N'Điểm đến, nơi đến', 'Paris is a popular tourist destination.', '', '', 'Noun'),
(3, 'Luggage', '/ˈlʌɡɪdʒ/', N'Hành lý', 'We need to check in our luggage.', '', '', 'Noun'),
(3, 'Boarding pass', '/ˈbɔːdɪŋ pɑːs/', N'Thẻ lên máy bay', 'Please show your boarding pass at the gate.', '', '', 'Noun'),
(3, 'Itinerary', '/aɪˈtɪnərəri/', N'Lịch trình chuyến đi', 'Our itinerary includes visits to three cities.', '', '', 'Noun'),

-- Lesson 4: Mua sắm
(4, 'Discount', '/ˈdɪskaʊnt/', N'Giảm giá, chiết khấu', 'There is a 20% discount on all items today.', '', '', 'Noun'),
(4, 'Receipt', '/rɪˈsiːt/', N'Hóa đơn, biên lai', 'Could I have the receipt, please?', '', '', 'Noun'),
(4, 'Fitting room', '/ˈfɪtɪŋ ruːm/', N'Phòng thử đồ', 'The fitting room is over there.', '', '', 'Noun'),
(4, 'Bargain', '/ˈbɑːɡɪn/', N'Mặc cả, trả giá', 'She likes to bargain at the market.', '', '', 'Verb'),
(4, 'Refund', '/ˈriːfʌnd/', N'Hoàn tiền', 'I would like a refund for this defective item.', '', '', 'Noun'),

-- Lesson 5: Đồ ăn
(5, 'Appetizer', '/ˈæpɪtaɪzə(r)/', N'Món khai vị', 'I will have a salad as an appetizer.', '', '', 'Noun'),
(5, 'Beverage', '/ˈbevərɪdʒ/', N'Đồ uống', 'What beverage would you like to order?', '', '', 'Noun'),
(5, 'Ingredient', '/ɪnˈɡriːdiənt/', N'Nguyên liệu, thành phần', 'This dish uses fresh local ingredients.', '', '', 'Noun'),
(5, 'Reservation', '/ˌrezəˈveɪʃn/', N'Đặt chỗ, đặt bàn', 'I would like to make a reservation for two.', '', '', 'Noun'),
(5, 'Tip', '/tɪp/', N'Tiền boa, tiền thưởng', 'It is customary to leave a 15% tip in America.', '', '', 'Noun'),

-- Lesson 6: Phỏng vấn
(6, 'Qualification', '/ˌkwɒlɪfɪˈkeɪʃn/', N'Bằng cấp, trình độ chuyên môn', 'What qualifications do you have for this role?', '', '', 'Noun'),
(6, 'Responsibility', '/rɪˌspɒnsəˈbɪləti/', N'Trách nhiệm', 'My main responsibility was managing the team.', '', '', 'Noun'),
(6, 'Achievement', '/əˈtʃiːvmənt/', N'Thành tựu, thành tích', 'My greatest achievement was launching the product.', '', '', 'Noun'),
(6, 'Salary', '/ˈsæləri/', N'Lương, tiền lương', 'The salary for this position is competitive.', '', '', 'Noun'),
(6, 'Deadline', '/ˈdedlaɪn/', N'Hạn chót, thời hạn', 'I always meet my deadlines on time.', '', '', 'Noun');

-- =============================================
-- Seed Grammar
-- =============================================
INSERT INTO grammar (lesson_id, title, explanation, formula, examples, level)
VALUES 
-- Lesson 1
(1, N'Động từ To Be (am, is, are)', N'Sử dụng để giới thiệu tên, tuổi, nghề nghiệp, quốc tịch hoặc mô tả đặc điểm, tính chất.', 'S + am/is/are + Noun/Adj\nS + am/is/are + not + Noun/Adj\nAm/Is/Are + S + Noun/Adj?', N'- I am a student.\n- He is friendly.\n- They are from Vietnam.', 'BEGINNER'),
(1, N'Đại từ nhân xưng (Personal Pronouns)', N'Các đại từ dùng để thay thế cho danh từ chỉ người, vật trong câu.', 'I / You / He / She / It / We / They', N'- I am a teacher.\n- He is my friend.\n- We are classmates.', 'BEGINNER'),

-- Lesson 2
(2, N'Thì Hiện tại đơn (Present Simple Tense)', N'Dùng để diễn tả một thói quen hằng ngày, hành động lặp đi lặp lại hoặc một sự thật hiển nhiên.', 'S + V(s/es)\nS + do/does + not + V_inf\nDo/Does + S + V_inf?', N'- She wakes up early every day.\n- They do not work on Sundays.\n- Does he drink coffee?', 'BEGINNER'),
(2, N'Trạng từ chỉ tần suất (Adverbs of Frequency)', N'Dùng để mô tả mức độ thường xuyên của một hành động: always, usually, often, sometimes, rarely, never.', 'S + adverb of frequency + V\nS + be + adverb of frequency + Adj', N'- I always brush my teeth before bed.\n- She is usually on time.\n- He rarely eats fast food.', 'BEGINNER'),

-- Lesson 3
(3, N'Động từ khuyết thiếu (Modal Verbs: Can, Should, Must)', N'Dùng để chỉ khả năng, lời khuyên hoặc sự bắt buộc trong ngữ cảnh hỏi đường và du lịch.', 'S + Modal Verb + V_inf', N'- You can take the bus to the city center.\n- You should ask for directions.\n- You must show your passport at the desk.', 'INTERMEDIATE'),
(3, N'Câu hỏi với Where/How (Questions with Where/How)', N'Dùng để hỏi về địa điểm hoặc cách thức di chuyển.', 'Where + be + S?\nHow + do/does + S + get to + place?', N'- Where is the nearest hospital?\n- How do I get to the train station?\n- Where can I find a taxi?', 'INTERMEDIATE'),

-- Lesson 4
(4, N'Câu so sánh hơn (Comparative)', N'Dùng để so sánh hai sự vật, sự việc. Thêm "-er" hoặc dùng "more" trước tính từ.', 'S1 + be + Adj-er + than + S2\nS1 + be + more + Adj + than + S2', N'- This shirt is cheaper than that one.\n- This brand is more expensive than others.\n- Which one is bigger?', 'BEGINNER'),

-- Lesson 5
(5, N'Câu hỏi lịch sự (Polite Requests)', N'Dùng cấu trúc lịch sự để yêu cầu hoặc hỏi thông tin trong nhà hàng.', 'Could I / Could you + V_inf?\nI would like + N / to V', N'- Could I have the menu, please?\n- I would like to order a steak.\n- Could you bring us some water?', 'INTERMEDIATE'),
(5, N'Danh từ đếm được và không đếm được', N'Phân biệt danh từ có thể đếm (a cake, two cakes) và không đếm (water, rice).', 'Countable: a/an/two/many + N_plural\nUncountable: some/much + N', N'- I would like two cups of coffee.\n- How much sugar do you want?\n- There are many restaurants here.', 'INTERMEDIATE'),

-- Lesson 6
(6, N'Thì Hiện tại hoàn thành (Present Perfect Tense)', N'Dùng để nói về kinh nghiệm, thành tựu hoặc hành động đã xảy ra nhưng chưa rõ thời gian cụ thể.', 'S + have/has + V_past_participle', N'- I have worked in IT for 5 years.\n- She has managed a team of 10 people.\n- We have completed the project successfully.', 'ADVANCED'),
(6, N'Câu điều kiện loại 2 (Second Conditional)', N'Dùng để nói về tình huống giả định không có thật ở hiện tại.', 'If + S + V_past, S + would + V_inf', N'- If I were the manager, I would improve the process.\n- If I had more experience, I would apply for the senior role.\n- What would you do if you got this job?', 'ADVANCED');

-- =============================================
-- Seed Exercises
-- =============================================
INSERT INTO exercises (lesson_id, title, question, exercise_type, options, correct_answer, explanation, points, difficulty, audio_url)
VALUES 
-- Lesson 1: Chào hỏi (3 câu)
(1, N'Chọn từ chào hỏi phù hợp', N'Khi gặp một người bạn mới vào buổi sáng, bạn sẽ chào thế nào?', 'MULTIPLE_CHOICE', '["Good night", "Good morning", "Goodbye", "See you later"]', '1', N'"Good morning" là lời chào buổi sáng, phù hợp khi gặp nhau.', 10, 'BEGINNER', ''),
(1, N'Điền động từ To Be thích hợp', N'I _____ from Vietnam. (am/is/are)', 'FILL_IN_BLANK', '["am"]', 'am', N'Chủ ngữ "I" đi kèm với động từ to be "am".', 10, 'BEGINNER', ''),
(1, N'Chọn cách giới thiệu đúng', N'Bạn muốn giới thiệu tên mình, câu nào đúng?', 'MULTIPLE_CHOICE', '["My name am John.", "I name is John.", "My name is John.", "Name my is John."]', '2', N'Cấu trúc đúng: My name is + tên.', 10, 'BEGINNER', ''),

-- Lesson 2: Thói quen (3 câu)
(2, N'Chia động từ thích hợp', N'My father always _____ (drink) tea in the morning.', 'FILL_IN_BLANK', '["drinks"]', 'drinks', N'Chủ ngữ "My father" (ngôi thứ 3 số ít) nên động từ "drink" thêm "s" thành "drinks" trong thì Hiện tại đơn.', 10, 'BEGINNER', ''),
(2, N'Chọn câu đúng thì Hiện tại đơn', N'Chọn câu phủ định đúng trong các câu sau đây:', 'MULTIPLE_CHOICE', '["He do not play football.", "He does not plays football.", "He does not play football.", "He is not play football."]', '2', N'Thể phủ định thì Hiện tại đơn ngôi thứ 3 số ít dùng trợ động từ "does not" + động từ nguyên thể "play".', 10, 'BEGINNER', ''),
(2, N'Điền trạng từ tần suất', N'She _____ goes to the gym. She goes five days a week.', 'MULTIPLE_CHOICE', '["never", "rarely", "usually", "sometimes"]', '2', N'"usually" (thường xuyên) phù hợp vì đi gym 5 ngày/tuần là rất thường xuyên.', 10, 'BEGINNER', ''),

-- Lesson 3: Du lịch (3 câu)
(3, N'Chọn câu hỏi đường phù hợp', N'Bạn muốn hỏi đường tới ga tàu gần nhất, câu nào sau đây là lịch sự và đúng?', 'MULTIPLE_CHOICE', '["Where station?", "How can I get to the nearest station?", "Station is where?", "Show me station."]', '1', N'"How can I get to the nearest station?" là cách hỏi đường lịch sự và chuẩn ngữ pháp.', 15, 'INTERMEDIATE', ''),
(3, N'Điền từ phù hợp', N'You _____ show your passport at the immigration desk.', 'FILL_IN_BLANK', '["must"]', 'must', N'"must" diễn tả sự bắt buộc - bạn bắt buộc phải xuất trình hộ chiếu tại quầy nhập cảnh.', 15, 'INTERMEDIATE', ''),
(3, N'Chọn từ đúng', N'All passengers must go through _____ before boarding the plane.', 'MULTIPLE_CHOICE', '["security check", "luggage room", "waiting hall", "parking lot"]', '0', N'"Security check" (kiểm tra an ninh) là bước bắt buộc trước khi lên máy bay.', 15, 'INTERMEDIATE', ''),

-- Lesson 4: Mua sắm (3 câu)
(4, N'Hỏi giá sản phẩm', N'Bạn muốn hỏi giá một chiếc áo, câu nào đúng nhất?', 'MULTIPLE_CHOICE', '["What money this?", "How much is this shirt?", "This shirt money?", "Price of shirt tell me."]', '1', N'"How much is this shirt?" là cách hỏi giá lịch sự và đúng ngữ pháp.', 10, 'BEGINNER', ''),
(4, N'Điền từ phù hợp', N'Could I try this dress on in the _____?', 'FILL_IN_BLANK', '["fitting room", "dressing room"]', 'fitting room', N'"fitting room" (phòng thử đồ) là nơi bạn thử quần áo trước khi mua.', 10, 'BEGINNER', ''),
(4, N'Chọn câu trả lời đúng', N'Người bán nói: "That will be $25." Bạn muốn trả bằng thẻ, bạn sẽ nói gì?', 'MULTIPLE_CHOICE', '["I pay card.", "Can I pay by card?", "Card money please.", "Give card you."]', '1', N'"Can I pay by card?" là cách hỏi lịch sự khi muốn thanh toán bằng thẻ.', 10, 'BEGINNER', ''),

-- Lesson 5: Đồ ăn (3 câu)
(5, N'Gọi món tại nhà hàng', N'Bạn muốn gọi một phần bít tết, cách nào lịch sự nhất?', 'MULTIPLE_CHOICE', '["Give me steak.", "I would like a steak, please.", "Steak! Now!", "I want eat steak."]', '1', N'"I would like a steak, please." là cách gọi món lịch sự nhất.', 15, 'INTERMEDIATE', ''),
(5, N'Điền từ phù hợp', N'Could I have the _____, please? I want to see what you serve.', 'FILL_IN_BLANK', '["menu"]', 'menu', N'"menu" (thực đơn) là thứ bạn cần xem để biết nhà hàng phục vụ những món gì.', 15, 'INTERMEDIATE', ''),
(5, N'Danh từ đếm được hay không?', N'Từ nào sau đây là danh từ KHÔNG đếm được?', 'MULTIPLE_CHOICE', '["apple", "water", "egg", "sandwich"]', '1', N'"water" (nước) là danh từ không đếm được. Ta nói "some water" chứ không nói "two waters".', 15, 'INTERMEDIATE', ''),

-- Lesson 6: Phỏng vấn (3 câu)
(6, N'Trả lời câu hỏi phỏng vấn', N'Nhà tuyển dụng hỏi: "Tell me about yourself." Cách trả lời nào chuyên nghiệp nhất?', 'MULTIPLE_CHOICE', '["I like playing games and sleeping.", "I have 3 years of experience in marketing and am passionate about digital strategy.", "I am fine, thank you.", "My name is Minh. I am 25 years old. I like cats."]', '1', N'Câu trả lời chuyên nghiệp nên tập trung vào kinh nghiệm và kỹ năng liên quan đến vị trí ứng tuyển.', 20, 'ADVANCED', ''),
(6, N'Điền thì đúng', N'I _____ (work) in the IT industry for 5 years.', 'FILL_IN_BLANK', '["have worked"]', 'have worked', N'Dùng thì Hiện tại hoàn thành "have worked" để nói về kinh nghiệm kéo dài đến hiện tại.', 20, 'ADVANCED', ''),
(6, N'Câu điều kiện loại 2', N'If I _____ the team leader, I would focus on improving communication.', 'FILL_IN_BLANK', '["were", "was"]', 'were', N'Trong câu điều kiện loại 2, dùng "were" cho tất cả các ngôi (kể cả "I"): "If I were..."', 20, 'ADVANCED', '');

-- =============================================
-- Seed Achievements
-- =============================================
SET IDENTITY_INSERT achievements ON;
INSERT INTO achievements (achievement_id, name, description, icon_url, points_required, badge_type)
VALUES 
(1, N'Khởi đầu Thuận lợi', N'Hoàn thành bài học đầu tiên trên hệ thống.', 'https://api.dicebear.com/7.x/identicon/svg?seed=first-step', 10, 'BRONZE'),
(2, N'Chăm Chỉ', N'Tích lũy tổng cộng 30 điểm học tập.', 'https://api.dicebear.com/7.x/identicon/svg?seed=diligence', 30, 'SILVER'),
(3, N'Chuyên Gia Ngôn Ngữ', N'Tích lũy tổng cộng 50 điểm học tập.', 'https://api.dicebear.com/7.x/identicon/svg?seed=expert', 50, 'GOLD'),
(4, N'Người Kiên Trì', N'Tích lũy tổng cộng 100 điểm học tập.', 'https://api.dicebear.com/7.x/identicon/svg?seed=persistence', 100, 'GOLD'),
(5, N'Bậc Thầy Tiếng Anh', N'Tích lũy tổng cộng 200 điểm học tập.', 'https://api.dicebear.com/7.x/identicon/svg?seed=master', 200, 'DIAMOND');
SET IDENTITY_INSERT achievements OFF;
